package com.tencent.devops.environment.service.cmdb.impl

import com.tencent.devops.environment.pojo.cmdb.common.CmdbServerDTO
import com.tencent.devops.environment.pojo.cmdb.resp.CmdbServerPage
import com.tencent.devops.environment.service.cmdb.EsbCmdbClient
import com.tencent.devops.environment.service.cmdb.TencentCmdbService
import org.slf4j.LoggerFactory
import org.slf4j.helpers.MessageFormatter

/**
 * 对接公司老CMDB的实现类
 */
class TencentCmdbServiceImpl(
    private val esbCmdbClient: EsbCmdbClient
) : TencentCmdbService {

    companion object {
        private val logger = LoggerFactory.getLogger(TencentCmdbServiceImpl::class.java)

        /**
         * 查询CMDB服务器数据时使用的默认用户
         */
        private const val DEFAULT_USER = "admin"

        /**
         * 查询CMDB服务器的批量大小
         */
        private const val CMDB_SERVER_FETCH_BATCH_SIZE = 1000
    }

    override fun queryServerByServerId(serverIdSet: Set<Long>): Map<Long, CmdbServerDTO> {
        return queryCmdbServerByBatch(
            fetchRawCmdbDataFunc = { start, limit ->
                esbCmdbClient.queryCmdbServerByServerIds(
                    userId = DEFAULT_USER,
                    serverIds = serverIdSet,
                    start = start,
                    limit = limit
                )
            },
            keySelector = { it.serverId }
        )
    }

    override fun queryServerByIp(ipSet: Set<String>): Map<String, CmdbServerDTO> {
        return queryCmdbServerByBatch(
            fetchRawCmdbDataFunc = { start, limit ->
                esbCmdbClient.queryCmdbServerByIps(
                    userId = DEFAULT_USER,
                    ips = ipSet,
                    start = start,
                    limit = limit
                )
            },
            keySelector = { it.ip }
        )
    }

    /**
     * 分批查询CMDB服务器信息
     */
    private fun <K> queryCmdbServerByBatch(
        fetchRawCmdbDataFunc: (start: Int, limit: Int) -> CmdbServerPage,
        keySelector: (CmdbServerDTO) -> K
    ): Map<K, CmdbServerDTO> {
        var start = 0
        var totalRowNum: Int
        val serverList = mutableListOf<CmdbServerDTO>()
        val startTime = System.currentTimeMillis()
        do {
            val cmdbServerPage = fetchRawCmdbDataFunc(start, CMDB_SERVER_FETCH_BATCH_SIZE)
            serverList.addAll(cmdbServerPage.nodes.map {
                CmdbServerDTO.fromRawCmdbNode(it)
            })
            start += CMDB_SERVER_FETCH_BATCH_SIZE
            totalRowNum = cmdbServerPage.totalRows
        } while (start < totalRowNum)
        val duration = System.currentTimeMillis() - startTime
        val logMessage = MessageFormatter.format(
            "{} server queried from CMDB, cost: {}ms",
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
}
