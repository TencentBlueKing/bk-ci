package com.tencent.devops.environment.service.cmdb.impl

import com.tencent.devops.environment.pojo.cmdb.common.CmdbServerDTO
import com.tencent.devops.environment.pojo.cmdb.req.NewCmdbCondition
import com.tencent.devops.environment.pojo.cmdb.req.NewCmdbConditionValue
import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbScrollPageData
import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbServer
import com.tencent.devops.environment.service.cmdb.NewCmdbClient
import com.tencent.devops.environment.service.cmdb.TencentCmdbService

/**
 * 使用新CMDB接口查询机器信息的服务
 */
class TencentNewCmdbServiceImpl(
    private val newCmdbClient: NewCmdbClient
) : TencentCmdbService {

    /**
     * 使用公司新CMDB接口根据serverId查询服务器列表
     * @param serverIdSet 服务器ID集合
     * @return 服务器信息Map<serverId, CmdbServerDTO>
     */
    override fun queryServerByServerId(serverIdSet: Set<Long>): Map<Long, CmdbServerDTO> {
        val serverIdCondition = buildServerIdCondition(serverIdSet)
        val serverList = newCmdbClient.queryAllServerByBaseCondition(serverIdCondition)
        return convertServerListToIdServerMap(serverList)
    }

    /**
     * 使用公司新CMDB接口根据IP查询服务器列表
     * @param ipSet IP集合
     * @return 服务器信息Map<ip, CmdbServerDTO>
     */
    override fun queryServerByIp(ipSet: Set<String>): Map<String, CmdbServerDTO> {
        val serverIpCondition = buildServerIpCondition(ipSet)
        val serverList = newCmdbClient.queryAllServerByBaseCondition(serverIpCondition)
        return convertServerListToIpServerMap(serverList)
    }

    /**
     * 使用公司新CMDB接口根据主负责人查询服务器列表
     * @param maintainer 主负责人
     * @param size 分页大小
     * @param scrollId 分页游标
     * @return 服务器分页数据
     */
    fun queryServerByMaintainer(
        maintainer: String,
        ips: List<String>?,
        size: Int,
        scrollId: String
    ): NewCmdbScrollPageData<NewCmdbServer> {
        val condition = buildMaintainerAndIpsCondition(maintainer, ips)
        return newCmdbClient.queryAllServerByBusiness(condition, size, scrollId)
    }

    /**
     * 使用公司新CMDB接口根据备份负责人查询服务器列表
     * @param bakMaintainer 备份负责人
     * @param size 分页大小
     * @param scrollId 分页游标
     * @return 服务器分页数据
     */
    fun queryServerByBakMaintainer(
        bakMaintainer: String,
        ips: List<String>?,
        size: Int,
        scrollId: String
    ): NewCmdbScrollPageData<NewCmdbServer> {
        val condition = buildBakMaintainerAndIpsCondition(bakMaintainer, ips)
        return newCmdbClient.queryAllServerByBusiness(condition, size, scrollId)
    }

    private fun buildServerIdCondition(serverIdSet: Set<Long>): NewCmdbCondition {
        return NewCmdbCondition(
            serverId = NewCmdbConditionValue(
                operator = NewCmdbConditionValue.Operator.IN,
                value = serverIdSet.map { it.toInt() }
            )
        )
    }

    private fun buildServerIpCondition(ipSet: Set<String>): NewCmdbCondition {
        return NewCmdbCondition(
            serverIp = NewCmdbConditionValue(
                operator = NewCmdbConditionValue.Operator.IN,
                value = ipSet.toList()
            )
        )
    }

    private fun buildMaintainerAndIpsCondition(
        maintainer: String,
        ips: List<String>?
    ): NewCmdbCondition {
        return NewCmdbCondition(
            serverIp = buildIpsConditionValue(ips),
            maintainer = NewCmdbConditionValue(
                operator = NewCmdbConditionValue.Operator.IN,
                value = listOf(maintainer)
            )
        )
    }

    private fun buildBakMaintainerAndIpsCondition(
        bakMaintainer: String,
        ips: List<String>?
    ): NewCmdbCondition {
        return NewCmdbCondition(
            serverIp = buildIpsConditionValue(ips),
            maintainerBak = NewCmdbConditionValue(
                operator = NewCmdbConditionValue.Operator.IN,
                value = listOf(bakMaintainer)
            )
        )
    }

    private fun buildIpsConditionValue(ips: List<String>?): NewCmdbConditionValue<String>? {
        if (ips.isNullOrEmpty()) {
            return null
        }
        return NewCmdbConditionValue(
            operator = NewCmdbConditionValue.Operator.IN,
            value = ips
        )
    }

    private fun convertServerListToIdServerMap(serverList: List<NewCmdbServer>): Map<Long, CmdbServerDTO> {
        return serverList
            .map { CmdbServerDTO.fromNewCmdbServer(it) }
            .associateBy { it.serverId }
    }

    private fun convertServerListToIpServerMap(serverList: List<NewCmdbServer>): Map<String, CmdbServerDTO> {
        return serverList
            .map { CmdbServerDTO.fromNewCmdbServer(it) }
            .associateBy { it.ip }
    }
}
