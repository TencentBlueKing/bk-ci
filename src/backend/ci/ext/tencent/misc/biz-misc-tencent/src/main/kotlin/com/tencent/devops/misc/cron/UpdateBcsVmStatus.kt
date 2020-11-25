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

package com.tencent.devops.misc.cron

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.environment.agent.client.BcsClient
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.misc.dao.EnvironmentNodeDao
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import org.jooq.DSLContext
import org.jooq.tools.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UpdateBcsVmStatus @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: EnvironmentNodeDao,
    private val bcsClient: BcsClient,
    private val client: Client,
    private val redisOperation: RedisOperation
) {

    @Value("\${env.alert.receiver:#{null}}")
    private val alertReceiver: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(UpdateBcsVmStatus::class.java)
        private val LOCK_KEY = "env_cron_updateBcsVmStatus"
        private val LOCK_VALUE = "env_cron_updateBcsVmStatus"
        private val BCSVM_ALERT_KEY = "env_cron_bcsvm_alert_on"
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    fun runUpdateBcsVmStatus() {
        logger.info("runUpdateBcsVmStatus")
        val lockValue = redisOperation.get(LOCK_KEY)
        if (lockValue != null) {
            logger.info("get lock failed, skip")
            return
        } else {
            redisOperation.set(
                LOCK_KEY,
                LOCK_VALUE, 60)
        }

        try {
            updateBcsVmStatus()
        } catch (t: Throwable) {
            logger.warn("update server node status failed", t)
        }
    }

    private fun updateBcsVmStatus() {
        val allBcsVmNodes = nodeDao.listAllNodesByType(dslContext, NodeType.BCSVM)

        if (allBcsVmNodes.isEmpty()) {
            return
        }

        allBcsVmNodes.forEach {
            val statusAndOS = bcsClient.inspectVmList(it.nodeClusterId, it.nodeNamespace, it.nodeName)
            if (statusAndOS != null) {
                it.nodeStatus = statusAndOS.nodeStatus
                it.osName = statusAndOS.osName
            }
        }

        checkAndAlert(allBcsVmNodes)

        nodeDao.batchUpdateNode(dslContext, allBcsVmNodes)
    }

    private fun checkAndAlert(allBcsVmNodes: List<TNodeRecord>) {
        if (System.getProperty("spring.profiles.active") != "prod") {
            return
        }

        if (StringUtils.isEmpty(alertReceiver)) {
            return
        }

        val lockValue = redisOperation.get(BCSVM_ALERT_KEY)
        if (lockValue != null) {
            return
        }

        val abnormalBcsVms = allBcsVmNodes.filter {
            val nodeStatus = NodeStatus.parseByName(it.nodeStatus)
            when (nodeStatus) {
                NodeStatus.ABNORMAL -> true
                else -> false
            }
        }

        if (abnormalBcsVms.isNotEmpty()) {
            val smsMsg = SmsNotifyMessage().apply {
                body = "【环境管理】系统存在异常的BCS虚拟机节点，请及时检查！"
            }
            smsMsg.addAllReceivers(alertReceiver!!.split(",", ";").toSet())
            logger.info("send env bcsvm alert sms notify")
            client.get(ServiceNotifyResource::class).sendSmsNotify(smsMsg)
            redisOperation.set(BCSVM_ALERT_KEY, "Yes", 900)
        }
    }
}