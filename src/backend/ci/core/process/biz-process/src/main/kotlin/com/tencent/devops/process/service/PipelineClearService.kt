/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.normal.BuildHistoryDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
class PipelineClearService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val dslContext: DSLContext,
    private val pipelineBuildDao: PipelineBuildDao,
    private val buildHistoryDao: BuildHistoryDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val redisOperation: RedisOperation
) {

    companion object {
        private const val KEY_LOCK = "process:DeletedPipelineClearJob:lock"
        private const val KEY_CLEAR_THREAD_RUNNING = "process:clearThreadRunning"
        private const val VALUE_CLEAR_THREAD_RUNNING_FALSE = "0"
        private const val VALUE_CLEAR_THREAD_RUNNING_TRUE = "1"
        private const val KEY_CLEAR_THREAD_FINISHED = "process:clearThreadFinished"
        private const val VALUE_CLEAR_THREAD_FINISHED_FALSE = "0"
        private const val VALUE_CLEAR_THREAD_FINISHED_TRUE = "1"
        private const val PIPELINE_DELETE_BATCH_SIZE = 5000
        private const val BUILD_ID_DELETE_BATCH_SIZE = 10000
    }

    private val logger = LoggerFactory.getLogger(PipelineClearService::class.java)

    @Value("\${deletedPipelineStoreDays:30}")
    private val deletedPipelineStoreDays: Int = 30

    //最多5线程，用完立即销毁
    private val executorService = ThreadPoolExecutor(0, 5, 0, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

    //清理线程异常恢复
    fun recover(): Boolean {
        val lock = RedisLock(redisOperation, KEY_LOCK, 10)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return false
            }
            //获取锁后
            val clearThreadRunning = redisOperation.get(KEY_CLEAR_THREAD_RUNNING)
            val clearThreadFinished = redisOperation.get(KEY_CLEAR_THREAD_FINISHED)
            if (clearThreadFinished == VALUE_CLEAR_THREAD_FINISHED_FALSE) {
                //上一次调用未完成
                return if (clearThreadRunning == VALUE_CLEAR_THREAD_RUNNING_TRUE) {
                    //已有清理线程正在跑
                    logger.info("pipeline clear thread already running")
                    false
                } else {
                    //清理线程被意外终止，需要重启
                    logger.info("recover pipeline clear thread")
                    doClear()
                }
            }
        } catch (t: Throwable) {
            logger.warn("recover pipeline clear thread failed", t)
        } finally {
            lock.unlock()
        }
        return false
    }

    fun clear(): Boolean {
        val lock = RedisLock(redisOperation, KEY_LOCK, 10)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return false
            }
            //获取锁后
            val clearThreadRunning = redisOperation.get(KEY_CLEAR_THREAD_RUNNING)
            val clearThreadFinished = redisOperation.get(KEY_CLEAR_THREAD_FINISHED)
            if (clearThreadFinished == null) {
                //首次调用
                return doClear()
            } else if (clearThreadFinished == VALUE_CLEAR_THREAD_FINISHED_TRUE) {
                //上一次清理已完成再次调用
                return doClear()
            } else {
                //上一次调用未完成
                if (clearThreadRunning == VALUE_CLEAR_THREAD_RUNNING_TRUE) {
                    //已有清理线程正在跑
                    return false
                } else {
                    //清理线程被意外终止，需要重启
                    return doClear()
                }
            }
        } catch (t: Throwable) {
            logger.warn("trigger pipeline clear thread failed", t)
        } finally {
            lock.unlock()
        }
        return false
    }

    private fun doClear(): Boolean {
        executorService.submit {
            //开一个心跳子线程
            val heartBeatThread = Thread {
                try {
                    while (true) {
                        redisOperation.set(KEY_CLEAR_THREAD_RUNNING, VALUE_CLEAR_THREAD_RUNNING_TRUE, expiredInSecond = 5L)
                        Thread.sleep(3000)
                    }
                } catch (e: InterruptedException) {
                    redisOperation.set(KEY_CLEAR_THREAD_RUNNING, VALUE_CLEAR_THREAD_RUNNING_FALSE, expiredInSecond = 5L)
                    logger.info("deleted pipeline clear task finished")
                }
            }
            heartBeatThread.start()

            val watch = StopWatch("clear deleted Task")
            try {
                redisOperation.set(KEY_CLEAR_THREAD_FINISHED, VALUE_CLEAR_THREAD_FINISHED_FALSE, expired = false)

                logger.info("clear pipelines deleted before $deletedPipelineStoreDays days")
                val deleteTime = LocalDateTime.now().minusDays(deletedPipelineStoreDays.toLong())
                //查出所有被删除超过过期时间的流水线
                val pipelinesLimit = 500
                var pipelineBatchNum = 0
                var deletedPipelines: List<PipelineInfo>
                do {
                    pipelineBatchNum += 1
                    deletedPipelines = pipelineRepositoryService.listDeletePipelineBefore(deleteTime, 0, pipelinesLimit)
                    watch.start("delete Task $pipelineBatchNum")
                    val deletedPipelineIds = deletedPipelines.map { it.pipelineId }
                    logger.info("deletedPipelineIds=${deletedPipelineIds.size},(${deletedPipelineIds.joinToString()})")
                    val pipelineBuildBaseInfoList = mutableListOf<PipelineBuildBaseInfo>()
                    var pipelinesCount = 0
                    var buildIdsCount = 0
                    val buildIdsLimit = 1000
                    deletedPipelines.forEach { pipelineInfo ->
                        pipelinesCount += 1
                        var buildIds: List<String>
                        var offset = 0
                        var buildBatchNum = 0
                        do {
                            buildBatchNum += 1
                            val subWatch = StopWatch("clear pipeline ${pipelineInfo.pipelineId} batch $buildBatchNum")
                            subWatch.start("${pipelineInfo.pipelineId} delete sub Task $buildBatchNum")
                            buildIds = pipelineBuildDao.listPipelineBuildInfo(
                                dslContext = dslContext,
                                projectId = pipelineInfo.projectId,
                                pipelineId = pipelineInfo.pipelineId,
                                offset = offset,
                                limit = buildIdsLimit
                            ).map { it.buildId }
                            offset += buildIdsLimit
                            buildIdsCount += buildIds.size
                            pipelineBuildBaseInfoList.add(PipelineBuildBaseInfo(pipelineInfo.projectId, pipelineInfo.pipelineId, buildIds))
                            //流水线数量/构建数量任意一个达到阈值就开始删除
                            if (pipelinesCount >= PIPELINE_DELETE_BATCH_SIZE || buildIdsCount >= BUILD_ID_DELETE_BATCH_SIZE) {
                                deleteRelatedAndBuildData(pipelineBuildBaseInfoList)
                                pipelinesCount = 0
                                buildIdsCount = 0
                                offset = 0
                                pipelineBuildBaseInfoList.clear()
                            }
                            subWatch.stop()
                            logger.info(subWatch.toString())
                        } while (buildIds.size == buildIdsLimit)
                    }
                    if (pipelineBuildBaseInfoList.isNotEmpty()) {
                        deleteRelatedAndBuildData(pipelineBuildBaseInfoList)
                    }
                    //删主干流水线数据
                    pipelineInfoDao.deletePipelinesHardly(dslContext, deletedPipelineIds)
                    watch.stop()
                } while (deletedPipelines.size == pipelinesLimit)
                heartBeatThread.interrupt()
                redisOperation.set(KEY_CLEAR_THREAD_FINISHED, VALUE_CLEAR_THREAD_FINISHED_TRUE, expired = false)
            } catch (e: Exception) {
                logger.error("fail to clear deleted pipelines", e)
            } finally {
                logger.info("Clear Deleted Task Time Consuming:$watch")
            }
        }
        return true
    }

    fun deleteRelatedAndBuildData(pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>) {
        //删关联数据
        pipelineRepositoryService.deletePipelinesHardly(pipelineBuildBaseInfoList, ChannelCode.BS)
        //删主干构建数据
        buildHistoryDao.deletePipelinesHardly(dslContext, pipelineBuildBaseInfoList)
    }
}
