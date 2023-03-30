package com.tencent.devops.dispatch.codecc.schedule

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.codecc.common.Constants
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerIPInfoDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class VmStatusScheduler @Autowired constructor(
    private val dslContext: DSLContext,
    private val gray: Gray,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(VmStatusScheduler::class.java)
        private const val failJobLockKey = "dispatch_codecc_docker_cron_volume_fresh_fail_job"
        private const val resetDockerIpLimitKey = "dispatch_codecc_docker_cron_reset_docker_ip_limit"
    }

    /**
     * 每隔1分钟check母机状态
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    fun checkVMStatus() {
        logger.info("Start check VM status gray: ${gray.isGray()}")
        val redisLock = RedisLock(redisOperation, failJobLockKey, 120L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Start check VM status get lock.")
                val dockerIpList = pipelineDockerIpInfoDao.getExpiredIpStatus(dslContext)
                dockerIpList.parallelStream().forEach {
                    try {
                        // 刷新时间间隔超时，更新容器状态为不可用
                        logger.info("Docker ${it.dockerIp} check timeout, update enable false.")
                        pipelineDockerIpInfoDao.updateDockerIpStatus(dslContext, it.dockerIp, false)
                    } catch (e: Exception) {
                        logger.error("singleDockerIpCheck updateDockerIpStatus fail.", e)
                    }
                }
            }
        } catch (e: Throwable) {
            logger.error("Check docker VM status failed.", e)
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 每隔2分钟重置限流
     */
    @Scheduled(cron = "0 0/2 * * * ?")
    fun resetDockerIpLimit() {
        val time = System.currentTimeMillis()
        logger.info("Start reset dockerIp limit: $time")
        val redisLock = RedisLock(redisOperation, resetDockerIpLimitKey, 150L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Start reset dockerIp limit get lock: $time")
                val dockerIpList = pipelineDockerIpInfoDao.getDockerIpList(dslContext, true, gray.isGray())
                dockerIpList.forEach {
                    // 重置限流设置
                    redisOperation.set("${Constants.DOCKER_IP_COUNT_KEY_PREFIX}${it.dockerIp}", "1", 150)
                }
            }
            logger.info("End reset dockerIp limit: $time")
        } catch (e: Throwable) {
            logger.error("Start reset dockerIp limit failed.", e)
        } finally {
            logger.info("End reset dockerIp limit get lock: $time")
            redisLock.unlock()
        }
    }
}
