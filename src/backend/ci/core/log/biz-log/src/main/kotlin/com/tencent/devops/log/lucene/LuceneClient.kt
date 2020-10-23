package com.tencent.devops.log.lucene

import com.tencent.devops.common.log.pojo.LogLine
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.util.Constants
import org.apache.lucene.document.Document
import org.apache.lucene.document.IntPoint
import org.apache.lucene.document.NumericDocValuesField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortedNumericSortField
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import javax.ws.rs.core.StreamingOutput

class LuceneClient constructor(
    private val logRootDirectory: String,
    private val indexService: IndexService,
    private val redisOperation: RedisOperation
) {

    fun indexBatchLog(buildId: String, documents: List<Document>): Int {
        val writer = prepareWriter(buildId)
        return try {
            writer.addDocuments(documents)
            writer.numRamDocs()
        } catch (e: Exception) {
            logger.error("[$buildId] batch index log ${documents.size} failed:", e)
            writer.rollback()
            0
        } finally {
            writer.maybeMerge()
            writer.close()
            writer.directory.close()
        }
    }

    fun fetchInitLogs(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        size: Int? = null
    ): MutableList<LogLine> {
        val lineNum = size ?: Constants.MAX_LINES
        val query = prepareQueryBuilder(buildId, tag, subTag, jobId, executeCount).build()
        logger.info("[$buildId] fetchInitLogs with query: $query")
        return doQueryLogsInSize(buildId, query, lineNum)
    }

    fun fetchLogs(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        start: Long? = null,
        end: Long? = null,
        size: Int? = null
    ): MutableList<LogLine> {
        val lower = start ?: 0
        val upper = end ?: Long.MAX_VALUE
        val lineNum = size ?: Constants.MAX_LINES
        val query = prepareQueryBuilder(buildId, tag, subTag, jobId, executeCount)
            .add(NumericDocValuesField.newSlowRangeQuery("lineNo", lower, upper), BooleanClause.Occur.MUST)
            .build()
        logger.info("[$buildId] fetchLogsInRange with query: $query")
        return doQueryLogsInSize(buildId, query, lineNum)
    }

    fun fetchLogsCount(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Int {
        val query = prepareQueryBuilder(buildId, tag, subTag, jobId, executeCount).build()
        logger.info("[$buildId] fetchLogsCount with query: $query")
        return doQueryLogsCount(buildId, query)
    }

    fun fetchDocumentsStreaming(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): StreamingOutput {
        val searcher = prepareSearcher(buildId)
        val query = prepareQueryBuilder(buildId, tag, subTag, jobId, executeCount).build()
        val sort = getLineNoSort()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
        try {
            var docs = searcher.search(query, 4000, sort)
            return StreamingOutput { output ->
                do {
                    val sb = StringBuilder()
                    docs?.scoreDocs?.forEach {
                        val hit = searcher.doc(it.doc)
                        val timestamp = hit.getField("timestamp").stringValue().toLong()
                        val message = hit.getField("message").stringValue().removePrefix("\u001b[31m").removePrefix("\u001b[1m").replace(
                            "\u001B[m",
                            ""
                        ).removeSuffix("\u001b[m")
                        val dateTime = sdf.format(Date(timestamp))
                        sb.append("""$dateTime : ${message}${System.lineSeparator()}""")
                    }
                    output.write(sb.toString().toByteArray())
                    output.flush()
                    docs = searcher.searchAfter(docs.scoreDocs.last(), query, 4000, sort, false)
                } while (docs.scoreDocs.isEmpty())
            }
        } catch (e: Exception) {
            logger.error("[$buildId] fetch logs in streaming failed:", e)
            throw e
        }
    }

    fun fetchAllLogsInPage(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): List<LogLine> {

        val builder = prepareQueryBuilder(buildId, tag, subTag, jobId, executeCount)
        if (page != -1 && pageSize != -1) {
            val endLineNo = pageSize * page
            val beginLineNo = endLineNo - pageSize + 1
            builder.add(NumericDocValuesField.newSlowRangeQuery("lineNo", endLineNo.toLong(), beginLineNo.toLong()), BooleanClause.Occur.MUST)
        }
        val query = builder.build()
        val searcher = prepareSearcher(buildId)
        val sort = getLineNoSort()
        val logs = mutableListOf<LogLine>()
        try {
            var docs = searcher.search(query, 4000, sort)
                do {
                    docs?.scoreDocs?.forEach {
                        val hit = searcher.doc(it.doc)
                        logs.add(genLogLine(hit))
                    }
                    docs = searcher.searchAfter(docs.scoreDocs.last(), query, 4000, sort, false)
                } while (docs.scoreDocs.isEmpty())
            return logs
        } catch (e: Exception) {
            logger.error("[$buildId] fetch logs in streaming failed:", e)
            throw e
        } finally {
            searcher.indexReader.close()
        }
    }

    fun listIndices(): List<String> {
        val rootDirectory = File(logRootDirectory ?: return emptyList())
        return try {
            rootDirectory.list().toList()
        } catch (e: Exception) {
            logger.error("list index files failed: ", e)
            emptyList()
        }
    }

    fun deleteIndex(index: String): Boolean {
        val indexDirectory = File(logRootDirectory + File.separator + index)
        return try {
            if (indexDirectory.exists()) indexDirectory.delete()
            true
        } catch (e: Exception) {
            logger.error("delete index files failed: ", e)
            false
        }
    }

    private fun doQueryLogsInSize(buildId: String, query: BooleanQuery, size: Int): MutableList<LogLine> {
        val searcher = prepareSearcher(buildId)
        try {
            val topDocs = searcher.search(query, size, getLineNoSort())
            return topDocs.scoreDocs.map {
                val hit = searcher.doc(it.doc)
                genLogLine(hit)
            }.toMutableList()
        } catch (e: Exception) {
            logger.error("[$buildId] fetch logs failed:", e)
            throw e
        } finally {
            searcher.indexReader.close()
        }
    }

    private fun doQueryLogsCount(buildId: String, query: BooleanQuery): Int {
        val searcher = prepareSearcher(buildId)
        try {
            return searcher.count(query)
        } catch (e: Exception) {
            logger.error("[$buildId] fetch logs failed:", e)
            throw e
        } finally {
            searcher.indexReader.close()
        }
    }

    private fun prepareSearcher(buildId: String): IndexSearcher {
        val index = indexService.getIndexName(buildId)
        val directory = prepareDirectory(buildId, index)
        val reader: IndexReader = DirectoryReader.open(directory)
        return IndexSearcher(reader)
    }

    private fun prepareWriter(buildId: String): IndexWriter {
        val index = indexService.getIndexName(buildId)
        // 同一索引不能同时存在两个IndexWriter，增加索引的互斥锁
        val indexLock = LuceneIndexLock(redisOperation, index, buildId)
        val directory = prepareDirectory(buildId, index)
        try {
            indexLock.lock()
            return IndexWriter(directory, IndexWriterConfig())
        } finally {
            indexLock.unlock()
        }
    }

    private fun prepareDirectory(buildId: String, index: String): Directory {
        val dirFile = File(logRootDirectory + File.separator + index + File.separator + buildId)
        return FSDirectory.open(dirFile.toPath())
    }

    private fun prepareQueryBuilder(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): BooleanQuery.Builder {
        val query = BooleanQuery.Builder()

        if (!tag.isNullOrBlank()) {
            query.add(TermQuery(Term("tag", tag)), BooleanClause.Occur.MUST)
        }
        if (!subTag.isNullOrBlank()) {
            query.add(TermQuery(Term("subTag", subTag)), BooleanClause.Occur.MUST)
        }
        if (!jobId.isNullOrBlank()) {
            query.add(TermQuery(Term("jobId", jobId)), BooleanClause.Occur.MUST)
        }

        return query.add(IntPoint.newExactQuery("executeCount", executeCount ?: 1), BooleanClause.Occur.MUST)
            .add(TermQuery(Term("buildId", buildId)), BooleanClause.Occur.MUST)
    }

    private fun genLogLine(document: Document): LogLine {
        return LogLine(
            lineNo = document.getField("lineNo").stringValue().toLong(),
            timestamp = document.getField("timestamp").stringValue().toLong(),
            message = document.getField("message").stringValue(),
            tag = document.getField("tag").stringValue(),
            subTag = document.getField("subTag").stringValue(),
            jobId = document.getField("jobId").stringValue(),
            executeCount = document.getField("executeCount").stringValue().toInt()
        )
    }

    private fun getLineNoSort(): Sort {
        return Sort(SortedNumericSortField("lineNo", SortField.Type.LONG, false))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LuceneClient::class.java)
    }
}