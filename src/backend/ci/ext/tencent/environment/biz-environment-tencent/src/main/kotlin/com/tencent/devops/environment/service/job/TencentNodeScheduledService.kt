package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.job.HostIdAndCloudAreaIdInfo
import com.tencent.devops.environment.service.CmdbNodeService
import com.tencent.devops.environment.service.CmdbNodeService.Companion.FIELD_BK_SVR_ID
import com.tencent.devops.environment.service.job.QueryFromCCService.Companion.FIELD_BK_HOST_ID
import com.tencent.devops.environment.service.job.QueryFromCCService.Companion.FIELD_BK_HOST_INNERIP
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
    private val queryFromCCService: QueryFromCCService,
    private val cmdbNodeService: CmdbNodeService,
    private val nodeScheduledService: NodeScheduledService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TencentNodeScheduledService::class.java)
        private const val CHECK_NODE_IN_CMDB_TIMEROUT_LOCK_KEY = "check_node_in_cmdb_timeout_lock"
        private const val SCHEDULED_WRITE_HOST_ID_CLOUD_AREA_ID_TIMEOUT_LOCK_KEY =
            "scheduled_write_host_id_and_cloud_area_id_timeout_lock"
        private const val SCHEDULED_ADD_NODE_TO_CC_TIMEOUT_LOCK_KEY = "scheduled_add_node_to_cc_timeout_lock"
        private const val DEFAULT_PAGE_SIZE = 100
    }

    /**
     * 定时任务：执行一次就行。
     * 分组执行，每次遍历100条记录。
     * 将不在CC中的 类型为CMDB 的节点，添加到CC中，并返回host_id和云区域id，将host_id和云区域id写入表中
     */
    @Scheduled(cron = "0 0 11 * * 1-5")
    fun scheduledAddNodeToCC() {
        nodeScheduledService.taskWithRedisLock(SCHEDULED_ADD_NODE_TO_CC_TIMEOUT_LOCK_KEY, ::addNodeToCC)
    }

    /**
     * 定时任务：执行一次就行。
     * 分组执行，每次遍历100条记录。
     * 对于 nodeType 为 CMDB 的机器，写入host_id和云区域ID(都是CC中查到的)。
     */
    @Scheduled(cron = "0 0 10 * * 1-5")
    fun scheduledWriteHostIdAndCloudAreaId() {
        nodeScheduledService.taskWithRedisLock(
            SCHEDULED_WRITE_HOST_ID_CLOUD_AREA_ID_TIMEOUT_LOCK_KEY, ::writeHostIdAndCloudAreaId
        )
    }

    /**
     * 后台定时轮询机器状态，看机器在不在公司cmdb中
     * 只轮询T_NODE表中 NODE_TYPE=="CMDB"的记录，用ip 调用get_query_info接口，看能否得到对应记录：能-不操作，不能-对应节点的 NODE_STATUS字段 改成 NOT_IN_CMDB
     * cron：每天上午9点执行。
     */
    @Scheduled(cron = "0 0 9 * * 1-5")
    fun scheduledCheckNodeInCmdb() {
        nodeScheduledService.taskWithRedisLock(CHECK_NODE_IN_CMDB_TIMEROUT_LOCK_KEY, ::checkNodeInCmdb)
    }

    private fun addNodeToCC() {
        val countCmdbNodes = nodeDao.countCmdbNodes(dslContext)
        if (logger.isDebugEnabled) logger.debug("[addNodeToCC]countCmdbNodes:$countCmdbNodes")
        countCmdbNodes.takeIf { it > 0 }?.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countCmdbNodes.toLong())
            for (page in 1..totalPages) {
                addNodeToCCByPage(page)
            }
        }
    }

    private fun addNodeToCCByPage(page: Int) {
        val cmdbNodesRecords =
            nodeDao.getCmdbNodesHostIdNullLimit(dslContext, page - 1, DEFAULT_PAGE_SIZE) // ip, nodeId
        if (logger.isDebugEnabled) logger.debug("[addNodeToCC]cmdbNodesRecords:$cmdbNodesRecords")
        val cmdbNodesIp = cmdbNodesRecords.map { it.value1() }.toSet()
        if (logger.isDebugEnabled) logger.debug("[addNodeToCC]cmdbNodesIp:$cmdbNodesIp.")
        val nodeIpToNodesRecords = cmdbNodesRecords.associateBy { it.value1() }
        val ipToCmdbInfoMap = tencentQueryFromCmdbService.queryCmdbInfoFromIp(cmdbNodesIp)
        if (logger.isDebugEnabled) logger.debug("[addNodeToCC]ipToCmdbInfoMap:$ipToCmdbInfoMap.")
        if (!ipToCmdbInfoMap.isNullOrEmpty()) {
            val svrIdToCmdbInfoMap = ipToCmdbInfoMap.values
                .associateBy { it.serverId?.toLong() }
            if (logger.isDebugEnabled) logger.debug("[addNodeToCC]svrIdToCmdbInfoMap:$svrIdToCmdbInfoMap.")
            val svrIdList = ipToCmdbInfoMap.values.mapNotNull { it.serverId?.toLong() } // 从cmdb中查到的 所有svrId
            val (_, inCCSvrIdList, notInCCSvrIdList) = cmdbNodeService.checkNodeInCCBySvrId(svrIdList)
            if (logger.isDebugEnabled) logger.debug("[addNodeToCC]inCCSvrIdList:$inCCSvrIdList.")
            if (logger.isDebugEnabled) logger.debug("[addNodeToCC]notInCCSvrIdList:$notInCCSvrIdList.")
            // 将不在CC中的svrId，添加到CC中，查出host_id和云区域id，写入db对应记录
            val addToCCResp = queryFromCCService.addHostToCiBiz(notInCCSvrIdList)
            if (logger.isDebugEnabled) logger.debug("[addNodeToCC]addToCCResp:$addToCCResp")
            val ccHostIdList = addToCCResp.data?.bkHostIds
            val addToCCInfoList = ccHostIdList?.mapIndexed { index, value ->
                HostIdAndCloudAreaIdInfo(
                    nodeId = nodeIpToNodesRecords[svrIdToCmdbInfoMap[notInCCSvrIdList[index]]?.SvrIp]
                        ?.value2(),
                    bkCloudId = null,
                    bkHostId = value
                )
            }
            if (!addToCCInfoList.isNullOrEmpty()) {
                nodeDao.updateHostIdAndCloudAreaIdByNodeId(dslContext, addToCCInfoList)
            }
        }
    }

    private fun writeHostIdAndCloudAreaId() {
        val countCmdbNodes = nodeDao.countCmdbNodes(dslContext)
        if (logger.isDebugEnabled) logger.debug("[writeHostIdAndCloudAreaId]countCmdbNodes:$countCmdbNodes")
        countCmdbNodes.takeIf { it > 0 }?.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countCmdbNodes.toLong())
            for (page in 1..totalPages) {
                val cmdbNodesRecords =
                    nodeDao.getCmdbNodesWhoseHostIdNullLimit(dslContext, page - 1, DEFAULT_PAGE_SIZE)
                if (logger.isDebugEnabled) logger.debug("[writeHostIdAndCloudAreaId]cmdbNodesRecords:$cmdbNodesRecords")
                val cmdbNodesIp = cmdbNodesRecords.map { it.value3() }.toSet()
                if (logger.isDebugEnabled) logger.debug("[writeHostIdAndCloudAreaId]cmdbNodesIp:$cmdbNodesIp.")
                val serverIdList = tencentQueryFromCmdbService.queryCmdbInfoFromIp(cmdbNodesIp)
                    ?.values
                    ?.map { it.serverId } // 从cmdb中查到的 所有svrId
                val inCCInfoList = queryFromCCService.queryCCListHostWithoutBizByInRules(
                    listOf(FIELD_BK_HOST_ID, FIELD_BK_HOST_INNERIP), serverIdList, FIELD_BK_SVR_ID
                ).data?.info
                if (logger.isDebugEnabled) logger.debug("[writeHostIdAndCloudAreaId]inCCInfoList:$inCCInfoList.")
                if (!inCCInfoList.isNullOrEmpty()) { // 有就写入
                    val ipToCCInfoMap = inCCInfoList.associateBy { it.bkHostInnerip }
                    val nodeHostIdAndCloudAreaIdInfoList = cmdbNodesRecords
                        .filter {
                            ipToCCInfoMap.containsKey(it.value3())
                        }
                        .map {
                            HostIdAndCloudAreaIdInfo(
                                nodeId = it.value1(),
                                bkCloudId = null,
                                bkHostId = ipToCCInfoMap[it.value3()]?.bkHostId
                            )
                        }
                    nodeDao.updateHostIdAndCloudAreaIdByNodeId(dslContext, nodeHostIdAndCloudAreaIdInfoList)
                }
            }
        }
    }

    private fun checkNodeInCmdb() {
        val countNodeInCmdb = nodeDao.countCmdbNodes(dslContext)
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCmdb]countNodeInCmdb:$countNodeInCmdb")
        if (0 < countNodeInCmdb) {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countNodeInCmdb.toLong())
            for (page in 1..totalPages) {
                checkNodeInCmdb(page)
            }
        }
        if (logger.isDebugEnabled) logger.debug("[checkNodeInCmdb]End Check whether the node is in the cmdb.")
    }

    private fun checkNodeInCmdb(page: Int) {
        val nodeRecords = nodeDao.getCmdbNodesWhoseHostIdNullLimit(
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
        if (inCmdbIpRecords.isNotEmpty) {
            val inCmdbSvrIdList = inCmdbIpRecords.map { it.value1() } // 需要再查询一次CC的ip
                .mapNotNull { ipToCmdbInfoMap?.get(it)?.serverId?.toLong() } // 需要再查询一次CC的svrId

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