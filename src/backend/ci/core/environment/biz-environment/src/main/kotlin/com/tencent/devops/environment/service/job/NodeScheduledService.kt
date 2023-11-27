package com.tencent.devops.environment.service.job

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.environment.dao.NodeDao
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
    }

    /**
     * 后台定时轮询机器状态，看机器是否在CC中（T_NODE表中host_id字段：为空 - 不在，不为空 - 在）
     * 遍历T_NODE表中host_id不为空的记录，用host_id 调用find_host_biz_relations接口，看能否得到对应记录：能-不操作，不能-对应记录host_id置为null
     * cron：每天上午10点执行。
     */
    @Scheduled(cron = "0 20 15 * * 1-5")
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
        val hostIdList = nodeRecords.map { it.hostId } // 要判断在不在cc中的 所有host_id

        val nodeCCList =
            if (hostIdList.isNotEmpty()) queryFromCCService.queryCCFindHostBizRelations(hostIdList).data
            else emptyList() // 在cc中的 host_id对应cc记录
        val hostIdToNodeCCMap = nodeCCList?.associateBy { it.bkHostId.toLong() } // 在cc中的 host_id-cc记录 映射
        // 不在cc中了，要置空的hostid，且 NODE_STATUS字段 要改成 NOT_IN_CC
        val invalidHostIdList = hostIdList.filterNot { hostIdToNodeCCMap?.containsKey(it) ?: false }
        nodeDao.updateNodeNotInCC(dslContext, invalidHostIdList)

        // T_NODE表中 NODE_STATUS字段 为NOT_IN_CC的记录，再去查在不在cc中：不在CC中 - 不处理；在CC中 - 将host_id写回T_NODE表中，NODE_STATUS字段 改成 NORMAL
        val nodeRecordsNotInCC = nodeDao.getNodesNotInCC(dslContext)
        val notInCCIpList = nodeRecordsNotInCC.map { it.nodeIp }
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
}