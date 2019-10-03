package com.tencent.devops.dispatch.cron

import com.tencent.devops.dispatch.dao.TstackSystemDao
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackSystemRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * deng
 * 17/01/2018
 */
@Component
class TstackSystemConfig @Autowired constructor(
    private val dslContext: DSLContext,
    private val tstackSystemDao: TstackSystemDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TstackSystemConfig::class.java)
    }

    private var tstackSystemConfig: TDispatchTstackSystemRecord? = null

    private var tstackFloatingIps: Set<String>? = null

    /**
     * 动态更新Tsatck构建系统配置
     */
    @Scheduled(initialDelay = 1000 * 60, fixedDelay = 1000 * 60 /* 1分钟 */)
    fun reloadTstackSystemConfig() {
        logger.info("Start reloading TStack system config")
        tstackSystemConfig = tstackSystemDao.getSystemConfig(dslContext)
        logger.info("Reload TStack system config end")
    }

    /**
     * 动态更新Tstack可用IP信息
     */
    @Scheduled(initialDelay = 1000 * 60, fixedDelay = 1000 * 60 /* 1分钟 */)
    fun reloadTstackFloatingIps() {
        logger.info("Start reloading TStack floating IP(s)")
        tstackFloatingIps = tstackSystemDao.getFloatingIpList(dslContext).map { it.floatingIp }.toSet()
        logger.info("Reload TStack floating IP(s) end")
    }

    fun getTstackSystemConfig(): TDispatchTstackSystemRecord {
        if (tstackSystemConfig == null) {
            tstackSystemConfig = tstackSystemDao.getSystemConfig(dslContext)
        }
        return tstackSystemConfig!!
    }

    fun getTstackFloatingIps(): Set<String> {
        if (tstackFloatingIps == null) {
            tstackFloatingIps = tstackSystemDao.getFloatingIpList(dslContext).map { it.floatingIp }.toSet()
        }
        return tstackFloatingIps!!
    }
}