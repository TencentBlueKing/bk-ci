/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.task

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.exception.SystemErrorException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.redis.RedisLock
import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.common.scanner.pojo.scanner.Scanner
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.scanner.configuration.ScannerProperties
import com.tencent.bkrepo.scanner.dao.FileScanResultDao
import com.tencent.bkrepo.scanner.dao.PlanArtifactLatestSubScanTaskDao
import com.tencent.bkrepo.scanner.dao.ProjectScanConfigurationDao
import com.tencent.bkrepo.scanner.dao.ScanTaskDao
import com.tencent.bkrepo.scanner.dao.SubScanTaskDao
import com.tencent.bkrepo.scanner.event.SubtaskStatusChangedEvent
import com.tencent.bkrepo.scanner.metrics.ScannerMetrics
import com.tencent.bkrepo.scanner.model.TFileScanResult
import com.tencent.bkrepo.scanner.model.TPlanArtifactLatestSubScanTask
import com.tencent.bkrepo.scanner.model.TProjectScanConfiguration
import com.tencent.bkrepo.scanner.model.TSubScanTask
import com.tencent.bkrepo.scanner.pojo.Node
import com.tencent.bkrepo.scanner.pojo.ScanTask
import com.tencent.bkrepo.scanner.pojo.ScanTaskStatus.FINISHED
import com.tencent.bkrepo.scanner.pojo.ScanTaskStatus.SCANNING_SUBMITTED
import com.tencent.bkrepo.scanner.pojo.ScanTaskStatus.SCANNING_SUBMITTING
import com.tencent.bkrepo.scanner.pojo.SubScanTask
import com.tencent.bkrepo.scanner.service.ScannerService
import com.tencent.bkrepo.scanner.task.ScanTaskSchedulerConfiguration.Companion.SCAN_TASK_SCHEDULER_THREAD_POOL_BEAN_NAME
import com.tencent.bkrepo.scanner.task.iterator.IteratorManager
import com.tencent.bkrepo.scanner.task.queue.SubScanTaskQueue
import com.tencent.bkrepo.scanner.utils.Converter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Component
class DefaultScanTaskScheduler @Autowired constructor(
    private val iteratorManager: IteratorManager,
    private val subScanTaskQueue: SubScanTaskQueue,
    private val scannerService: ScannerService,
    private val repositoryClient: RepositoryClient,
    private val subScanTaskDao: SubScanTaskDao,
    private val planArtifactLatestSubScanTaskDao: PlanArtifactLatestSubScanTaskDao,
    private val scanTaskDao: ScanTaskDao,
    private val fileScanResultDao: FileScanResultDao,
    private val projectScanConfigurationDao: ProjectScanConfigurationDao,
    @Qualifier(SCAN_TASK_SCHEDULER_THREAD_POOL_BEAN_NAME)
    private val executor: ThreadPoolTaskExecutor,
    private val scannerMetrics: ScannerMetrics,
    private val redisOperation: RedisOperation,
    private val publisher: ApplicationEventPublisher,
    private val scannerProperties: ScannerProperties
) : ScanTaskScheduler {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val repoInfoCache: LoadingCache<String, RepositoryInfo> = CacheBuilder.newBuilder()
        .maximumSize(DEFAULT_REPO_INFO_CACHE_SIZE)
        .expireAfterWrite(DEFAULT_REPO_INFO_CACHE_DURATION_MINUTES, TimeUnit.MINUTES)
        .build(CacheLoader.from { key -> loadRepoInfo(key!!) })

    @Autowired
    private lateinit var self: DefaultScanTaskScheduler

    override fun schedule(scanTask: ScanTask) {
        executor.execute { enqueueAllSubScanTask(scanTask) }
    }

    override fun schedule(subScanTask: SubScanTask): Boolean {
        val enqueued = subScanTaskQueue.enqueue(subScanTask)
        logger.info(
            "subTask[${subScanTask.taskId}] of parentTask[${subScanTask.parentScanTaskId}] enqueued[$enqueued]]"
        )
        return enqueued
    }

    override fun notify(projectId: String): Int {
        // 加锁避免多个进程唤醒项目子任务导致唤醒的任务数量超过配额
        // 此处不要求锁绝对可靠，极端情况下有两个进程同时持有锁时只会导致project执行的任务数量超过配额
        val lockKey = notifySubtaskLockKey(projectId)
        val lock = RedisLock(redisOperation, lockKey, DEFAULT_NOTIFY_SUBTASK_LOCK_TIMEOUT_SECONDS)

        lock.use {
            it.lock()
            val subtaskCountLimit = projectScanConfigurationDao.findByProjectId(projectId)?.subScanTaskCountLimit
                ?: scannerProperties.defaultProjectSubScanTaskCountLimit
            val countToUpdate = (subtaskCountLimit - subScanTaskDao.scanningCount(projectId)).toInt()
            return if (countToUpdate > 0) {
                val notifiedCount = subScanTaskDao.notify(projectId, countToUpdate)?.modifiedCount?.toInt() ?: 0
                scannerMetrics.decSubtaskCountAndGet(SubScanTaskStatus.BLOCKED, notifiedCount.toLong())
                scannerMetrics.incSubtaskCountAndGet(SubScanTaskStatus.CREATED, notifiedCount.toLong())
                notifiedCount
            } else {
                0
            }
        }
    }

    /**
     * 创建扫描子任务，并提交到扫描队列
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private fun enqueueAllSubScanTask(scanTask: ScanTask) {
        // 设置扫描任务状态为提交子任务中
        val lastModifiedDate = LocalDateTime.parse(scanTask.lastModifiedDateTime, DateTimeFormatter.ISO_DATE_TIME)
        // 更新任务状态失败，表示任务已经被提交过，不再重复处理，直接返回
        if (scanTaskDao.updateStatus(scanTask.taskId, SCANNING_SUBMITTING, lastModifiedDate).modifiedCount == 0L) {
            return
        }

        scannerMetrics.incTaskCountAndGet(SCANNING_SUBMITTING)

        // 加锁避免有其他任务正在提交这个project的任务，导致project执行中的任务数量统计错误
        // 此处不要求锁绝对可靠，极端情况下有两个进程同时持有锁时只会导致project执行的任务数量超过配额
        val lock = scanTask.scanPlan?.projectId?.let {
            val lockKey = submitScanTaskLockKey(it)
            RedisLock(redisOperation, lockKey, DEFAULT_SUBMIT_PROJECT_SUBTASK_LOCK_TIMEOUT_SECONDS)
        }

        val (submittedSubTaskCount, reuseResultTaskCount) = try {
            lock?.lock()
            submit(scanTask)
        } finally {
            lock?.unlock()
        }

        // 更新任务状态为所有子任务已提交
        logger.info(
            "submit $submittedSubTaskCount sub tasks, $reuseResultTaskCount sub tasks reuse result, " +
                "update task[${scanTask.taskId}] status to SCANNING_SUBMITTED"
        )
        scanTaskDao.updateStatus(scanTask.taskId, SCANNING_SUBMITTED)
        scannerMetrics.incTaskCountAndGet(SCANNING_SUBMITTED)

        // 没有提交任何子任务，直接设置为任务扫描结束
        if (submittedSubTaskCount == 0L) {
            val now = LocalDateTime.now()
            scanTaskDao.taskFinished(scanTask.taskId, now, now)
            scannerMetrics.incTaskCountAndGet(FINISHED)
            logger.info("scan finished, task[${scanTask.taskId}]")
        }
    }

    /**
     * 遍历[scanTask]指定的所有制品，提交子任务
     *
     * @return first 提交的子任务数量 second 复用扫描结果的子任务数量
     */
    private fun submit(scanTask: ScanTask): Pair<Long, Long> {
        val scanner = scannerService.get(scanTask.scanner)
        val projectId = scanTask.scanPlan?.projectId
        var projectScanConfiguration = projectId?.let { projectScanConfigurationDao.findByProjectId(it) }
        logger.info("submitting sub tasks of task[${scanTask.taskId}], scanner: [${scanner.name}]")

        var scanningCount = projectId?.let { subScanTaskDao.scanningCount(it) }
        var submittedSubTaskCount = 0L
        var reuseResultTaskCount = 0L
        val subScanTasks = ArrayList<TSubScanTask>()
        val finishedSubScanTasks = ArrayList<TPlanArtifactLatestSubScanTask>()
        val nodeIterator = iteratorManager.createNodeIterator(scanTask, false)
        for (node in nodeIterator) {
            // 未使用扫描方案的情况直接取node的projectId
            projectScanConfiguration = projectScanConfiguration
                ?: projectScanConfigurationDao.findByProjectId(node.projectId)
            scanningCount = scanningCount ?: subScanTaskDao.scanningCount(node.projectId)

            val storageCredentialsKey = repoInfoCache
                .get(generateKey(node.projectId, node.repoName))
                .storageCredentialsKey

            // 文件已存在扫描结果，跳过扫描
            val existsFileScanResult =
                fileScanResultDao.find(storageCredentialsKey, node.sha256, scanner.name, scanner.version)
            if (existsFileScanResult != null && !scanTask.force) {
                logger.info("skip scan file[${node.sha256}], credentials[$storageCredentialsKey]")
                val finishedSubtask = createFinishedSubTask(scanTask, existsFileScanResult, node, storageCredentialsKey)
                finishedSubScanTasks.add(finishedSubtask)
            } else {
                // 添加到扫描任务队列
                val status = status(scanningCount, projectScanConfiguration)
                subScanTasks.add(createSubTask(scanTask, scanner, node, storageCredentialsKey, status))
                if (status == SubScanTaskStatus.CREATED) {
                    scanningCount++
                }
            }

            // 批量保存重用扫描结果的任务
            if (finishedSubScanTasks.size == BATCH_SIZE || !nodeIterator.hasNext()) {
                self.save(finishedSubScanTasks)
                scannerMetrics.incReuseResultSubtaskCount(finishedSubScanTasks.size.toLong())
                reuseResultTaskCount += finishedSubScanTasks.size
                finishedSubScanTasks.clear()
            }

            // 批量提交子任务
            if (subScanTasks.size == BATCH_SIZE || !nodeIterator.hasNext()) {
                self.submit(subScanTasks, scanner)
                submittedSubTaskCount += subScanTasks.size
                subScanTasks.clear()
            }
        }

        // 任务提交结束后尝试唤醒一个任务，避免全部任务都处于BLOCK状态
        projectId?.let { self.notify(it) }

        return Pair(submittedSubTaskCount, reuseResultTaskCount)
    }

    @Transactional(rollbackFor = [Throwable::class])
    fun submit(subScanTasks: List<TSubScanTask>, scanner: Scanner) {
        if (subScanTasks.isEmpty()) {
            return
        }
        val subTasks = self.saveSubTasks(subScanTasks).map { convert(it, scanner) }
        val enqueuedTasks = subScanTaskQueue.enqueue(subTasks)
        logger.info("${enqueuedTasks.size} subTasks enqueued")

        if (enqueuedTasks.isNotEmpty()) {
            subScanTaskDao.updateStatus(enqueuedTasks, SubScanTaskStatus.ENQUEUED)
            scannerMetrics.decSubtaskCountAndGet(SubScanTaskStatus.CREATED, enqueuedTasks.size.toLong())
            scannerMetrics.incSubtaskCountAndGet(SubScanTaskStatus.ENQUEUED, enqueuedTasks.size.toLong())
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    fun save(tasks: List<TPlanArtifactLatestSubScanTask>) {
        if (tasks.isEmpty()) {
            return
        }
        planArtifactLatestSubScanTaskDao.replace(tasks)
        tasks.forEach { publisher.publishEvent(SubtaskStatusChangedEvent(null, it)) }

        // 更新当前正在扫描的任务数
        val overview = HashMap<String, Number>()
        tasks.forEach { task ->
            task.scanResultOverview?.forEach { (k, v) ->
                overview[k] = overview.getOrDefault(k, 0L).toLong() + v.toLong()
            }
        }

        val task = tasks.first()
        scanTaskDao.updateScanResult(task.parentScanTaskId, tasks.size, overview, success = true, reuseResult = true)
        scannerMetrics.incSubtaskCountAndGet(SubScanTaskStatus.SUCCESS, tasks.size.toLong())
    }

    fun createSubTask(
        scanTask: ScanTask,
        scanner: Scanner,
        node: Node,
        credentialKey: String? = null,
        status: SubScanTaskStatus = SubScanTaskStatus.CREATED
    ): TSubScanTask {
        with(node) {
            val now = LocalDateTime.now()
            val repoInfo = repoInfoCache.get(generateKey(projectId, repoName))
            return TSubScanTask(
                createdBy = scanTask.createdBy,
                createdDate = now,
                lastModifiedBy = scanTask.createdBy,
                lastModifiedDate = now,

                parentScanTaskId = scanTask.taskId,
                planId = scanTask.scanPlan?.id,

                projectId = projectId,
                repoName = repoName,
                repoType = repoInfo.type.name,
                packageKey = packageKey,
                version = packageVersion,
                fullPath = fullPath,
                artifactName = artifactName,

                status = status.name,
                executedTimes = 0,
                scanner = scanTask.scanner,
                scannerType = scanTask.scannerType,
                sha256 = sha256,
                size = size,
                credentialsKey = credentialKey
            )
        }
    }

    fun createFinishedSubTask(
        scanTask: ScanTask,
        fileScanResult: TFileScanResult,
        node: Node,
        credentialKey: String? = null,
        resultStatus: String = SubScanTaskStatus.SUCCESS.name
    ): TPlanArtifactLatestSubScanTask {
        with(node) {
            val now = LocalDateTime.now()
            val repoInfo = repoInfoCache.get(generateKey(projectId, repoName))
            val overview = fileScanResult
                .scanResult[scanTask.scanner]
                ?.overview
                ?.let { Converter.convert(it) }
            return TPlanArtifactLatestSubScanTask(
                createdBy = scanTask.createdBy,
                createdDate = now,
                lastModifiedBy = scanTask.createdBy,
                lastModifiedDate = now,
                startDateTime = now,
                finishedDateTime = now,

                parentScanTaskId = scanTask.taskId,
                planId = scanTask.scanPlan?.id,

                projectId = projectId,
                repoName = repoName,
                repoType = repoInfo.type.name,
                packageKey = packageKey,
                version = packageVersion,
                fullPath = fullPath,
                artifactName = artifactName,

                status = resultStatus,
                scanner = scanTask.scanner,
                scannerType = scanTask.scannerType,
                sha256 = sha256,
                size = size,
                credentialsKey = credentialKey,

                scanResultOverview = overview
            )
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    fun saveSubTasks(subScanTasks: List<TSubScanTask>): Collection<TSubScanTask> {
        if (subScanTasks.isEmpty()) {
            return emptyList()
        }
        val tasks = subScanTaskDao.insert(subScanTasks)

        // 保存方案制品最新扫描记录
        val planArtifactLatestSubScanTasks = tasks.map { TPlanArtifactLatestSubScanTask.convert(it, it.status) }
        planArtifactLatestSubScanTaskDao.replace(planArtifactLatestSubScanTasks)
        planArtifactLatestSubScanTasks.forEach { publisher.publishEvent(SubtaskStatusChangedEvent(null, it)) }

        // 更新当前正在扫描的任务数
        val task = tasks.first()
        scanTaskDao.updateScanningCount(task.parentScanTaskId, tasks.size)

        // 统计BLOCKED与CREATED任务数量
        val createdTasks = tasks.filter { it.status == SubScanTaskStatus.CREATED.name }
        val blockedTaskCount = tasks.size - createdTasks.size
        if (blockedTaskCount != 0) {
            scannerMetrics.incSubtaskCountAndGet(SubScanTaskStatus.BLOCKED, blockedTaskCount.toLong())
        }
        scannerMetrics.incSubtaskCountAndGet(SubScanTaskStatus.CREATED, createdTasks.size.toLong())
        logger.info("${createdTasks.size} created subtasks and $blockedTaskCount blocked subtasks saved")

        return createdTasks
    }

    override fun resume(scanTask: ScanTask) {
        TODO("Not yet implemented")
    }

    override fun pause(scanTask: ScanTask) {
        TODO("Not yet implemented")
    }

    override fun stop(scanTask: ScanTask) {
        TODO("Not yet implemented")
    }

    private fun loadRepoInfo(key: String): RepositoryInfo {
        val (projectId, repoName) = fromKey(key)
        val repoRes = repositoryClient.getRepoInfo(projectId, repoName)
        if (repoRes.isNotOk()) {
            logger.error(
                "Get repo info failed: code[${repoRes.code}], message[${repoRes.message}]," +
                    " projectId[$projectId], repoName[$repoName]"
            )
            throw SystemErrorException(CommonMessageCode.SYSTEM_ERROR, repoRes.message ?: "")
        }
        return repoRes.data ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, key)
    }

    private fun generateKey(projectId: String, repoName: String) = "$projectId$REPO_SPLIT$repoName"
    private fun fromKey(key: String): Pair<String, String> {
        val indexOfRepoSplit = key.indexOf(REPO_SPLIT)
        val projectId = key.substring(0, indexOfRepoSplit)
        val repoName = key.substring(indexOfRepoSplit + REPO_SPLIT.length, key.length)
        return Pair(projectId, repoName)
    }

    private fun submitScanTaskLockKey(projectId: String) = "scanner:lock:submit:$projectId"
    private fun notifySubtaskLockKey(projectId: String) = "scanner:lock:notify:$projectId:subtask"

    private fun convert(subScanTask: TSubScanTask, scanner: Scanner): SubScanTask {
        return SubScanTask(
            taskId = subScanTask.id!!,
            parentScanTaskId = subScanTask.parentScanTaskId,
            scanner = scanner,
            sha256 = subScanTask.sha256,
            size = subScanTask.size,
            credentialsKey = subScanTask.credentialsKey
        )
    }

    /**
     * 根据扫描数量是否超过限制返回扫描状态
     *
     * @param scanningCount 正在扫描的任务数量
     * @param projectConfiguration 项目扫描配置
     *
     */
    private fun status(
        scanningCount: Long,
        projectConfiguration: TProjectScanConfiguration?
    ): SubScanTaskStatus {
        val limitSubScanTaskCount = projectConfiguration?.subScanTaskCountLimit
            ?: scannerProperties.defaultProjectSubScanTaskCountLimit
        if (scanningCount >= limitSubScanTaskCount.toLong()) {
            return SubScanTaskStatus.BLOCKED
        }
        return SubScanTaskStatus.CREATED
    }

    companion object {
        private const val REPO_SPLIT = "::repo::"
        private const val DEFAULT_REPO_INFO_CACHE_SIZE = 1000L
        private const val DEFAULT_REPO_INFO_CACHE_DURATION_MINUTES = 60L
        private const val DEFAULT_SUBMIT_PROJECT_SUBTASK_LOCK_TIMEOUT_SECONDS = 1200L
        private const val DEFAULT_NOTIFY_SUBTASK_LOCK_TIMEOUT_SECONDS = 30L

        /**
         * 批量提交子任务数量
         */
        private const val BATCH_SIZE = 20
    }
}
