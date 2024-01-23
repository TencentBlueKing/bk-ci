package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.constant.DEFAULT_SYTEM_USER
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.job.cmdbreq.CmdbGetQueryInfoReq
import com.tencent.devops.environment.pojo.job.cmdbreq.CmdbKeyValues
import com.tencent.devops.environment.pojo.job.cmdbreq.CmdbPagingInfo
import com.tencent.devops.environment.pojo.job.cmdbres.CmdbDataIns
import com.tencent.devops.environment.pojo.job.cmdbres.CmdbResp
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TencentQueryFromCmdbService : IQueryOperatorService {
    @Value("\${environment.apigw.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${environment.apigw.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${environment.cmdb.cmdbGetQueryInfoBaseUrl:}")
    private val cmdbGetQueryInfoBaseUrl = ""

    @Value("\${job.cmdbGetQueryInfoPath:#{\"/get_query_info\"}}")
    private val cmdbGetQueryInfoPath = ""

    companion object {
        private val logger = LoggerFactory.getLogger(TencentQueryFromCmdbService::class.java)
        private const val LOG_OUTPUT_MAX_LENGTH = 4000

        const val PAGE_SIZE = 1000
        const val COLUMN_SVR_BAK_OPERATOR = "SvrBakOperator"
        const val COLUMN_SVR_OPERATOR = "SvrOperator"
        const val COLUMN_SVR_IP = "SvrIp"
        const val COLUMN_SVR_NAME = "SvrName"
        const val COLUMN_SFW_NAME = "SfwName"
        const val COLUMN_SEVER_LAN_IP = "serverLanIP"
        const val DEFAULT_START_INDEX = 0
        const val DEFAULT_RETURN_TOTAL_ROWS = 1
    }

    /*
     *  判断：用户or节点导入人 是机器的主备负责人（用户：函数中形参userId；节点导入人：T_NODE表中的createdUser）
     *  ext中实现：从cmdb中 用ip查询机器的主备负责人
     */
    override fun isOperatorOrBakOperator(userId: String, nodeRecords: Set<TNodeRecord>) {
        val nodeIpList: List<String> = nodeRecords.map { it.nodeIp } // 所有host对应的ip
        if (logger.isDebugEnabled) logger.debug("[isOperatorOrBakOperator] nodeIpList: $nodeIpList")
        val nodeIpToNodeMap = nodeRecords.associateBy { it.nodeIp } // 所有host的：ip - 记录 映射

        val cmdbGetQueryInfoReq = CmdbGetQueryInfoReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            operator = DEFAULT_SYTEM_USER,
            reqColumn = listOf(
                COLUMN_SVR_BAK_OPERATOR, COLUMN_SVR_OPERATOR, COLUMN_SVR_IP,
                COLUMN_SVR_NAME, COLUMN_SFW_NAME, COLUMN_SEVER_LAN_IP
            ),
            keyValues = CmdbKeyValues(
                svrIp = nodeIpList.joinToString(separator = ";")
            ),
            pagingInfo = CmdbPagingInfo(DEFAULT_START_INDEX, PAGE_SIZE, DEFAULT_RETURN_TOTAL_ROWS)
        )
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        val responseBody = executePostRequest(
            headers, cmdbGetQueryInfoBaseUrl + cmdbGetQueryInfoPath, cmdbGetQueryInfoReq
        )
        if (logger.isDebugEnabled) logger.debug("[isOperatorOrBakOperator] responseBody: $responseBody")
        val cmdbIpToCmdbDataMap = getNodeIpToCmdbDataMap(responseBody)

        val invalidIpList = nodeIpList.filter {
            val isOperator = userId == cmdbIpToCmdbDataMap[it]?.SvrOperator ||
                nodeIpToNodeMap[it]?.createdUser == cmdbIpToCmdbDataMap[it]?.SvrOperator
            val isBakOpertor = cmdbIpToCmdbDataMap[it]?.SvrBakOperator?.split(";")?.contains(userId)!! ||
                cmdbIpToCmdbDataMap[it]?.SvrBakOperator?.split(";")?.contains(nodeIpToNodeMap[it]?.createdUser)!!
            !isOperator && !isBakOpertor
        }
        if (invalidIpList.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL_USER,
                params = arrayOf(invalidIpList.joinToString(","))
            )
        }
    }

    fun queryCmdbInfoFromIp(nodeIpList: Set<String>): Map<String, CmdbDataIns>? {
        val cmdbGetQueryInfoReq = CmdbGetQueryInfoReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            operator = DEFAULT_SYTEM_USER,
            reqColumn = listOf(COLUMN_SVR_IP, COLUMN_SVR_NAME, COLUMN_SFW_NAME),
            keyValues = CmdbKeyValues(
                svrIp = nodeIpList.joinToString(separator = ";")
            ),
            pagingInfo = CmdbPagingInfo(DEFAULT_START_INDEX, PAGE_SIZE, DEFAULT_RETURN_TOTAL_ROWS)
        )
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        if (logger.isDebugEnabled) logger.debug("[queryCmdb]req:$cmdbGetQueryInfoReq")
        if (logger.isDebugEnabled) logger.debug("[queryCmdb]url:${cmdbGetQueryInfoBaseUrl + cmdbGetQueryInfoPath}")
        val responseBody = executePostRequest(
            headers, cmdbGetQueryInfoBaseUrl + cmdbGetQueryInfoPath, cmdbGetQueryInfoReq
        )
        return getNodeIpToCmdbDataMap(responseBody)
    }

    private fun <T> executePostRequest(headers: Map<String, String>, url: String, req: T): String? {
        val requestContent = jacksonObjectMapper().writeValueAsString(req)
        if (logger.isDebugEnabled) logger.debug("[executePostRequest] url: $url, body: $requestContent")
        logger.info("[executePostRequest]POST url: $url, body: ${logWithLengthLimit(requestContent)}")
        val ccPostResponse = OkhttpUtils.doPost(url, requestContent, headers)
        val ccPostRes = ccPostResponse.body?.string()
        logger.info("[executePostRequest]POST res: ${logWithLengthLimit(ccPostRes ?: "")}")
        return ccPostRes
    }

    private fun getNodeIpToCmdbDataMap(responseBody: String?): Map<String, CmdbDataIns> {
        val cmdbResp = jacksonObjectMapper().readValue<CmdbResp>(responseBody!!)
        if (logger.isDebugEnabled) logger.debug("[getNodeIpToCmdbDataMap] cmdbResp: $cmdbResp")
        val cmdbData = cmdbResp.data.data
        val cmdbIpToCmdbDataMap: Map<String, CmdbDataIns> = cmdbData?.associateBy { it.SvrIp!! } ?: mapOf()
        return cmdbIpToCmdbDataMap
    }

    private fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }
}