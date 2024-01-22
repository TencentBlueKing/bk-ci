package com.tencent.devops.environment.service.job

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service("NodeScheduledService")
class NodeScheduledService @Autowired constructor(
    private val iStockDataUpdateService: IStockDataUpdateService,
    private val stockDataUpdateService: StockDataUpdateService
) {
    companion object {
        private const val SCHEDULED_CHECK_NODES_TIMEOUT_LOCK_KEY = "scheduled_check_nodes_timeout_lock"
    }

    @Scheduled(cron = "0 0 10 * * ?")
    fun scheduledCheckDeployNodes() {
        stockDataUpdateService.taskWithRedisLock(SCHEDULED_CHECK_NODES_TIMEOUT_LOCK_KEY, ::sCheckDeployNodes)
    }

    private fun sCheckDeployNodes() {
        iStockDataUpdateService.checkDeployNodes()
    }
}