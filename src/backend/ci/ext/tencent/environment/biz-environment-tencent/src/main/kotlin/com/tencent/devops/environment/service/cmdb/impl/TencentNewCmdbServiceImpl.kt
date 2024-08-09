package com.tencent.devops.environment.service.cmdb.impl

import com.tencent.devops.environment.pojo.cmdb.common.CmdbServerDTO
import com.tencent.devops.environment.pojo.cmdb.req.NewCmdbCondition
import com.tencent.devops.environment.pojo.cmdb.req.NewCmdbConditionValue
import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbScrollPageData
import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbServer
import com.tencent.devops.environment.service.cmdb.NewCmdbClient
import com.tencent.devops.environment.service.cmdb.TencentCmdbService
import org.slf4j.LoggerFactory
import org.slf4j.helpers.MessageFormatter
import kotlin.math.min

/**
 * 使用新CMDB接口查询机器信息的服务
 */
class TencentNewCmdbServiceImpl(
    private val newCmdbClient: NewCmdbClient
) : TencentCmdbService {

    companion object {
        private val logger = LoggerFactory.getLogger(TencentNewCmdbServiceImpl::class.java)

        /**
         * 查询新CMDB服务器时单次传入的查询条件取值批量大小
         */
        private const val QUERY_VALUE_BATCH_SIZE = 50
    }

    /**
     * 使用公司新CMDB接口根据serverId查询服务器列表
     * @param serverIds 服务器ID集合
     * @return 通用服务器信息Map<serverId, CmdbServerDTO>
     */
    override fun queryServerByServerId(serverIds: Collection<Long>): Map<Long, CmdbServerDTO> {
        val newCmdbServerMap = queryNewCmdbServerByBatch(
            queryValues = serverIds,
            buildNewCmdbConditionFunc = this::buildServerIdCondition,
            fetchNewCmdbDataFunc = newCmdbClient::queryAllServerByBaseCondition,
            keySelector = { server -> server.serverId }
        )
        return newCmdbServerMap.mapValues { (_, newCmdbServer) ->
            CmdbServerDTO.fromNewCmdbServer(newCmdbServer)
        }
    }

    /**
     * 使用公司新CMDB接口根据IP查询服务器列表
     * @param ips IP集合
     * @return 通用服务器信息Map<ip, CmdbServerDTO>
     */
    override fun queryServerByIp(ips: Collection<String>): Map<String, CmdbServerDTO> {
        val newCmdbServerMap = queryNewServerByIp(ips)
        return newCmdbServerMap.mapValues { (_, newCmdbServer) ->
            CmdbServerDTO.fromNewCmdbServer(newCmdbServer)
        }
    }

    /**
     * 使用公司新CMDB接口根据IP查询服务器列表
     * @param ips IP集合
     * @return 新CMDB服务器信息Map<ip, NewCmdbServer>
     */
    fun queryNewServerByIp(ips: Collection<String>): Map<String, NewCmdbServer> {
        return queryNewCmdbServerByBatch(
            queryValues = ips,
            buildNewCmdbConditionFunc = this::buildServerIpCondition,
            fetchNewCmdbDataFunc = newCmdbClient::queryAllServerByBaseCondition,
            keySelector = { server -> server.getFirstIp() ?: "" }
        )
    }

    /**
     * 分批查询新CMDB服务器信息
     * @param queryValues 查询条件取值集合
     * @param buildNewCmdbConditionFunc 构建新CMDB查询条件函数
     * @param fetchNewCmdbDataFunc 获取新CMDB数据函数
     * @param keySelector 分组聚合Key选择器
     */
    private fun <K> queryNewCmdbServerByBatch(
        queryValues: Collection<K>,
        buildNewCmdbConditionFunc: (values: Collection<K>) -> NewCmdbCondition,
        fetchNewCmdbDataFunc: (condition: NewCmdbCondition) -> List<NewCmdbServer>,
        keySelector: (NewCmdbServer) -> K
    ): Map<K, NewCmdbServer> {
        var start = 0
        val serverList = mutableListOf<NewCmdbServer>()
        val startTime = System.currentTimeMillis()
        val queryValueList = queryValues.toList()
        do {
            val end = min(start + QUERY_VALUE_BATCH_SIZE, queryValueList.size)
            val subValueList = queryValueList.subList(start, end)
            val condition = buildNewCmdbConditionFunc(subValueList)
            val batchServerList = fetchNewCmdbDataFunc(condition)
            serverList.addAll(batchServerList)
            start += QUERY_VALUE_BATCH_SIZE
        } while (start < queryValueList.size)
        val duration = System.currentTimeMillis() - startTime
        val logMessage = MessageFormatter.format(
            "queryNewCMDBServer|count={}|cost={}ms",
            serverList.size,
            duration
        ).message
        if (duration >= 5000) {
            logger.warn(logMessage)
        } else if (duration >= 1000) {
            logger.info(logMessage)
        }
        return serverList.associateBy(keySelector)
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
        size: Int,
        scrollId: String
    ): NewCmdbScrollPageData<NewCmdbServer> {
        val condition = buildMaintainerCondition(maintainer)
        val cmdbServerPage = newCmdbClient.queryAllServerByBusiness(condition, size, scrollId)
        setRealHasNext(cmdbServerPage, condition, size)
        return cmdbServerPage
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
        size: Int,
        scrollId: String
    ): NewCmdbScrollPageData<NewCmdbServer> {
        val condition = buildBakMaintainerCondition(bakMaintainer)
        val cmdbServerPage = newCmdbClient.queryAllServerByBusiness(condition, size, scrollId)
        setRealHasNext(cmdbServerPage, condition, size)
        return cmdbServerPage
    }

    /**
     * 解决公司CMDB已知问题：hasNext标识可能不准（最后一页数据还是true，要再请求一次到list为空列表才为false），
     * 因此hasNext字段无法直接使用，需要再查一次获得真正的hasNext值
     * @param cmdbServerPage 服务器分页数据
     * @param condition 查询条件
     * @param size 分页大小
     */
    private fun setRealHasNext(
        cmdbServerPage: NewCmdbScrollPageData<NewCmdbServer>,
        condition: NewCmdbCondition,
        size: Int
    ) {
        val nextCmdbServerPage = newCmdbClient.queryAllServerByBusiness(condition, size, cmdbServerPage.scrollId!!)
        cmdbServerPage.hasNext = nextCmdbServerPage.list.isNotEmpty()
    }

    private fun buildServerIdCondition(serverIds: Collection<Long>): NewCmdbCondition {
        return NewCmdbCondition(
            serverId = NewCmdbConditionValue(
                operator = NewCmdbConditionValue.Operator.IN,
                value = serverIds.map { it.toInt() }
            )
        )
    }

    private fun buildServerIpCondition(ips: Collection<String>): NewCmdbCondition {
        return NewCmdbCondition(
            serverIp = NewCmdbConditionValue(
                operator = NewCmdbConditionValue.Operator.IN,
                value = ips.toList()
            )
        )
    }

    private fun buildMaintainerCondition(maintainer: String): NewCmdbCondition {
        return NewCmdbCondition(
            maintainer = NewCmdbConditionValue(
                operator = NewCmdbConditionValue.Operator.IN,
                value = listOf(maintainer)
            )
        )
    }

    private fun buildBakMaintainerCondition(bakMaintainer: String): NewCmdbCondition {
        return NewCmdbCondition(
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
}
