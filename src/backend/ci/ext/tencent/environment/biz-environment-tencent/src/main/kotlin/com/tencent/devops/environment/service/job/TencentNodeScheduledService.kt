package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.service.CmdbNodeService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TencentNodeScheduledService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val tencentQueryFromCmdbService: TencentQueryFromCmdbService,
    private val cmdbNodeService: CmdbNodeService,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TencentNodeScheduledService::class.java)
        private const val CHECK_NODE_IN_CMDB_TIMEROUT_LOCK_KEY = "check_node_in_cmdb_timeout_lock"
        private const val EXPTIRATION_TIME_OF_THE_LOCK = 60L
        private const val DEFAULT_PAGE_SIZE = 100
    }

    /**
     * 后台定时轮询机器状态，看机器在不在公司cmdb中（cloud_area_id 字段：-1 - 不在，0 - 在）
     * 只轮询T_NODE表中 NODE_TYPE=="CMDB"的记录，用ip 调用get_query_info接口，看能否得到对应记录：能-不操作，不能-对应记录的cloud_area_id置为-1
     * cron：每天上午9点执行。
     */
    @Scheduled(cron = "0 0 9 * * ?")
    fun scheduledCheckNodeInCmdb() {
        taskWithRedisLockTencent(CHECK_NODE_IN_CMDB_TIMEROUT_LOCK_KEY, ::checkNodeInCmdb)
    }

    private fun checkNodeInCmdb() {
        val countNodeInCmdb = nodeDao.countCmdbNodes(dslContext)
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCmdb]countNodeInCmdb:$countNodeInCmdb")
        if (0 < countNodeInCmdb) {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countNodeInCmdb.toLong())
            for (page in 1..totalPages) {
                val nodeRecords = nodeDao.getCmdbNodesLimit(
                    dslContext, page, DEFAULT_PAGE_SIZE
                ) // T_NODE表中 所有NODE_TYPE=="CMDB"的记录
                val nodeIpList = nodeRecords.map { it.value3() }.toSet() // 要判断在不在cmdb中的 所有ip
                val ipToCmdbInfoMap =
                    tencentQueryFromCmdbService.queryCmdbInfoFromIp(nodeIpList) // 从cmdb中查到的 所有ip - 记录 映射

                // 不在cmdb中，对应节点的 NODE_STATUS字段 要改成 NOT_IN_CMDB
                val invalidIpList = nodeIpList.filterNot { ipToCmdbInfoMap?.containsKey(it) ?: false }
                nodeDao.updateNodeNotInCmdb(dslContext, invalidIpList)

                // 在cmdb中，但对应节点的 NODE_STATUS字段==NOT_IN_CMDB，再查询一次CC: 在CC-改为NORMAL，不在CC-改为NOT_IN_CC
                val inCmdbIpList = nodeIpList.filter { ipToCmdbInfoMap?.containsKey(it) ?: false }
                val inCmdbIpRecords = nodeDao.getNotInCmdbNodes(dslContext, inCmdbIpList)
                if (inCmdbIpRecords.isNotEmpty()) {
                    val inCmdbSvrIdList = inCmdbIpRecords.map { it.value1() } // 需要再查询一次CC的ip
                        .map { ipToCmdbInfoMap?.get(it)?.serverId?.toLong() }.filterNotNull() // 需要再查询一次CC的svrId

                    val (svrIdQueryCCRes, _, _) =
                        cmdbNodeService.checkNodeInCCBySvrId(inCmdbSvrIdList) // 用svrId，得到：其中所有在CC中的节点记录，在/不在CC中的svrId列表
                    val ccData = svrIdQueryCCRes.data?.info
                    val inCCIpList = ccData?.mapNotNull { it.bkHostInnerip }
                    val notInCCIpList = inCmdbIpList.filterNot { inCCIpList?.contains(it) ?: false }
                    if (!inCCIpList.isNullOrEmpty())
                        nodeDao.updateNodeInCCByIp(dslContext, inCCIpList) // 在CC-改为NORMAL
                    if (notInCCIpList.isNotEmpty())
                        nodeDao.updateNodeNotInCCByIp(dslContext, notInCCIpList) // 不在CC-改为NOT_IN_CC
                }
            }
        }
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCmdb]End Check whether the node is in the cmdb.")
    }

    private fun taskWithRedisLockTencent(lockKey: String, operation: () -> Unit) {
        val redisLock = RedisLock(redisOperation, lockKey, EXPTIRATION_TIME_OF_THE_LOCK)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                if (logger.isDebugEnabled) logger.debug("[taskWithRedisLockTencent]Locked.")
                operation()
            } else {
                if (logger.isDebugEnabled) logger.debug("[taskWithRedisLockTencent]Lock failed.")
            }
        } catch (e: Throwable) {
            logger.error("[taskWithRedisLockTencent]exception: ", e)
        } finally {
            redisLock.unlock()
        }
    }
}