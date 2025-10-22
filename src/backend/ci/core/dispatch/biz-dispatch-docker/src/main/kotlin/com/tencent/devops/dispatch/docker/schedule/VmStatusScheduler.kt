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

package com.tencent.devops.dispatch.docker.schedule

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.docker.common.Constants
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerIpInfoRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class VmStatusScheduler @Autowired constructor(
    private val dslContext: DSLContext,
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
    @Scheduled(cron = "0 0/2 * * * ?")
    fun checkVMStatus() {
        val redisLock = RedisLock(redisOperation, failJobLockKey, 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Start check VM status.")
                val dockerIpList = pipelineDockerIpInfoDao.getDockerIpList(dslContext, true)
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
