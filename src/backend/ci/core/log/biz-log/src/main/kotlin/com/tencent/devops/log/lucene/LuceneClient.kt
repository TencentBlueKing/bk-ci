/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.log.lucene

import com.tencent.devops.common.log.constant.Constants
import com.tencent.devops.common.log.pojo.LogLine
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.util.IndexNameUtils.LOG_INDEX_PREFIX
import com.tencent.devops.log.util.LuceneIndexUtils
import jakarta.ws.rs.core.StreamingOutput
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.codecs.Codec
import org.apache.lucene.document.Document
import org.apache.lucene.document.IntPoint
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.DocValuesType
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.LeafReaderContext
import org.apache.lucene.index.MultiReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreMode
import org.apache.lucene.search.SimpleCollector
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortedNumericSortField
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHits
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.store.LockObtainFailedException
import org.slf4j.LoggerFactory

data class LuceneLogQuery(
    val buildId: String,
    val debug: Boolean,
    val logType: LogType? = null,
    val tag: String? = null,
    val subTag: String? = null,
    val containerHashId: String? = null,
    val executeCount: Int? = null,
    val jobId: String? = null,
    val stepId: String? = null,
    val startLineNo: Long? = null,
    val endLineNo: Long? = null,
    val size: Int = Constants.NORMAL_MAX_LINES,
    val reverse: Boolean = false
)

data class LuceneLogPage(
    val logs: MutableList<LogLine> = mutableListOf(),
    val totalHits: Long = 0
)

/**
 * 社区版本地日志索引。仅在 `log.storage.type=lucene` 时创建。
 * 索引落在本进程可见的磁盘上，多副本必须挂载同一 dataDirectory，否则读写会落到不同磁盘。
 */
@Suppress("LongParameterList", "TooManyFunctions", "MagicNumber", "ComplexMethod")
class LuceneClient internal constructor(
    private val logRootDirectory: String,
    private val indexNameOf: (String) -> String,
    private val redisOperation: RedisOperation?
) {

    constructor(
        logRootDirectory: String,
        indexService: IndexService,
        redisOperation: RedisOperation
    ) : this(
        logRootDirectory = logRootDirectory,
        indexNameOf = { buildId -> indexService.getIndexName(buildId) },
        redisOperation = redisOperation
    )

    init {
        warmupLuceneSpi()
        File(logRootDirectory).mkdirs()
    }

    fun indexBatchLog(buildId: String, documents: List<Document>, deterministicId: Boolean): Int {
        if (documents.isEmpty()) {
            return 0
        }
        val index = indexNameOf(buildId)
        return withWriteLock(index, buildId) {
            writeWithRetry(buildId, index, documents, deterministicId)
        }
    }

    fun search(index: String, query: LuceneLogQuery): LuceneLogPage {
        if (index.isBlank() || query.size <= 0) {
            return LuceneLogPage()
        }
        if (query.startLineNo != null && query.endLineNo != null && query.startLineNo > query.endLineNo) {
            return LuceneLogPage()
        }
        return withSearcher(index, query.buildId) { searcher ->
            if (supportsIndexedSort(searcher.indexReader)) {
                searchIndexed(searcher, query)
            } else {
                searchLegacy(searcher, query)
            }
        } ?: LuceneLogPage()
    }

    fun count(index: String, query: LuceneLogQuery): Long {
        if (index.isBlank()) {
            return 0
        }
        if (query.startLineNo != null && query.endLineNo != null && query.startLineNo > query.endLineNo) {
            return 0
        }
        return withSearcher(index, query.buildId) { searcher ->
            if (supportsIndexedSort(searcher.indexReader) && hasLinePoint(searcher.indexReader)) {
                searcher.count(buildQuery(query)).toLong()
            } else {
                searchLegacy(searcher, query.copy(size = LEGACY_SCAN_LIMIT)).totalHits
            }
        } ?: 0L
    }

    fun fetchDocumentsStreaming(index: String, query: LuceneLogQuery): StreamingOutput {
        return StreamingOutput { output ->
            if (index.isBlank()) {
                return@StreamingOutput
            }
            withSearcher(index, query.buildId) { searcher ->
                if (!supportsIndexedSort(searcher.indexReader)) {
                    val legacy = searchLegacy(searcher, query.copy(size = LEGACY_SCAN_LIMIT, reverse = false))
                    writeLines(output, legacy.logs)
                    return@withSearcher
                }
                val luceneQuery = buildQuery(query)
                val sort = sort(reverse = false)
                var topDocs: org.apache.lucene.search.TopDocs =
                    searcher.search(luceneQuery, STREAM_BATCH, sort)
                var guard = 0
                while (topDocs.scoreDocs.isNotEmpty() && guard++ < STREAM_MAX_BATCHES) {
                    val lines = topDocs.scoreDocs.map { scoreDoc -> toLogLine(searcher.doc(scoreDoc.doc)) }
                    writeLines(output, lines)
                    val last = topDocs.scoreDocs.last()
                    topDocs = searcher.searchAfter(last, luceneQuery, STREAM_BATCH, sort)
                }
            }
        }
    }

    fun ensureIndex(index: String) {
        if (index.isBlank()) {
            return
        }
        File(logRootDirectory, index).mkdirs()
    }

    fun indexExists(index: String): Boolean {
        if (!index.startsWith(LOG_INDEX_PREFIX)) {
            return false
        }
        return File(logRootDirectory, index).isDirectory ||
            File(logRootDirectory, "$COLD_DIR${File.separator}$index").isDirectory
    }

    fun listIndices(cold: Boolean = false): List<String> {
        val root = if (cold) File(logRootDirectory, COLD_DIR) else File(logRootDirectory)
        return try {
            root.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith(LOG_INDEX_PREFIX) }
                ?.map { it.name }
                ?: emptyList()
        } catch (ignore: Exception) {
            logger.error("list lucene index files failed", ignore)
            emptyList()
        }
    }

    fun deleteIndex(index: String): Boolean {
        val hotDeleted = deleteRecursively(File(logRootDirectory, index))
        val coldDeleted = deleteRecursively(File(logRootDirectory, "$COLD_DIR${File.separator}$index"))
        return hotDeleted && coldDeleted
    }

    /**
     * 热数据迁到 dataDirectory/cold，查询时热目录和冷目录一起打开。
     * 对应 ES 的冷节点迁移：数据仍可查，只是离开热目录。
     */
    fun archiveIndex(index: String): Boolean {
        val source = File(logRootDirectory, index)
        if (!source.exists()) {
            return false
        }
        val coldRoot = File(logRootDirectory, COLD_DIR)
        if (!coldRoot.exists() && !coldRoot.mkdirs()) {
            logger.warn("[$index] fail to create lucene cold directory: ${coldRoot.absolutePath}")
            return false
        }
        val target = File(coldRoot, index)
        if (target.exists()) {
            logger.warn("[$index] lucene cold index already exists, skip archive")
            return false
        }
        return try {
            Files.move(source.toPath(), target.toPath())
            true
        } catch (ignore: Exception) {
            logger.error("[$index] archive lucene index failed", ignore)
            false
        }
    }

    /**
     * 锁文件放在数据目录里。每个进程清理自己看到的磁盘；
     * 共享盘时文件锁避免两个进程同时搬迁同一目录。不用 Redis 锁，避免其他节点的本地目录永远清不到。
     */
    fun <T> withCleanLock(action: () -> T): T? {
        val root = File(logRootDirectory)
        if (!root.exists() && !root.mkdirs()) {
            logger.warn("lucene data directory is not available: $logRootDirectory")
            return null
        }
        val channel = FileChannel.open(
            File(root, CLEAN_LOCK_FILE).toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )
        channel.use {
            val lock = try {
                it.tryLock()
            } catch (ignore: OverlappingFileLockException) {
                null
            } ?: return null
            try {
                return action()
            } finally {
                runCatching { lock.release() }
            }
        }
    }

    private fun searchIndexed(searcher: IndexSearcher, query: LuceneLogQuery): LuceneLogPage {
        val luceneQuery = buildQuery(query)
        val topDocs = searcher.search(luceneQuery, query.size, sort(query.reverse))
        val logs = distinctLines(topDocs.scoreDocs.map { scoreDoc -> toLogLine(searcher.doc(scoreDoc.doc)) })
        return LuceneLogPage(
            logs = logs,
            totalHits = resolveTotalHits(searcher, luceneQuery, topDocs.totalHits, logs.size, query.size)
        )
    }

    /**
     * 旧索引把 timestamp 存成字符串，没有数值 DocValues，按数值排序会抛类型错误。
     * 这类索引改为读出后在内存排序，并在内存里按行号过滤。
     */
    private fun searchLegacy(searcher: IndexSearcher, query: LuceneLogQuery): LuceneLogPage {
        val lines = mutableListOf<LogLine>()
        searcher.search(buildQuery(query, linePoint = false), object : SimpleCollector() {
            private var leafReader: org.apache.lucene.index.LeafReader? = null

            override fun scoreMode(): ScoreMode = ScoreMode.COMPLETE_NO_SCORES

            override fun doSetNextReader(context: LeafReaderContext) {
                leafReader = context.reader()
            }

            override fun collect(doc: Int) {
                if (lines.size >= LEGACY_SCAN_LIMIT) {
                    return
                }
                val reader = leafReader ?: return
                lines.add(toLogLine(reader.document(doc)))
            }
        })
        if (lines.size >= LEGACY_SCAN_LIMIT) {
            logger.warn("[${query.buildId}] lucene legacy scan reached $LEGACY_SCAN_LIMIT")
        }
        val filtered = lines.filter { line ->
            (query.startLineNo == null || line.lineNo >= query.startLineNo) &&
                (query.endLineNo == null || line.lineNo <= query.endLineNo)
        }
        val distinct = distinctLines(filtered)
        val sorted = if (query.reverse) {
            distinct.sortedWith(compareByDescending<LogLine> { it.timestamp }.thenByDescending { it.lineNo })
        } else {
            distinct.sortedWith(compareBy<LogLine> { it.timestamp }.thenBy { it.lineNo })
        }
        return LuceneLogPage(
            logs = sorted.take(query.size).toMutableList(),
            totalHits = distinct.size.toLong()
        )
    }

    private fun distinctLines(lines: List<LogLine>): MutableList<LogLine> {
        val seen = LinkedHashSet<Long>()
        val result = mutableListOf<LogLine>()
        lines.forEach { line ->
            if (seen.add(line.lineNo)) {
                result.add(line)
            }
        }
        return result
    }

    private fun supportsIndexedSort(reader: IndexReader): Boolean {
        if (reader.leaves().isEmpty()) {
            return true
        }
        return reader.leaves().all { leaf ->
            val infos = leaf.reader().fieldInfos
            infos.fieldInfo(LuceneIndexUtils.FIELD_TIMESTAMP)?.docValuesType == DocValuesType.SORTED_NUMERIC &&
                infos.fieldInfo(LuceneIndexUtils.FIELD_LINE_NO)?.docValuesType == DocValuesType.SORTED_NUMERIC
        }
    }

    private fun hasLinePoint(reader: IndexReader): Boolean {
        if (reader.leaves().isEmpty()) {
            return true
        }
        return reader.leaves().all { leaf ->
            val lineNo = leaf.reader().fieldInfos.fieldInfo(LuceneIndexUtils.FIELD_LINE_NO)
            lineNo != null && lineNo.pointDimensionCount > 0
        }
    }

    private fun writeLines(output: java.io.OutputStream, lines: List<LogLine>) {
        if (lines.isEmpty()) {
            return
        }
        val sb = StringBuilder()
        lines.forEach { line ->
            val dateTime = LOG_TIME_FORMATTER.format(Instant.ofEpochMilli(line.timestamp))
            sb.append(dateTime)
                .append(" : ")
                .append(line.message.stripAnsi())
                .append(System.lineSeparator())
        }
        output.write(sb.toString().toByteArray())
        output.flush()
    }

    private fun writeWithRetry(
        buildId: String,
        index: String,
        documents: List<Document>,
        deterministicId: Boolean
    ): Int {
        var attempt = 0
        var lastError: Exception? = null
        while (attempt < WRITE_LOCK_RETRY) {
            try {
                return writeOnce(buildId, index, documents, deterministicId)
            } catch (lockFail: LockObtainFailedException) {
                lastError = lockFail
                attempt++
                logger.warn("[$buildId] lucene index writer is busy, retry $attempt/$WRITE_LOCK_RETRY", lockFail)
                Thread.sleep(WRITE_LOCK_RETRY_SLEEP_MS * attempt)
            }
        }
        throw lastError ?: LockObtainFailedException("lucene index writer lock failed: $buildId")
    }

    private fun writeOnce(
        buildId: String,
        index: String,
        documents: List<Document>,
        deterministicId: Boolean
    ): Int {
        val directory = openWritableDirectory(index, buildId)
        var writer: IndexWriter? = null
        return try {
            val opened = IndexWriter(directory, newWriterConfig())
            writer = opened
            writeDocuments(opened, documents, deterministicId)
            opened.commit()
            documents.size
        } catch (fail: Exception) {
            rollbackQuietly(writer)
            writer = null
            logger.error("[$buildId] batch index log ${documents.size} failed", fail)
            throw fail
        } finally {
            runCatching { writer?.close() }
            runCatching { directory.close() }
        }
    }

    private fun writeDocuments(writer: IndexWriter, documents: List<Document>, deterministicId: Boolean) {
        if (!deterministicId) {
            writer.addDocuments(documents)
            return
        }
        documents.forEach { document -> upsertDocument(writer, document) }
    }

    private fun upsertDocument(writer: IndexWriter, document: Document) {
        val docId = document.get(LuceneIndexUtils.FIELD_DOC_ID)
        if (docId.isNullOrBlank()) {
            writer.addDocument(document)
            return
        }
        writer.updateDocument(Term(LuceneIndexUtils.FIELD_DOC_ID, docId), document)
    }

    private fun rollbackQuietly(writer: IndexWriter?) {
        if (writer == null) {
            return
        }
        runCatching { writer.rollback() }
    }

    private fun <T> withWriteLock(index: String, buildId: String, action: () -> T): T {
        val local = stripeLock(buildId)
        local.lock()
        try {
            val redis = redisOperation ?: return action()
            return LuceneIndexLock(redis, index, buildId).lockAround(action)
        } finally {
            local.unlock()
        }
    }

    private fun <T> withSearcher(index: String, buildId: String, block: (IndexSearcher) -> T): T? {
        val directories = openReadableDirectories(index, buildId)
        if (directories.isEmpty()) {
            return null
        }
        val readers = mutableListOf<DirectoryReader>()
        var composite: IndexReader? = null
        return try {
            directories.forEach { directory ->
                if (DirectoryReader.indexExists(directory)) {
                    readers.add(DirectoryReader.open(directory))
                }
            }
            if (readers.isEmpty()) {
                return null
            }
            composite = if (readers.size == 1) {
                readers.first()
            } else {
                MultiReader(readers.toTypedArray(), true)
            }
            block(IndexSearcher(composite))
        } catch (ignore: Exception) {
            logger.error("[$buildId] fetch lucene logs failed", ignore)
            throw ignore
        } finally {
            runCatching { composite?.close() }
            if (composite == null) {
                readers.forEach { reader -> runCatching { reader.close() } }
            }
            directories.forEach { directory -> runCatching { directory.close() } }
        }
    }

    private fun openWritableDirectory(index: String, buildId: String): Directory {
        val dir = buildDir(index, buildId, cold = false)
        dir.mkdirs()
        return FSDirectory.open(dir.toPath())
    }

    private fun openReadableDirectories(index: String, buildId: String): List<Directory> {
        return listOf(false, true).mapNotNull { cold ->
            val dir = buildDir(index, buildId, cold)
            if (!dir.exists()) {
                null
            } else {
                FSDirectory.open(dir.toPath())
            }
        }
    }

    private fun buildDir(index: String, buildId: String, cold: Boolean): File {
        val root = if (cold) File(logRootDirectory, COLD_DIR) else File(logRootDirectory)
        val prefix = buildId.take(BUILD_PREFIX_LENGTH).ifBlank { "0000" }
        return File(root, index + File.separator + prefix + File.separator + buildId)
    }

    private fun buildQuery(query: LuceneLogQuery, linePoint: Boolean = true): BooleanQuery {
        val builder = BooleanQuery.Builder()
        addTerm(builder, LuceneIndexUtils.FIELD_TAG, query.tag)
        addTerm(builder, LuceneIndexUtils.FIELD_SUB_TAG, query.subTag)
        addTerm(builder, LuceneIndexUtils.FIELD_CONTAINER_HASH_ID, query.containerHashId)
        addTerm(builder, LuceneIndexUtils.FIELD_JOB_ID, query.jobId)
        addTerm(builder, LuceneIndexUtils.FIELD_STEP_ID, query.stepId)
        if (query.logType != null) {
            builder.add(
                TermQuery(Term(LuceneIndexUtils.FIELD_LOG_TYPE, query.logType.name)),
                BooleanClause.Occur.MUST
            )
        }
        if (!query.debug) {
            builder.add(
                TermQuery(Term(LuceneIndexUtils.FIELD_LOG_TYPE, LogType.DEBUG.name)),
                BooleanClause.Occur.MUST_NOT
            )
        }
        builder.add(
            IntPoint.newExactQuery(LuceneIndexUtils.FIELD_EXECUTE_COUNT, query.executeCount ?: 1),
            BooleanClause.Occur.MUST
        )
        builder.add(
            TermQuery(Term(LuceneIndexUtils.FIELD_BUILD_ID, query.buildId)),
            BooleanClause.Occur.MUST
        )
        if (linePoint && (query.startLineNo != null || query.endLineNo != null)) {
            val lower = query.startLineNo ?: Long.MIN_VALUE
            val upper = query.endLineNo ?: Long.MAX_VALUE
            builder.add(LongPoint.newRangeQuery(LuceneIndexUtils.FIELD_LINE_NO, lower, upper), BooleanClause.Occur.MUST)
        }
        return builder.build()
    }

    private fun addTerm(builder: BooleanQuery.Builder, field: String, value: String?) {
        if (!value.isNullOrBlank()) {
            builder.add(TermQuery(Term(field, value)), BooleanClause.Occur.MUST)
        }
    }

    private fun sort(reverse: Boolean): Sort {
        return Sort(
            SortedNumericSortField(LuceneIndexUtils.FIELD_TIMESTAMP, SortField.Type.LONG, reverse),
            SortedNumericSortField(LuceneIndexUtils.FIELD_LINE_NO, SortField.Type.LONG, reverse)
        )
    }

    private fun resolveTotalHits(
        searcher: IndexSearcher,
        query: Query,
        totalHits: TotalHits,
        returned: Int,
        size: Int
    ): Long {
        if (totalHits.relation == TotalHits.Relation.EQUAL_TO) {
            return totalHits.value
        }
        if (returned < size) {
            return returned.toLong()
        }
        return searcher.count(query).toLong()
    }

    private fun toLogLine(document: Document): LogLine {
        return LogLine(
            lineNo = storedLong(document, LuceneIndexUtils.FIELD_LINE_NO),
            timestamp = storedLong(document, LuceneIndexUtils.FIELD_TIMESTAMP),
            message = storedString(document, LuceneIndexUtils.FIELD_MESSAGE),
            priority = Constants.DEFAULT_PRIORITY_NOT_DELETED,
            tag = storedString(document, LuceneIndexUtils.FIELD_TAG),
            subTag = storedString(document, LuceneIndexUtils.FIELD_SUB_TAG),
            jobId = storedString(document, LuceneIndexUtils.FIELD_JOB_ID),
            executeCount = storedInt(document, LuceneIndexUtils.FIELD_EXECUTE_COUNT),
            containerHashId = storedString(document, LuceneIndexUtils.FIELD_CONTAINER_HASH_ID),
            stepId = storedString(document, LuceneIndexUtils.FIELD_STEP_ID)
        )
    }

    private fun storedString(document: Document, name: String): String {
        return document.get(name) ?: ""
    }

    private fun storedLong(document: Document, name: String): Long {
        document.getFields(name).forEach { field ->
            field.numericValue()?.let { return it.toLong() }
            field.stringValue()?.toLongOrNull()?.let { return it }
        }
        return 0L
    }

    private fun storedInt(document: Document, name: String): Int {
        document.getFields(name).forEach { field ->
            field.numericValue()?.let { return it.toInt() }
            field.stringValue()?.toIntOrNull()?.let { return it }
        }
        return 1
    }

    private fun deleteRecursively(file: File): Boolean {
        if (!file.exists()) {
            return true
        }
        return try {
            Files.walk(file.toPath())
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
            true
        } catch (ignore: Exception) {
            logger.error("delete lucene index files failed: ${file.absolutePath}", ignore)
            false
        }
    }

    private fun newWriterConfig(): IndexWriterConfig {
        // 字段都是 StringField / Point / DocValues，不走分词。
        // IndexWriterConfig() 默认要 StandardAnalyzer，该类在 lucene-analyzers-common 里，
        // assembly 只带 lucene-core 时写日志会 NoClassDefFoundError。
        return IndexWriterConfig(keywordAnalyzer)
    }

    private fun stripeLock(buildId: String): ReentrantLock {
        val slot = (buildId.hashCode().toUInt() % STRIPE_LOCKS.size.toUInt()).toInt()
        return STRIPE_LOCKS[slot]
    }

    private fun warmupLuceneSpi() {
        val loader = LuceneClient::class.java.classLoader
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        try {
            thread.contextClassLoader = loader
            val codec = Codec.getDefault()
            logger.info("Lucene codec ready: ${codec.name}, dataDirectory=$logRootDirectory")
        } catch (fail: Throwable) {
            logger.error(
                "Lucene codec SPI failed. log.storage.type=lucene requires lucene-core on the log process classpath",
                fail
            )
            throw IllegalStateException("Lucene codec is not available", fail)
        } finally {
            thread.contextClassLoader = previous
        }
    }

    private fun String.stripAnsi(): String {
        return removePrefix("\u001b[31m")
            .removePrefix("\u001b[1m")
            .replace("\u001B[m", "")
            .removeSuffix("\u001b[m")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LuceneClient::class.java)
        private const val COLD_DIR = "cold"
        private const val CLEAN_LOCK_FILE = ".lucene-clean.lock"
        private const val BUILD_PREFIX_LENGTH = 4
        private const val STREAM_BATCH = 1000
        private const val STREAM_MAX_BATCHES = 100000
        private const val LEGACY_SCAN_LIMIT = 1_000_000
        private const val WRITE_LOCK_RETRY = 5
        private const val WRITE_LOCK_RETRY_SLEEP_MS = 200L
        private val LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
            .withZone(ZoneId.systemDefault())
        private val STRIPE_LOCKS = Array(64) { ReentrantLock() }

        private val keywordAnalyzer = object : Analyzer() {
            override fun createComponents(fieldName: String): TokenStreamComponents {
                val tokenizer = object : Tokenizer() {
                    private val term = addAttribute(CharTermAttribute::class.java)
                    private var consumed = false

                    override fun incrementToken(): Boolean {
                        if (consumed) {
                            return false
                        }
                        consumed = true
                        clearAttributes()
                        term.setEmpty()
                        return false
                    }

                    override fun reset() {
                        super.reset()
                        consumed = false
                    }
                }
                return TokenStreamComponents(tokenizer)
            }
        }
    }
}
