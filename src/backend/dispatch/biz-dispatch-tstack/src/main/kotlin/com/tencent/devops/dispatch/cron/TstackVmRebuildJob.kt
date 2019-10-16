package com.tencent.devops.dispatch.cron

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.dispatch.dao.TstackVmDao
import com.tencent.devops.dispatch.pojo.enums.TstackVmStatus
import com.tencent.devops.dispatch.service.vm.TstackClient
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackVmRecord
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * deng
 * 17/01/2018
 */
@Component
class TstackVmRebuildJob @Autowired constructor(
        private val dslContext: DSLContext,
        private val tstackVmDao: TstackVmDao,
        private val tstackClient: TstackClient,
        private val tstackSystemConfig: TstackSystemConfig,
        private val redisOperation: RedisOperation,
        private val client: Client,
        private val profile: Profile
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TstackVmRebuildJob::class.java)
        private val ALERT_LOCK_KEY = "dispatch_tstack_rebuild_alert_lock"
        private val REBUILD_LOCK_KEY = "dispatch_cron_rebuild_tstack_vm_lock"
    }

    @Value("\${dispatch.tstack.systemAlertOn:#{null}}")
    private val alertOn: Boolean? = null

    @Value("\${dispatch.tstack.systemAlertReceiver:#{null}}")
    private val alertReceiver: String? = null

    @Scheduled(initialDelay = 1000 * 60, fixedDelay = 1000 * 20 /* 20秒 */)
    fun rebuildTstackVms() {
        logger.info("Start to rebuild Tstack Vms")
        val redisLock = RedisLock(redisOperation, REBUILD_LOCK_KEY, 1800L)
        try {
            var lockSuccess = redisLock.tryLock()
            if (!lockSuccess) {
                logger.info("get lock failed, skip")
                return
            }

            cleanInvalidBuilds()
            recycleVms()
            buildNewVms()
        } catch (t: Throwable) {
            logger.error("error occur while rebuilding statck vm", t)
            sendAlert()
        } finally {
            redisLock.unlock()
        }
        logger.info("Rebuild Tstack Vms End")
    }

    private fun cleanInvalidBuilds() {
    }

    private fun buildNewVms() {
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val availableVms = tstackVmDao.listVmByStatus(dslContext, TstackVmStatus.AVAILABLE.name)
        val buildingVms = tstackVmDao.listVmByStatus(dslContext, TstackVmStatus.BUILDING.name)
        val needCreateCount = (systemConfig.maxVmCount - availableVms.size - buildingVms.size)
        if (needCreateCount <= 0) {
            logger.info("no need to create new tstack vm")
            return
        }

        logger.info("try to create 1 tstack vms")
        val tstackVm = tstackClient.syncCreateBuildVm(systemConfig.osSnapshotWin7, systemConfig.flavorId)
        tstackVmDao.insertVm(dslContext,
                tstackVm.id,
                tstackVm.floatingIp,
                tstackVm.vmName,
                tstackVm.vmOs,
                tstackVm.vmOsVersion,
                tstackVm.vmCpu,
                tstackVm.vmMemory,
                TstackVmStatus.AVAILABLE.name
        )
        logger.info("1 tstack vm created")
    }

    private fun recycleVms() {
        logger.info("recycle tstack vms start")
        val recycleVms = tstackVmDao.listVmByStatus(dslContext, TstackVmStatus.RECYCLABLE.name)
        if (recycleVms.isNotEmpty) {
            logger.info("try to destroy ${recycleVms.size} tstack vms")
            val token = tstackClient.getToken()
            recycleVms.forEach { tstackVm ->
                try {
                    destroyVms(token, tstackVm)
                    tstackVmDao.updateStatus(dslContext, tstackVm.id, TstackVmStatus.DESTROYED.name)
                    logger.info("tstack vm ${tstackVm.id} destroyed")
                } catch (e: Exception) {
                    logger.error("destroy tstack vm ${tstackVm.id} failed")
                }
            }
        }
        logger.info("recycle tstack vms end")
    }

    private fun sendAlert() {
        if (!alertOn!! || alertReceiver.isNullOrBlank()) {
            logger.info("send alert, no receiver, skip")
            return
        }

        val lockValue = redisOperation.get(ALERT_LOCK_KEY)
        if (lockValue != null) {
            logger.info("send alert, key is locked, skip")
            return
        }

        val message = RtxNotifyMessage()
        message.addAllReceivers(alertReceiver!!.split(",", ";").toSet())
        message.title = "【蓝盾devops系统告警】"
        val envStr = when {
            profile.isProd() -> "生产环境"
            profile.isTest() -> "测试环境"
            else -> "开发环境"
        }
        message.body = "[$envStr]Dispath模块，TStack虚拟机重建发生异常，请及时检查！"

        client.get(ServiceNotifyResource::class).sendRtxNotify(message)
        logger.info("alert sended: ${message.body}")

        redisOperation.set(ALERT_LOCK_KEY, "Yes", 900)
    }

    private fun destroyVms(token: String, tstackVm: TDispatchTstackVmRecord) {
        tstackClient.deleteVm(token, tstackVm.tstackVmId)
    }
}