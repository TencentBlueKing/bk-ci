package com.tencent.devops.environment.service.cmdb.impl

import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.CmdbNode
import com.tencent.devops.environment.pojo.ScrollIdPage
import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbScrollPageData
import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbServer
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.service.cc.TencentCCService
import com.tencent.devops.environment.service.gseagent.utils.NodeStatusUtils
import com.tencent.devops.environment.service.job.QueryAgentStatusService
import com.tencent.devops.environment.utils.BinarySearchUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.min

/**
 * 导入测试机-选择CMDB节点服务
 */
@Service
class ImportCmdbNodeService(
    @Autowired(required = false)
    private val newCmdbService: TencentNewCmdbServiceImpl?,
    private val cmdbNodeDao: CmdbNodeDao,
    private val tencentCCService: TencentCCService,
    private val queryAgentStatusService: QueryAgentStatusService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ImportCmdbNodeService::class.java)
    }

    /**
     * 查询用户名下的CMDB服务器
     * @param userId 用户名
     * @param projectId 项目id
     * @param bakOperator 是否是备负责人
     * @param scrollId 分页游标
     * @param pageSize 分页大小
     * @param ips 要搜索的服务器ip列表
     * @return 服务器分页数据
     */
    fun listUserCmdbNodes(
        userId: String,
        projectId: String,
        bakOperator: Boolean,
        scrollId: String,
        pageSize: Int,
        ips: List<String>?
    ): ScrollIdPage<CmdbNode> {
        // 1.查出CMDB中的机器信息
        val cmdbServerPage: NewCmdbScrollPageData<NewCmdbServer> = if (ips.isNullOrEmpty()) {
            queryServerByMaintainerOrBak(userId, bakOperator, scrollId, pageSize)
        } else {
            queryServerByIpsAndFilterByMaintainer(userId, bakOperator, scrollId, pageSize, ips)
        }
        checkCmdbServerPage(cmdbServerPage)
        // 2.转换为CmdbNode类型数据
        val serverIdSet = mutableSetOf<Long>()
        val cmdbNodeList = cmdbServerPage.list.map {
            serverIdSet.add(it.serverId)
            it.toCmdbNode()
        }
        // 3.填充已导入节点的导入状态与节点状态数据
        val unImportedServerIdSet = fillStatusForImportedNode(projectId, serverIdSet, cmdbNodeList)
        // 4.填充未导入节点的导入状态与节点状态数据
        fillStatusForUnImportedNode(unImportedServerIdSet, cmdbNodeList)
        return ScrollIdPage(
            scrollId = cmdbServerPage.scrollId!!,
            hasNext = cmdbServerPage.hasNext!!,
            records = cmdbNodeList
        )
    }

    /**
     * 直接通过主/备负责人查询服务器
     */
    private fun queryServerByMaintainerOrBak(
        userId: String,
        bakOperator: Boolean,
        scrollId: String,
        pageSize: Int
    ): NewCmdbScrollPageData<NewCmdbServer> {
        return if (!bakOperator) {
            newCmdbService!!.queryServerByMaintainer(userId, pageSize, scrollId)
        } else {
            newCmdbService!!.queryServerByBakMaintainer(userId, pageSize, scrollId)
        }
    }

    /**
     * 新CMDB接口限制主/备负责人与IP无法同时作为条件查询服务器数据，
     * 因此只能先通过IP查询服务器，再根据主/备负责人进行过滤
     */
    private fun queryServerByIpsAndFilterByMaintainer(
        userId: String,
        bakOperator: Boolean,
        scrollId: String,
        pageSize: Int,
        ips: List<String>
    ): NewCmdbScrollPageData<NewCmdbServer> {
        var serverList = newCmdbService!!.queryNewServerByIp(ips.toSet()).values.filter {
            if (!bakOperator) {
                it.hasOperator(userId)
            } else {
                it.hasBakOperator(userId)
            }
        }.toList()
        serverList = serverList.sortedBy { it.serverId }
        return buildScrollPageDataInMemory(serverList, scrollId, pageSize)
    }

    /**
     * 从内存中构建CMDB节点分页数据
     * @param serverIdSortedServerList 按serverId升序排序后的CMDB节点列表
     * @param scrollIdStr 分页游标
     * @param pageSize 分页大小
     * @return 分页数据
     */
    private fun buildScrollPageDataInMemory(
        serverIdSortedServerList: List<NewCmdbServer>,
        scrollIdStr: String,
        pageSize: Int
    ): NewCmdbScrollPageData<NewCmdbServer> {
        if (serverIdSortedServerList.isEmpty()) {
            return NewCmdbScrollPageData(
                scrollId = scrollIdStr,
                hasNext = false,
                list = emptyList()
            )
        }
        // 1.找出scrollId对应服务器所在的下标
        val scrollId = scrollIdStr.toLong()
        val scrollIdIndex = BinarySearchUtils.binarySearchIndexOrCeil(
            serverIdSortedServerList.map { it.serverId },
            scrollId
        )
        if (scrollIdIndex < 0) {
            return NewCmdbScrollPageData(
                scrollId = scrollIdStr,
                hasNext = false,
                list = emptyList()
            )
        }
        // 2.截取一页数据，从scrollId对应服务器的下一台开始截取
        val scrollIdServerId = serverIdSortedServerList[scrollIdIndex].serverId
        val startIndex = if (scrollIdServerId == scrollId) scrollIdIndex + 1 else scrollIdIndex
        val endIndex = min(startIndex + pageSize, serverIdSortedServerList.size)
        val pageServerList = serverIdSortedServerList.subList(startIndex, endIndex)
        // 3.构造分页数据返回
        val newScrollId = serverIdSortedServerList[endIndex - 1].serverId
        return NewCmdbScrollPageData(
            scrollId = newScrollId.toString(),
            hasNext = endIndex < serverIdSortedServerList.size,
            list = pageServerList
        )
    }

    private fun checkCmdbServerPage(cmdbServerPage: NewCmdbScrollPageData<NewCmdbServer>) {
        if (cmdbServerPage.scrollId == null) {
            logger.warn("CMDBDataError|scrollIdIsNull")
        }
        if (cmdbServerPage.hasNext == null) {
            logger.warn("CMDBDataError|hasNextIsNull")
        }
    }

    /**
     * 为已导入的CMDB节点填充导入状态和节点状态
     * @param projectId 项目id
     * @param serverIdSet 服务器id集合
     * @param cmdbNodeList 待填充的CMDB节点列表
     * @return 未导入的服务器id集合
     */
    private fun fillStatusForImportedNode(
        projectId: String,
        serverIdSet: MutableSet<Long>,
        cmdbNodeList: List<CmdbNode>
    ): Set<Long> {
        val cmdbNodeStatusList = cmdbNodeDao.listCmdbNodeStatusByProjectIdAndServerId(
            projectId, serverIdSet
        )
        val serverIdToCmdbNodeStatusMap = cmdbNodeStatusList.associateBy { it.serverId }
        val importedServerIdSet = cmdbNodeStatusList.map { it.serverId }.toSet()
        cmdbNodeList.forEach {
            val importStatus = importedServerIdSet.contains(it.serverId)
            it.importStatus = importStatus
            if (importStatus) {
                it.nodeStatus = serverIdToCmdbNodeStatusMap[it.serverId]?.nodeStatus
            }
        }
        return serverIdSet.minus(importedServerIdSet.filterNotNull().toSet())
    }

    /**
     * 为未导入的CMDB节点填充导入状态和节点状态
     * @param serverIdSet 服务器id集合
     * @param cmdbNodeList 待填充的CMDB节点列表
     * @return 未导入的服务器id集合
     */
    private fun fillStatusForUnImportedNode(
        serverIdSet: Set<Long>,
        cmdbNodeList: List<CmdbNode>
    ) {
        // 1.批量获取机器状态
        val ccHostList = tencentCCService.listHostByServerId(serverIdSet)
        val notInCCServerIdSet = serverIdSet.minus(ccHostList.map { it.svrId }.toSet())
        val agentVersionList = queryAgentStatusService.getAgentVersions(
            ccHostList.map {
                AgentVersion(serverId = it.svrId, ip = it.bkHostInnerip, bkHostId = it.bkHostId)
            }
        )
        val serverIdToAgentVersionMap = agentVersionList?.associateBy { it.serverId } ?: emptyMap()
        // 2.填充导入状态和节点状态
        val serverIdCmdbNodeMap = cmdbNodeList.associateBy { it.serverId }
        serverIdSet.forEach { serverId ->
            val cmdbNode = serverIdCmdbNodeMap[serverId]
            cmdbNode?.importStatus = false
            cmdbNode?.nodeStatus = computeNodeStatus(serverId, notInCCServerIdSet, serverIdToAgentVersionMap)
        }
    }

    /**
     * 计算蓝盾定义的CMDB节点状态
     * @param serverId 服务器id
     * @param notInCCServerIdSet 未在CC中查询到的服务器id集合
     * @param serverIdToAgentVersionMap 服务器id到AgentVersion的映射
     * @return 节点状态
     */
    private fun computeNodeStatus(
        serverId: Long,
        notInCCServerIdSet: Set<Long?>,
        serverIdToAgentVersionMap: Map<Long?, AgentVersion>
    ): String {
        if (notInCCServerIdSet.contains(serverId)) {
            return NodeStatus.NOT_IN_CC.name
        }
        val agentVersion = serverIdToAgentVersionMap[serverId]
        return NodeStatusUtils.getNodeStatus(agentVersion)
    }
}
