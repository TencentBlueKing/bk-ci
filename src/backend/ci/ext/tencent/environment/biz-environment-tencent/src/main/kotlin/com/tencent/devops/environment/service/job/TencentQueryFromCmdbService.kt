package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.constant.DEFAULT_SYTEM_USER
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.T_NODE_CREATED_USER
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.pojo.job.cmdbreq.CmdbGetQueryInfoReq
import com.tencent.devops.environment.pojo.job.cmdbreq.CmdbKeyValues
import com.tencent.devops.environment.pojo.job.cmdbreq.CmdbPagingInfo
import com.tencent.devops.environment.pojo.job.cmdbres.CmdbDataIns
import com.tencent.devops.environment.pojo.job.cmdbres.CmdbResp
import org.jooq.Record5
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TencentQueryFromCmdbService {
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
    fun isOperatorOrBakOperator(userId: String, nodeRecords: Set<Record5<Long, String, Long, Long, String>>) {
        val nodeIpList: List<String> = nodeRecords.mapNotNull { it[T_NODE_NODE_IP] as? String } // 所有host对应的ip
        val nodeIpToNodeMap = nodeRecords.associateBy { it[T_NODE_NODE_IP] as? String } // 所有host的：ip - 记录 映射
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
        val cmdbIpToCmdbDataMap = getNodeIpToCmdbDataMap(responseBody)

        val ipNotInCmdb = mutableListOf<String>()
        val unauthorisedIpList = nodeIpList.filter {
            if (null != cmdbIpToCmdbDataMap[it]) {
                val isOperator = userId == cmdbIpToCmdbDataMap[it]?.SvrOperator ||
                    nodeIpToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String == cmdbIpToCmdbDataMap[it]?.SvrOperator
                val isBakOpertor = cmdbIpToCmdbDataMap[it]?.SvrBakOperator?.split(";")?.contains(userId) ?: false ||
                    cmdbIpToCmdbDataMap[it]?.SvrBakOperator?.split(";")
                        ?.contains(nodeIpToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String) ?: false
                !isOperator && !isBakOpertor
            } else { // 机器不在CMDB中
                ipNotInCmdb.add(it)
                false
            }
        }
        if (unauthorisedIpList.isNotEmpty() || ipNotInCmdb.isNotEmpty()) {
            logger.warn(
                "[isOperatorOrBakOperator] unauthorisedIpList: ${unauthorisedIpList.joinToString()}, " +
                    "notInCmdbIpList: ${ipNotInCmdb.joinToString()}"
            )
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL,
                params = arrayOf(
                    ipNotInCmdb.joinToString(","),
                    unauthorisedIpList.joinToString(","),
                    userId,
                    unauthorisedIpList.joinToString(", ") {
                        it + " - " + nodeIpToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String
                    }
                )
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
        val responseBody = executePostRequest(
            headers, cmdbGetQueryInfoBaseUrl + cmdbGetQueryInfoPath, cmdbGetQueryInfoReq
        )
        return getNodeIpToCmdbDataMap(responseBody)
    }

    private fun <T> executePostRequest(headers: Map<String, String>, url: String, req: T): String? {
        val requestContent = jacksonObjectMapper().writeValueAsString(req)
        logger.info("POST url: $url, req: ${logWithLengthLimit(requestContent)}")

        val ccPostRes = OkhttpUtils.doPost(url, requestContent, headers).body?.string()
        logger.info("POST res: ${logWithLengthLimit(ccPostRes ?: "")}")
        return ccPostRes
    }

    private fun getNodeIpToCmdbDataMap(responseBody: String?): Map<String, CmdbDataIns> {
        val cmdbData = jacksonObjectMapper().readValue<CmdbResp>(responseBody!!).data.data
        return cmdbData?.associateBy { it.SvrIp!! } ?: mapOf()
    }

    private fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }
}