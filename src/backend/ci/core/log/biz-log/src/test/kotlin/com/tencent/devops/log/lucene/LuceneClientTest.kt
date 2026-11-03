package com.tencent.devops.log.lucene

import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessageWithLineNo
import com.tencent.devops.log.util.LuceneIndexUtils
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.IntPoint
import org.apache.lucene.document.NumericDocValuesField
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LuceneClientTest {

    @Test
    fun writeAndQueryFollowLineOrder() {
        withClient { client ->
            val buildId = "b-12345678"
            client.indexBatchLog(buildId, listOf(
                doc(buildId, 1, "first", timestamp = 300),
                doc(buildId, 2, "second", timestamp = 100),
                doc(buildId, 3, "third", timestamp = 200)
            ), deterministicId = true)
            client.ensureIndex(INDEX)

            val page = client.search(INDEX, query(buildId, size = 10))
            assertEquals(listOf(2L, 3L, 1L), page.logs.map { it.lineNo })
            assertEquals(3L, page.totalHits)
            assertEquals("second", page.logs.first().message)
            assertTrue(client.indexExists(INDEX))
        }
    }

    @Test
    fun longMessageAndIdempotentRewrite() {
        withClient { client ->
            val buildId = "b-long0001"
            val message = "x".repeat(40_000)
            client.indexBatchLog(buildId, listOf(doc(buildId, 1, message)), deterministicId = true)
            client.indexBatchLog(buildId, listOf(doc(buildId, 1, "replaced")), deterministicId = true)

            val page = client.search(INDEX, query(buildId))
            assertEquals(1, page.logs.size)
            assertEquals("replaced", page.logs.first().message)
            assertEquals(1L, client.count(INDEX, query(buildId)))
        }
    }

    @Test
    fun beforeAfterAndPageUseLineRange() {
        withClient { client ->
            val buildId = "b-range001"
            val docs = (1L..25L).map { doc(buildId, it, "line-$it", timestamp = it * 1000) }
            client.indexBatchLog(buildId, docs, deterministicId = true)

            val before = client.search(INDEX, query(buildId, endLineNo = 10, size = 3, reverse = true))
            assertEquals(listOf(10L, 9L, 8L), before.logs.map { it.lineNo })
            assertTrue(before.totalHits > before.logs.size)

            val after = client.search(INDEX, query(buildId, startLineNo = 20, size = 3))
            assertEquals(listOf(20L, 21L, 22L), after.logs.map { it.lineNo })
            assertEquals(6L, after.totalHits)

            val page = client.search(INDEX, query(buildId, startLineNo = 11, endLineNo = 20, size = 10))
            assertEquals((11L..20L).toList(), page.logs.map { it.lineNo })
        }
    }

    @Test
    fun debugAndExecuteCountAreFiltered() {
        withClient { client ->
            val buildId = "b-filter01"
            client.indexBatchLog(
                buildId,
                listOf(
                    doc(buildId, 1, "normal"),
                    doc(buildId, 2, "debug", logType = LogType.DEBUG),
                    doc(buildId, 3, "retry", executeCount = 2)
                ),
                deterministicId = true
            )
            val hidden = client.search(INDEX, query(buildId, debug = false))
            assertEquals(listOf("normal"), hidden.logs.map { it.message })

            val withDebug = client.search(INDEX, query(buildId, debug = true))
            assertEquals(setOf("normal", "debug"), withDebug.logs.map { it.message }.toSet())

            val retry = client.search(INDEX, query(buildId, executeCount = 2, debug = true))
            assertEquals(listOf("retry"), retry.logs.map { it.message })
        }
    }

    @Test
    fun archiveKeepsLogsQueryableAndDeleteRemovesThem() {
        withClient { client ->
            val buildId = "b-clean001"
            client.indexBatchLog(buildId, listOf(doc(buildId, 1, "keep-me")), deterministicId = true)
            assertEquals(listOf(INDEX), client.listIndices())

            assertTrue(client.archiveIndex(INDEX))
            assertTrue(client.listIndices().isEmpty())
            assertEquals(listOf(INDEX), client.listIndices(cold = true))
            assertEquals("keep-me", client.search(INDEX, query(buildId)).logs.first().message)
            assertTrue(client.indexExists(INDEX))

            assertTrue(client.deleteIndex(INDEX))
            assertFalse(client.indexExists(INDEX))
            assertTrue(client.search(INDEX, query(buildId)).logs.isEmpty())
            assertEquals(0L, client.count(INDEX, query(buildId)))
        }
    }

    @Test
    fun downloadStreamsEveryLine() {
        withClient { client ->
            val buildId = "b-stream01"
            client.indexBatchLog(
                buildId,
                listOf(
                    doc(buildId, 1, "\u001b[31merror\u001b[m", timestamp = 1_700_000_000_000),
                    doc(buildId, 2, "ok", timestamp = 1_700_000_000_100)
                ),
                deterministicId = true
            )
            val output = ByteArrayOutputStream()
            client.fetchDocumentsStreaming(INDEX, query(buildId, debug = true)).write(output)
            val text = output.toString()
            assertTrue(text.contains("error"))
            assertTrue(text.contains("ok"))
            assertFalse(text.contains("\u001b[31m"))
        }
    }

    @Test
    fun legacyStringTimestampCanBeQueried() {
        val root = Files.createTempDirectory("lucene-log-legacy").toFile()
        try {
            val buildId = "b-legacy01"
            writeLegacy(root, buildId, listOf(
                legacyDoc(buildId, 1, 300, "old-first"),
                legacyDoc(buildId, 2, 100, "old-second")
            ))
            val client = LuceneClient(root.absolutePath, { INDEX }, null)
            val page = client.search(INDEX, query(buildId, size = 10))
            assertEquals(listOf(2L, 1L), page.logs.map { it.lineNo })
            assertEquals("old-second", page.logs.first().message)

            val tail = client.search(INDEX, query(buildId, endLineNo = 1, size = 1, reverse = true))
            assertEquals(listOf(1L), tail.logs.map { it.lineNo })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun concurrentWritesDoNotLoseLines() {
        withClient { client ->
            val buildId = "b-concurrent"
            val pool = Executors.newFixedThreadPool(2)
            val ready = CountDownLatch(1)
            val done = CountDownLatch(2)
            try {
                repeat(2) { worker ->
                    pool.submit {
                        ready.await()
                        client.indexBatchLog(
                            buildId,
                            listOf(doc(buildId, worker + 1L, "w$worker")),
                            deterministicId = true
                        )
                        done.countDown()
                    }
                }
                ready.countDown()
                assertTrue(done.await(20, TimeUnit.SECONDS))
            } finally {
                pool.shutdownNow()
            }
            val page = client.search(INDEX, query(buildId, size = 10))
            assertEquals(setOf(1L, 2L), page.logs.map { it.lineNo }.toSet())
        }
    }

    private fun withClient(block: (LuceneClient) -> Unit) {
        val root = Files.createTempDirectory("lucene-log").toFile()
        try {
            val client = LuceneClient(root.absolutePath, { INDEX }, null)
            block(client)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun query(
        buildId: String,
        debug: Boolean = false,
        executeCount: Int? = 1,
        startLineNo: Long? = null,
        endLineNo: Long? = null,
        size: Int = 100,
        reverse: Boolean = false
    ) = LuceneLogQuery(
        buildId = buildId,
        debug = debug,
        executeCount = executeCount,
        tag = "step",
        startLineNo = startLineNo,
        endLineNo = endLineNo,
        size = size,
        reverse = reverse
    )

    private fun doc(
        buildId: String,
        lineNo: Long,
        message: String,
        timestamp: Long = lineNo * 1000,
        logType: LogType = LogType.LOG,
        executeCount: Int = 1
    ): Document {
        return LuceneIndexUtils.getDocumentObject(
            buildId,
            LogMessageWithLineNo(
                tag = "step",
                subTag = "sub",
                containerHashId = "container",
                message = message,
                timestamp = timestamp,
                logType = logType,
                lineNo = lineNo,
                executeCount = executeCount,
                jobId = "job",
                stepId = "step-1"
            )
        )
    }

    private fun legacyDoc(buildId: String, lineNo: Long, timestamp: Long, message: String): Document {
        val doc = Document()
        doc.add(StringField("buildId", buildId, Field.Store.YES))
        doc.add(StringField("message", message, Field.Store.YES))
        doc.add(StringField("timestamp", timestamp.toString(), Field.Store.YES))
        doc.add(StringField("tag", "step", Field.Store.YES))
        doc.add(StringField("subTag", "", Field.Store.YES))
        doc.add(StringField("containerHashId", "container", Field.Store.YES))
        doc.add(StringField("jobId", "job", Field.Store.YES))
        doc.add(StringField("stepId", "step-1", Field.Store.YES))
        doc.add(StringField("logType", LogType.LOG.name, Field.Store.YES))
        doc.add(IntPoint("executeCount", 1))
        doc.add(StoredField("executeCount", 1))
        doc.add(NumericDocValuesField("lineNo", lineNo))
        doc.add(StoredField("lineNo", lineNo))
        return doc
    }

    private fun writeLegacy(root: java.io.File, buildId: String, documents: List<Document>) {
        val dir = java.io.File(root, "$INDEX/${buildId.take(4)}/$buildId")
        dir.mkdirs()
        val directory = FSDirectory.open(dir.toPath())
        val writer = IndexWriter(directory, IndexWriterConfig(noopAnalyzer()))
        try {
            writer.addDocuments(documents)
            writer.commit()
        } finally {
            writer.close()
            directory.close()
        }
    }

    private fun noopAnalyzer(): Analyzer {
        return object : Analyzer() {
            override fun createComponents(fieldName: String): TokenStreamComponents {
                val tokenizer = object : Tokenizer() {
                    override fun incrementToken(): Boolean = false
                }
                return TokenStreamComponents(tokenizer)
            }
        }
    }

    companion object {
        private const val INDEX = "log-2026-10-22"
    }
}
