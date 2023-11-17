package com.tencent.devops.environment.service.job

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.environment.dao.NodeDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TencentNodeScheduledService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val tencentQueryFromCmdbService: TencentQueryFromCmdbService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TencentNodeScheduledService::class.java)
        private const val CHECK_NODE_IN_CMDB_TIMEROUT_LOCK_KEY = "check_node_in_cmdb_timeout_lock"
        private const val EXPTIRATION_TIME_OF_THE_LOCK = 60L
    }

    /**
     * 后台定时轮询机器状态，看机器在不在公司cmdb中（cloud_area_id 字段：-1 - 不在，0 - 在）
     * 只轮询T_NODE表中 NODE_TYPE=="CMDB"的记录，用ip 调用get_query_info接口，看能否得到对应记录：能-不操作，不能-对应记录的cloud_area_id置为-1
     * cron：每天上午9点执行。
     */
    @Scheduled(cron = "0 0 9 * * ?")
    fun scheduledCheckNodeInCmdb() {
        val redisLock = RedisLock(
            getRedisStringSerializerOperation(), CHECK_NODE_IN_CMDB_TIMEROUT_LOCK_KEY, EXPTIRATION_TIME_OF_THE_LOCK
        )
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                if (logger.isDebugEnabled)
                    logger.debug("---[scheduledCheckNodeInCmdb]Check whether the node is in cmdb.---")
                checkNodeInCmdb()
            } else {
                if (logger.isDebugEnabled)
                    logger.debug("---[scheduledCheckNodeInCmdb]Task is running and doesn't need to be started.---")
            }
        } catch (e: Throwable) {
            logger.error("[scheduledCheckNodeInCmdb]exception: ", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun getRedisStringSerializerOperation(): RedisOperation {
        return SpringContextUtil.getBean(RedisOperation::class.java, "redisStringHashOperation")
    }

    private fun checkNodeInCmdb() {
        val nodeRecords = nodeDao.getCmdbNodes(dslContext) // T_NODE表中 所有NODE_TYPE=="CMDB"的记录
        val nodeIpList = nodeRecords.map { it.nodeIp } // 要判断在不在cmdb中的 所有ip
        val ipToCmdbInfoMap = tencentQueryFromCmdbService.queryCmdbInfoFromIp(nodeIpList) // 从cmdb中查到的 所有ip - 记录 映射
        val invalidIpList = nodeIpList.filterNot {
            ipToCmdbInfoMap?.containsKey(it) ?: false
        } // 不在cmdb中，对应节点的 NODE_STATUS字段 要改成 NOT_IN_CMDB
        nodeDao.updateNodeNotInCmdb(dslContext, invalidIpList)
        if (logger.isDebugEnabled) logger.debug("---[checkNodeInCmdb]End Check whether the node is in the cmdb.---")
    }
}