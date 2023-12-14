package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.job.DisplayNameInfo
import com.tencent.devops.environment.pojo.job.ccres.CCInfo
import com.tencent.devops.environment.service.job.QueryFromCCService.Companion.FIELD_BK_HOST_ID
import com.tencent.devops.environment.service.job.QueryFromCCService.Companion.FIELD_BK_HOST_INNERIP
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class NodeScheduledService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val queryFromCCService: QueryFromCCService,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NodeScheduledService::class.java)
        private const val SCHEDULED_CHECK_NODE_IN_CC_TIMEOUT_LOCK_KEY = "scheduled_check_node_in_cc_timeout_lock"
        private const val SCHEDULED_WRITE_DISPLAY_NAME_TIMEOUT_LOCK_KEY = "scheduled_write_display_name_timeout_lock"
        private const val EXPIRATION_TIME_OF_THE_LOCK = 200L
        private const val DEFAULT_PAGE_SIZE = 100
    }

    /**
     * 定时任务：执行一次就行。
     * 分组执行，每次遍历100条记录。
     * display_name为空的：拼接节点类型、node hash值、nodeId这三个字段，写入display_name。
     */
    @Scheduled(cron = "0 16 18 * * 1-5")
    fun scheduledWriteDisplayName() {
        taskWithRedisLock(SCHEDULED_WRITE_DISPLAY_NAME_TIMEOUT_LOCK_KEY, ::writeDisplayName)
    }

    /**
     * 后台定时轮询机器状态，看机器是否在CC中（T_NODE表中host_id字段：为空 - 不在，不为空 - 在）
     * 遍历T_NODE表中host_id不为空的记录，用host_id 调用find_host_biz_relations接口，看能否得到对应记录：能-不操作，不能-对应记录host_id置为null
     * cron：每天上午10点执行。
     */
    @Scheduled(cron = "0 29 18 * * 1-5")
    fun scheduledCheckNodeInCC() {
        taskWithRedisLock(SCHEDULED_CHECK_NODE_IN_CC_TIMEOUT_LOCK_KEY, ::checkNodeInCC)
    }

    private fun writeDisplayName() {
        val countDisplayNameEmptyNodes = nodeDao.countDisplayNameEmptyNodes(dslContext)
        if (logger.isDebugEnabled)
            logger.debug("[writeDisplayName]countDisplayNameEmptyNodes:$countDisplayNameEmptyNodes.")
        if (0 < countDisplayNameEmptyNodes) {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countDisplayNameEmptyNodes.toLong())
            for (page in 1..totalPages) {
                val displayNameNullNodeRecords =
                    nodeDao.getNodesWhoseDisplayNameIsEmpty(dslContext, page - 1, DEFAULT_PAGE_SIZE)
                val nodeDisplayNameInfoList = displayNameNullNodeRecords.map {
                    DisplayNameInfo(
                        nodeId = it.value1(),
                        nodeType = it.value2(),
                        nodeHashId = it.value3(),
                        displayName = it.value2() + "-" + it.value3() + "-" + it.value1().toString()
                    )
                }
                nodeDao.updateDisplayNameByNodeId(dslContext, nodeDisplayNameInfoList)
            }
        } else {
            if (logger.isDebugEnabled) logger.debug("[writeDisplayName] There is no node with empty DisplayName.")
        }
    }

    private fun checkNodeInCC() {
        val countHostIdNotNullRecord = nodeDao.countNodesWhoseHostIdNotNull(dslContext)
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCC]countHostIdNotNullRecord:$countHostIdNotNullRecord.")
        if (0 < countHostIdNotNullRecord) {
            val totalPagesHostIdNotNull = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countHostIdNotNullRecord.toLong())
            for (pageHostIdNotNull in 1..totalPagesHostIdNotNull) {
                val nodeRecords = nodeDao.getNodesWhoseHostIdNotNullLimit(
                    dslContext, pageHostIdNotNull - 1, DEFAULT_PAGE_SIZE
                ) // T_NODE表中host_id不为空的记录
                val hostIdList = nodeRecords.map { it.value1() } // 要判断在不在cc中的 所有host_id
                val nodeCCList =
                    if (hostIdList.isNotEmpty()) queryFromCCService.queryCCFindHostBizRelations(hostIdList).data
                    else emptyList() // 在cc中的 host_id对应cc记录
                val hostIdToNodeCCMap = nodeCCList?.associateBy { it.bkHostId.toLong() } // 在cc中的 host_id-cc记录 映射
                // 不在cc中了，置空 hostid 和 云区域id，且 NODE_STATUS字段 要改成 NOT_IN_CC
                val invalidHostIdList = hostIdList.filterNot { hostIdToNodeCCMap?.containsKey(it) ?: false }
                nodeDao.updateNodeNotInCC(dslContext, invalidHostIdList)
            }
        }
        // T_NODE表中 NODE_STATUS字段 为NOT_IN_CC的记录，再去查在不在cc中：不在CC中 - 不处理；在CC中 - 将host_id写回T_NODE表中，NODE_STATUS字段 改成 NORMAL
        val countNodesNotInCC = nodeDao.countNodesNotInCC(dslContext)
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCC]countNodesNotInCC:$countNodesNotInCC.")
        if (0 < countNodesNotInCC) {
            val totalPagesNodesNotInCC = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countNodesNotInCC.toLong())
            for (pageNodesNotInCC in 1..totalPagesNodesNotInCC) {
                val nodeIpsNotInCC = nodeDao.getNodeIpsNotInCC(
                    dslContext, pageNodesNotInCC - 1, DEFAULT_PAGE_SIZE
                )
                val notInCCIpList = nodeIpsNotInCC.map { it.value1() }
                val inCCInfoList = queryFromCCService.queryCCListHostWithoutBizByInRules(
                    listOf(FIELD_BK_HOST_ID, FIELD_BK_HOST_INNERIP), notInCCIpList, FIELD_BK_HOST_INNERIP
                ).data?.info
                if (logger.isDebugEnabled) logger.debug("[checkNodeInCC]inCCInfoList:$inCCInfoList.")
                batchUpdateByCCInfo(inCCInfoList)
            }
        }
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCC]End Check whether the node is in the cc.")
    }

    private fun batchUpdateByCCInfo(inCCInfoList: List<CCInfo>?) {
        if (!inCCInfoList.isNullOrEmpty()) {
//                    nodeDao.updateNodeHostIdByIp(dslContext, inCCInfoList) // 在CC中 - 将host_id写回T_NODE表中
            val inCCIpToCCInfoMap = inCCInfoList.associateBy { it.bkHostInnerip }
            val inCCIpList = inCCInfoList.mapNotNull { it.bkHostInnerip }.distinct()
            val inCCRecord = nodeDao.getInCmdbNodesByIp(dslContext, inCCIpList)
            if (logger.isDebugEnabled) logger.debug("[batchUpdateByCCInfo]inCCRecord:$inCCRecord.")
            inCCRecord.forEach {
                it.hostId = inCCIpToCCInfoMap[it.nodeIp]?.bkHostId
                it.cloudAreaId = inCCIpToCCInfoMap[it.nodeIp]?.bkCloudId?.toLong()
            }
            if (logger.isDebugEnabled) logger.debug("[batchUpdateByCCInfo]inCCRecord2:$inCCRecord.")
            nodeDao.batchUpdateNodeHostIdByIp(dslContext, inCCRecord)
            nodeDao.updateNodeInCCByIp(dslContext, inCCIpList) // 在CC中 - NODE_STATUS字段 改成 NORMAL
        }
    }

    private fun taskWithRedisLock(lockKey: String, operation: () -> Unit) {
        val redisLock = RedisLock(redisOperation, lockKey, EXPIRATION_TIME_OF_THE_LOCK)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                if (logger.isDebugEnabled) logger.debug("[taskWithRedisLock]Locked.")
                operation()
            } else {
                if (logger.isDebugEnabled) logger.debug("[taskWithRedisLock]Lock failed.")
            }
        } catch (e: Throwable) {
            logger.error("[taskWithRedisLock]exception: ", e)
        } finally {
            redisLock.unlock()
        }
    }
}