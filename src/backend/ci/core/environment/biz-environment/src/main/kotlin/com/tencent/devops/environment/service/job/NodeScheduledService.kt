package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.job.DisplayNameInfo
import com.tencent.devops.environment.pojo.job.HostIdAndCloudAreaIdInfo
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
    private val queryFromCCService: QueryFromCCService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NodeScheduledService::class.java)
        private const val SCHEDULED_CLEAN_HOST_ID_TIMEROUT_LOCK_KEY = "scheduled_clean_invalid_host_id_timeout_lock"
        private const val EXPTIRATION_TIME_OF_THE_LOCK = 60L
        private const val DEFAULT_PAGE_SIZE = 100
        private const val DEFAULT_CLOUD_AREA_ID = 0L
    }

    /**
     * 后台定时轮询机器状态，看机器是否在CC中（T_NODE表中host_id字段：为空 - 不在，不为空 - 在）
     * 遍历T_NODE表中host_id不为空的记录，用host_id 调用find_host_biz_relations接口，看能否得到对应记录：能-不操作，不能-对应记录host_id置为null
     * cron：每天上午10点执行。
     */
    @Scheduled(cron = "0 0 10 * * 1-5")
    fun scheduledCleanInvalidHostId() {
        val redisLock = RedisLock(
            getRedisStringSerializerOperation(), SCHEDULED_CLEAN_HOST_ID_TIMEROUT_LOCK_KEY, EXPTIRATION_TIME_OF_THE_LOCK
        )
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                if (logger.isDebugEnabled)
                    logger.debug("---[scheduledCleanInvalidHostId]Check whether the node is in cc.---")
                checkNodeInCC()
            } else {
                if (logger.isDebugEnabled)
                    logger.debug("---[scheduledCleanInvalidHostId]Task is running and doesn't need to be started.---")
            }
        } catch (e: Throwable) {
            logger.error("[scheduledCleanInvalidHostId]exception: ", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun getRedisStringSerializerOperation(): RedisOperation {
        return SpringContextUtil.getBean(RedisOperation::class.java, "redisStringHashOperation")
    }

    private fun checkNodeInCC() {
        val nodeRecords = nodeDao.getNodesWhoseHostIdNotNull(dslContext) // T_NODE表中host_id不为空的记录
        val hostIdList = nodeRecords.map { it.value1() } // 要判断在不在cc中的 所有host_id

        val nodeCCList =
            if (hostIdList.isNotEmpty()) queryFromCCService.queryCCFindHostBizRelations(hostIdList).data
            else emptyList() // 在cc中的 host_id对应cc记录
        val hostIdToNodeCCMap = nodeCCList?.associateBy { it.bkHostId.toLong() } // 在cc中的 host_id-cc记录 映射
        // 不在cc中了，要置空的hostid，且 NODE_STATUS字段 要改成 NOT_IN_CC
        val invalidHostIdList = hostIdList.filterNot { hostIdToNodeCCMap?.containsKey(it) ?: false }
        nodeDao.updateNodeNotInCC(dslContext, invalidHostIdList)

        // T_NODE表中 NODE_STATUS字段 为NOT_IN_CC的记录，再去查在不在cc中：不在CC中 - 不处理；在CC中 - 将host_id写回T_NODE表中，NODE_STATUS字段 改成 NORMAL
        val nodeRecordsNotInCC = nodeDao.getNodesNotInCC(dslContext)
        val notInCCIpList = nodeRecordsNotInCC.map { it.value1() }
        val inCCInfoList = queryFromCCService.queryCCListHostWithoutBizByInRules(
            listOf(FIELD_BK_HOST_ID, FIELD_BK_HOST_INNERIP), notInCCIpList, FIELD_BK_HOST_INNERIP
        ).data?.info
        if (!inCCInfoList.isNullOrEmpty()) {
            nodeDao.updateNodeHostIdByIp(dslContext, inCCInfoList) // 在CC中 - 将host_id写回T_NODE表中
            val inCCIpList = inCCInfoList.mapNotNull { it.bkHostInnerip }
            nodeDao.updateNodeInCCByIp(dslContext, inCCIpList) // 在CC中 - NODE_STATUS字段 改成 NORMAL
        }
        if (logger.isDebugEnabled) logger.debug("---[checkNodeInCC]End Check whether the node is in the cc.---")
    }

    /**
     * 定时任务：执行一次就行。
     * 分组执行，每次遍历100条记录。
     * display_name为空的：拼接节点类型、node hash值、nodeId这三个字段，写入display_name。
     */
    @Scheduled(cron = "0 40 12 * * 1-5")
    fun writeDisplayName() {
        val countDisplayNameEmptyNodes = nodeDao.countDisplayNameEmptyNodes(dslContext)
        if (logger.isDebugEnabled)
            logger.debug("[writeDisplayName]countDisplayNameEmptyNodes:$countDisplayNameEmptyNodes.")
        if (0 < countDisplayNameEmptyNodes) {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countDisplayNameEmptyNodes.toLong())
            for (page in 1..totalPages) {
                val displayNameNullNodeRecords =
                    nodeDao.getNodesWhoseDisplayNameIsEmpty(dslContext, page, DEFAULT_PAGE_SIZE)
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

    /**
     * 定时任务：执行一次就行。
     * 分组执行，每次遍历100条记录。
     * 对于 nodeType 为 CMDB 的机器，写入host_id(CC中查到的)，并将云区域ID设为0。
     */
    @Scheduled(cron = "0 41 12 * * 1-5")
    fun writeHostIdAndCloudAreaId() {
        val countCmdbNodes = nodeDao.countCmdbNodes(dslContext)
        if (logger.isDebugEnabled) logger.debug("[writeHostIdAndCloudAreaId]countCmdbNodes:$countCmdbNodes.")
        if (0 < countCmdbNodes) {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countCmdbNodes.toLong())
            for (page in 1..totalPages) {
                val cmdbNodesRecords =
                    nodeDao.getCmdbNodesLimit(dslContext, page, DEFAULT_PAGE_SIZE)
                val cmdbNodesIp = cmdbNodesRecords.map { it.value3() }.toSet()
                val inCCInfoList = queryFromCCService.queryCCListHostWithoutBizByInRules(
                    listOf(FIELD_BK_HOST_ID, FIELD_BK_HOST_INNERIP), cmdbNodesIp, FIELD_BK_HOST_INNERIP
                ).data?.info
                if (!inCCInfoList.isNullOrEmpty()) { // 有就写入
                    val ipToCCInfoMap = inCCInfoList.associateBy { it.bkHostInnerip }
                    val nodeHostIdAndCloudAreaIdInfoList = cmdbNodesRecords
                        .filter {
                            ipToCCInfoMap.containsKey(it.value3())
                        }
                        .map {
                            HostIdAndCloudAreaIdInfo(
                                nodeId = it.value1(),
                                bkCloudId = DEFAULT_CLOUD_AREA_ID,
                                bkHostId = ipToCCInfoMap[it.value3()]?.bkHostId
                            )
                        }
                    nodeDao.updateHostIdAndCloudAreaIdByNodeId(dslContext, nodeHostIdAndCloudAreaIdInfoList)
                }
            }
        } else {
            if (logger.isDebugEnabled) logger.debug("[writeHostIdAndCloudAreaId] There is no cmdb node.")
        }
    }
}