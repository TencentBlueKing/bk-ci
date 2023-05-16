package com.tencent.devops.buildless

import com.tencent.devops.buildless.common.ErrorCodeEnum
import com.tencent.devops.buildless.config.BuildLessConfig
import com.tencent.devops.buildless.exception.BuildLessException
import com.tencent.devops.buildless.pojo.BuildLessPoolInfo
import com.tencent.devops.buildless.pojo.BuildLessStartInfo
import com.tencent.devops.buildless.pojo.BuildLessTask
import com.tencent.devops.buildless.pojo.RejectedExecutionType
import com.tencent.devops.buildless.rejected.RejectedExecutionFactory
import com.tencent.devops.buildless.service.BuildLessContainerService
import com.tencent.devops.buildless.utils.CommonUtils
import com.tencent.devops.buildless.utils.RedisUtils
import com.tencent.devops.buildless.utils.ThreadPoolName
import com.tencent.devops.buildless.utils.ThreadPoolUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@Component
class ContainerPoolExecutor @Autowired constructor(
    private val redisUtils: RedisUtils,
    private val buildLessConfig: BuildLessConfig,
    private val rejectedExecutionFactory: RejectedExecutionFactory,
    private val buildLessContainerService: BuildLessContainerService
) {

    fun execute(buildLessStartInfo: BuildLessStartInfo) {
        with(buildLessStartInfo) {
            if (requestRejected(this)) {
                return
            }

            logger.info("$buildId|$vmSeqId|$executionCount left push buildLessReadyTask")
            redisUtils.leftPushBuildLessReadyTask(
                BuildLessTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    executionCount = executionCount,
                    vmSeqId = vmSeqId,
                    agentId = agentId,
                    secretKey = secretKey
                )
            )

            // FOLLOW或JUMP策略下，尝试创建超载容器
            if (rejectedExecutionType == RejectedExecutionType.FOLLOW_POLICY ||
                rejectedExecutionType == RejectedExecutionType.JUMP_POLICY
            ) {
                addContainer(true)
            }
        }
    }

    fun addContainer(oversold: Boolean = false) {
        val corePoolSize = buildLessConfig.coreContainerPool
        val maximumPoolSize = buildLessConfig.maxContainerPool

        val lock = mainLock
        if (lock.tryLock(20, TimeUnit.SECONDS)) {
            try {
                val runningContainerCount = buildLessContainerService.getRunningPoolSize(true)
                logger.info("Container pool add container, running containers: $runningContainerCount")

                // 非超载模式下，把容器池填满corePoolSize
                if (!oversold && (runningContainerCount < corePoolSize)) {
                    createBuildLessPoolContainer(corePoolSize - runningContainerCount)
                }

                // 超载模式下，将容器池填充至maximumPoolSize
                if (oversold && (runningContainerCount < maximumPoolSize)) {
                    createBuildLessPoolContainer(maximumPoolSize - runningContainerCount)
                }
            } finally {
                lock.unlock()
            }
        } else {
            logger.warn("add container tryLock 60s failed.")
        }
    }

    fun getContainerStatus(containerId: String): BuildLessPoolInfo? {
        synchronized(containerId.intern()) {
            return redisUtils.getBuildLessPoolContainer(containerId)
        }
    }

    fun clearTimeoutContainers() {
        val containerList = buildLessContainerService.getDockerRunTimeoutContainers()
        containerList.forEach {
            synchronized(CommonUtils.formatContainerId(it).intern()) {
                buildLessContainerService.stopContainer("clear timeout", "", it)
            }
        }
    }

    private fun createBuildLessPoolContainer(index: Int = 1) {
        val startTime = System.currentTimeMillis()
        val latch = CountDownLatch(index)
        for (i in 1..index) {
            ThreadPoolUtils.getInstance().getThreadPool(ThreadPoolName.ADD_CONTAINER.name).submit {
                buildLessContainerService.createContainer()
                latch.countDown()
            }
        }

        latch.await()
        logger.info("Finish add container. count: $index, cost: ${System.currentTimeMillis() - startTime}")
    }

    private fun requestRejected(buildLessStartInfo: BuildLessStartInfo): Boolean {
        val lock = idlePoolLock
        try {
            if (!lock.tryLock(60, TimeUnit.SECONDS)) {
                throw BuildLessException(
                    errorType = ErrorCodeEnum.GET_LOCK_FAILED.errorType,
                    errorCode = ErrorCodeEnum.GET_LOCK_FAILED.errorCode,
                    errorMsg = ErrorCodeEnum.GET_LOCK_FAILED.getFormatErrorMessage()
                )
            }

            retry@ while (true) {
                val idlePoolSize = redisUtils.getIdlePoolSize()

                // 无空闲容器时执行拒绝策略
                logger.info("${buildLessStartInfo.buildId}|${buildLessStartInfo.vmSeqId} idlePoolSize: $idlePoolSize")
                if (idlePoolSize <= 0L) {
                    return !rejectedExecutionFactory
                        .getRejectedExecutionHandler(buildLessStartInfo.rejectedExecutionType)
                        .rejectedExecution(buildLessStartInfo)
                }

                // 再次check idlePoolSize，有变更则retry
                if (idlePoolSize != redisUtils.getIdlePoolSize()) continue@retry

                // 已经进入需求池，资源池减1
                redisUtils.increIdlePool(-1)
                return false
            }
        } finally {
            lock.unlock()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerPoolExecutor::class.java)

        /**
         * 容器创建锁
         */
        private val mainLock = ReentrantLock()

        /**
         * 空闲资源锁
         */
        private val idlePoolLock = ReentrantLock()
    }
}
