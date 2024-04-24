package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.constant.COLUMN_SEVER_LAN_IP
import com.tencent.devops.environment.constant.COLUMN_SFW_NAME
import com.tencent.devops.environment.constant.COLUMN_SVR_BAK_OPERATOR
import com.tencent.devops.environment.constant.COLUMN_SVR_IP
import com.tencent.devops.environment.constant.COLUMN_SVR_NAME
import com.tencent.devops.environment.constant.COLUMN_SVR_OPERATOR
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
import java.net.SocketTimeoutException
import javax.ws.rs.core.Response

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
        val cmdbIpToCmdbDataMap = queryCmdbInfoFromIp(
            nodeIpList.toSet(),
            COLUMN_SVR_BAK_OPERATOR, COLUMN_SVR_OPERATOR, COLUMN_SVR_IP,
            COLUMN_SVR_NAME, COLUMN_SFW_NAME, COLUMN_SEVER_LAN_IP
        )
        val ipNotInCmdb = mutableListOf<String>()
        val unauthorisedIpList = nodeIpList.filter {
            if (null != cmdbIpToCmdbDataMap?.get(it)) {
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

    fun queryCmdbInfoFromIp(nodeIpList: Set<String>, vararg reqColumn: String): Map<String, CmdbDataIns>? {
        val cmdbGetQueryInfoReq = CmdbGetQueryInfoReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            operator = DEFAULT_SYTEM_USER,
            reqColumn = reqColumn.toList(),
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

        var ccPostRes: String? = null
        try {
            ccPostRes = OkhttpUtils.doPost(url, requestContent, headers).body?.string()
            logger.info("POST res: ${logWithLengthLimit(ccPostRes ?: "")}")
        } catch (timeoutError: SocketTimeoutException) {
            logger.error("Query CMDB interface time out. Error: $timeoutError")
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_CMDB_INTERFACE_TIME_OUT)
        } catch (error: Exception) {
            logger.error("Query CMDB interface error. Error: $error")
        }
        return ccPostRes
    }

    private fun getNodeIpToCmdbDataMap(responseBody: String?): Map<String, CmdbDataIns> {
        val cmdbData: List<CmdbDataIns>?
        try {
            cmdbData = jacksonObjectMapper().readValue<CmdbResp>(responseBody!!).data.data
        } catch (e: Exception) {
            throw CustomException(
                Response.Status.INTERNAL_SERVER_ERROR,
                "CMDB api response error."
            )
        }
        return cmdbData?.associateBy { it.SvrIp!! } ?: mapOf()
    }

    private fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }
}
