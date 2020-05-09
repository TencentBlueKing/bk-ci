package com.tencent.devops.dispatch.schedule

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.common.Constants
import com.tencent.devops.dispatch.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.utils.DockerHostUtils
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerIpInfoRecord
import okhttp3.Request
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class VmStatusScheduler @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val dockerHostUtils: DockerHostUtils,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(VmStatusScheduler::class.java)
        private const val jobLockKey = "dispatch_docker_cron_volume_fresh_job"
        private const val failJobLockKey = "dispatch_docker_cron_volume_fresh_fail_job"
    }

    /**
     * 定时刷新check母机状态
     */
    @Scheduled(cron = "0 0/3 * * * ?")
    fun run() {
        val redisLock = RedisLock(redisOperation, jobLockKey, 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Docker VM status fresh start")
                // executeTask()
            }
        } catch (e: Throwable) {
            logger.error(" Docker status fresh exception", e)
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 每隔2分钟check母机状态
     */
    @Scheduled(cron = "0 0/2 * * * ?")
    fun checkVMStatus() {
        val redisLock = RedisLock(redisOperation, failJobLockKey, 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                var grayEnv = false
                val gray = System.getProperty("gray.project", "none")
                if (gray == "grayproject") {
                    grayEnv = true
                }
                val dockerIpList = pipelineDockerIpInfoDao.getDockerIpList(dslContext, true, grayEnv)
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
            logger.info("nowTimestamp: $nowTimestamp, lastUpdateTimestamp: $lastUpdateTimestamp")
            if ((nowTimestamp - lastUpdateTimestamp) >= 60) {
                // 刷新时间间隔超时，更新容器状态为不可用
                logger.info("Docker ${dockerIpInfoRecord.dockerIp} check timeout, update enable false.")
                pipelineDockerIpInfoDao.updateDockerIpStatus(dslContext, dockerIpInfoRecord.dockerIp, false)
            }
        } catch (e: Exception) {
            logger.error("singleDockerIpCheck updateDockerIpStatus fail.", e)
        }
    }

    private fun executeTask() {
        var grayEnv = false
        val gray = System.getProperty("gray.project", "none")
        if (gray == "grayproject") {
            grayEnv = true
        }

        val dockerIpList = pipelineDockerIpInfoDao.getDockerIpList(dslContext, true, grayEnv)
        logger.info("getAvailableDockerIp gray: $gray, dockerIpList size: ${dockerIpList.size}")
        dockerIpList.parallelStream().forEach {
            singleTask(it)
        }
    }

    private fun singleTask(it: TDispatchPipelineDockerIpInfoRecord) {
        val itDockerIp = it.dockerIp as String
        val enable = it.enable as Boolean
        val proxyUrl = dockerHostUtils.getIdc2DevnetProxyUrl("/api/docker/host/load", itDockerIp, it.dockerHostPort)
        val request = Request.Builder().url(proxyUrl)
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        // logger.info("Docker VM status fresh url: $proxyUrl")
        try {
            OkhttpUtils.doHttp(request).use { resp ->
                val responseBody = resp.body()!!.string()
                // logger.info("Docker VM $itDockerIp status fresh responseBody: $responseBody")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                if (response["status"] == 0) {
                    val dockerHostLoad: Map<String, Any> = response["data"] as LinkedHashMap<String, Any>
                    val usedNum = dockerHostLoad["usedContainerNum"] as Int
                    val averageCpuLoad = dockerHostLoad["averageCpuLoad"] as Int
                    val averageMemLoad = dockerHostLoad["averageMemLoad"] as Int
                    val averageDiskLoad = dockerHostLoad["averageDiskLoad"] as Int
                    val averageDiskIOLoad = dockerHostLoad["averageDiskIOLoad"] as Int
                    pipelineDockerIpInfoDao.updateDockerIpLoad(
                        dslContext = dslContext,
                        dockerIp = itDockerIp,
                        dockerHostPort = it.dockerHostPort,
                        used = usedNum,
                        cpuLoad = averageCpuLoad,
                        memLoad = averageMemLoad,
                        diskLoad = averageDiskLoad,
                        diskIOLoad = averageDiskIOLoad,
                        enable = true
                    )

                    redisOperation.set("${Constants.DOCKER_IP_COUNT_KEY_PREFIX}$itDockerIp", "1", 180)
                } else {
                    // 如果之前可用，更新容器状态
                    if (enable) {
                        pipelineDockerIpInfoDao.updateDockerIpStatus(dslContext, it.dockerIp, false)
                    }

                    val msg = response["message"] as String
                    logger.error("Get Docker VM container failed, msg: $msg")
                }
            }
        } catch (e: Exception) {
            // 如果之前可用，更新容器状态
            if (enable) {
                pipelineDockerIpInfoDao.updateDockerIpStatus(dslContext, it.dockerIp, false)
            }
            logger.error("Get Docker VM: $itDockerIp container failed.", e)
        }
    }
}