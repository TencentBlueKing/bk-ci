package com.tencent.devops.misc.cron

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.misc.client.BcsClient
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.misc.dao.NodeDao
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import com.tencent.devops.notify.api.ServiceNotifyResource
import com.tencent.devops.notify.model.SmsNotifyMessage
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
    private val nodeDao: NodeDao,
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