package com.tencent.devops.dispatch.schedule

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.common.Constants
import com.tencent.devops.dispatch.dao.PipelineDockerIPInfoDao
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerIpInfoRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
        private const val failJobLockKey = "dispatch_docker_cron_volume_fresh_fail_job"
    }

    /**
     * 每隔2分钟check母机状态
     */
    // @Scheduled(cron = "0 0/2 * * * ?")
    fun checkVMStatus() {
        val redisLock = RedisLock(redisOperation, failJobLockKey, 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Start check VM status gray: ${gray.isGray()}")
                val dockerIpList = pipelineDockerIpInfoDao.getDockerIpList(dslContext, true, gray.isGray())
                dockerIpList.stream().forEach {
                    singleDockerIpCheck(it)
                }
            }
        } catch (e: Throwable) {
            logger.error("Check docker VM status failed.", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun singleDockerIpCheck(dockerIpInfoRecord: TDispatchPipelineDockerIpInfoRecord) {
        try {
            // 重置限流设置
            redisOperation.set("${Constants.DOCKER_IP_COUNT_KEY_PREFIX}${dockerIpInfoRecord.dockerIp}", "1", 120)

            val nowTimestamp = System.currentTimeMillis() / 1000
            val lastUpdateTimestamp = dockerIpInfoRecord.gmtModified.timestamp()
            if ((nowTimestamp - lastUpdateTimestamp) >= 60) {
                // 刷新时间间隔超时，更新容器状态为不可用
                logger.info("Docker ${dockerIpInfoRecord.dockerIp} check timeout, update enable false.")
                pipelineDockerIpInfoDao.updateDockerIpStatus(dslContext, dockerIpInfoRecord.dockerIp, false)
            }
        } catch (e: Exception) {
            logger.error("singleDockerIpCheck updateDockerIpStatus fail.", e)
        }
    }
}