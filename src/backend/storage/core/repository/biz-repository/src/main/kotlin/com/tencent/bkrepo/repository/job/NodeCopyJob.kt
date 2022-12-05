package com.tencent.bkrepo.repository.job

import com.tencent.bkrepo.common.api.collection.concurrent.ConcurrentHashSet
import com.tencent.bkrepo.common.api.kotlin.adapter.util.CompletableFutureKotlin
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.constant.DEFAULT_STORAGE_CREDENTIALS_KEY
import com.tencent.bkrepo.repository.constant.SHARDING_COUNT
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.job.base.CenterNodeJob
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.service.file.FileReferenceService
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import org.apache.commons.logging.LogFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 复制节点任务
 * 因为支持快速拷贝，用户在拷贝时，只是增加了存储实例的映射，并没有真正的进行数据拷贝
 * */
@Component
class NodeCopyJob(
    private val nodeDao: NodeDao,
    private val storageService: StorageService,
    private val storageCredentialService: StorageCredentialService,
    private val fileReferenceService: FileReferenceService
) : CenterNodeJob() {

    @Scheduled(cron = "0 0 2/6 * * ?") // 2点开始，6小时执行一次
    override fun start() {
        super.start()
    }

    override fun getLockAtMostFor(): Duration = Duration.ofDays(7)

    override fun run() {
        val mongoTemplate = nodeDao.determineMongoTemplate()
        val jobContext = JobContext()
        val completableFutures = arrayOfNulls<CompletableFuture<Void>>(SHARDING_COUNT)
        val processed = AtomicInteger()
        for (sequence in 0 until SHARDING_COUNT) {
            CompletableFuture.runAsync {
                doCopyTaskForEachCollection(sequence, mongoTemplate, jobContext)
                processed.incrementAndGet()
                logger.info("Process Info: $processed/$SHARDING_COUNT processed/sum Tables")
            }.apply { completableFutures[sequence] = this }
        }
        CompletableFutureKotlin.allOf(completableFutures).join()
        logProcess(jobContext)
    }

    private fun doCopyTaskForEachCollection(
        sequence: Int,
        mongoTemplate: MongoTemplate,
        jobContext: JobContext
    ) {
        var pageNum = 0
        val pageSize = 10000
        var querySize: Int
        do {
            val collectionName = nodeDao.parseSequenceToCollectionName(sequence)
            val criteria = where(TNode::copyFromCredentialsKey).ne(null)
            val query = Query.query(criteria).with(PageRequest.of(pageNum, pageSize))
            val copyNodeList = mongoTemplate.find(query, TNode::class.java, collectionName)
            val futures = copyNodeList.map {
                CompletableFuture.runAsync {
                    copy(it, jobContext)
                }
            }.toTypedArray()
            CompletableFutureKotlin.allOf(futures).join()
            querySize = copyNodeList.size
            pageNum++
            logProcess(jobContext)
        } while (querySize == pageSize)
    }

    private fun copy(node: TNode, jobContext: JobContext) {
        with(jobContext) {
            var digest: String? = null
            var srcCredentials: StorageCredentials? = null
            var dstCredentials: StorageCredentials? = null
            try {
                digest = node.sha256!!
                srcCredentials = keyToStorageCredentials(node.copyFromCredentialsKey!!, this)
                val repositoryDetail = repositoryDetail(node)
                dstCredentials = repositoryDetail.storageCredentials
                fileReferenceCheck(dstCredentials, node, digest)
                val targetCopy = TargetCopy(targetCredentialsKey = dstCredentials?.key, digest = digest)
                if (alreadyCopySet.contains(targetCopy)) {
                    afterCopySuccess(node)
                    success.incrementAndGet()
                    return
                }
                if (storageService.exist(digest, srcCredentials)) {
                    storageService.copy(digest, srcCredentials, dstCredentials)
                    afterCopySuccess(node)
                    alreadyCopySet.add(targetCopy)
                    success.incrementAndGet()
                } else {
                    fileMissing.incrementAndGet()
                    logger.warn("File[$digest] is missing on [$srcCredentials], skip copy.")
                }
            } catch (ignored: Exception) {
                failed.incrementAndGet()
                logger.error("Failed to copy file[$digest] from [$srcCredentials] to [$dstCredentials].", ignored)
            } finally {
                total.incrementAndGet()
            }
        }
    }

    /**
     * 拷贝成功后，删除存储映射
     * */
    private fun afterCopySuccess(node: TNode) {
        node.copyFromCredentialsKey = null
        node.copyIntoCredentialsKey = null
        nodeDao.save(node)
    }

    /**
     * 文件引用核对
     * 拷贝时的存储实例与当前仓库的存储实例不同，说明仓库已经迁移到其他存储实例，
     * 则原先增加引用的存储实例，文件引用要减1还原
     * 当前存储实例引用加1
     * */
    private fun fileReferenceCheck(
        dstCredentials: StorageCredentials?,
        node: TNode,
        digest: String
    ) {
        val dstCredentialsKey = dstCredentials?.key ?: DEFAULT_STORAGE_CREDENTIALS_KEY
        if (dstCredentialsKey != node.copyIntoCredentialsKey) {
            fileReferenceService.decrement(digest, node.copyIntoCredentialsKey)
            fileReferenceService.increment(digest, dstCredentialsKey)
        }
    }

    private fun repositoryDetail(node: TNode): RepositoryDetail {
        val repositoryId = ArtifactContextHolder.RepositoryId(node.projectId, node.repoName)
        return ArtifactContextHolder.getRepoDetail(repositoryId)
    }

    private fun keyToStorageCredentials(
        credentialsKey: String,
        jobContext: JobContext
    ): StorageCredentials? {
        with(jobContext) {
            return cacheMap.getOrPut(credentialsKey) {
                storageCredentialService.findByKey(credentialsKey) ?: return null
            }
        }
    }

    private fun logProcess(jobContext: JobContext) {
        with(jobContext) {
            logger.info(
                "Copy [$total] node, success[$success], " +
                    "failed[$failed], file missing[$fileMissing]."
            )
        }
    }

    data class JobContext(
        val cacheMap: ConcurrentHashMap<String, StorageCredentials?> = ConcurrentHashMap(),
        val alreadyCopySet: ConcurrentHashSet<TargetCopy> = ConcurrentHashSet(),
        var total: AtomicLong = AtomicLong(),
        var success: AtomicLong = AtomicLong(),
        var failed: AtomicLong = AtomicLong(),
        var fileMissing: AtomicLong = AtomicLong()
    )

    data class TargetCopy(
        val targetCredentialsKey: String?,
        val digest: String
    )

    companion object {
        private val logger = LogFactory.getLog(NodeCopyJob::class.java)
    }
}
