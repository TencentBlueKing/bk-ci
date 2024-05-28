package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.Constants.COLUMN_SERVER_ID
import com.tencent.devops.environment.constant.Constants.COLUMN_SEVER_LAN_IP
import com.tencent.devops.environment.constant.Constants.COLUMN_SFW_NAME
import com.tencent.devops.environment.constant.Constants.COLUMN_SVR_BAK_OPERATOR
import com.tencent.devops.environment.constant.Constants.COLUMN_SVR_IP
import com.tencent.devops.environment.constant.Constants.COLUMN_SVR_NAME
import com.tencent.devops.environment.constant.Constants.COLUMN_SVR_OPERATOR
import com.tencent.devops.environment.constant.DEFAULT_SYTEM_USER
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_CMDB_INTERFACE_TIME_OUT
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_CMDB_RESPONSE
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL
import com.tencent.devops.environment.constant.T_NODE_CREATED_USER
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_SERVER_ID
import com.tencent.devops.environment.pojo.job.cmdbreq.CmdbGetQueryInfoReq
import com.tencent.devops.environment.pojo.job.cmdbreq.CmdbKeyValues
import com.tencent.devops.environment.pojo.job.cmdbreq.CmdbPagingInfo
import com.tencent.devops.environment.pojo.job.cmdbres.CmdbDataIns
import com.tencent.devops.environment.pojo.job.cmdbres.CmdbResp
import org.jooq.Record6
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

        private const val CONNECT_TIMEOUT = 5L // 三个超时时间单位：均为秒
        private const val READ_TIMEOUT = 15L
        private const val WRITE_TIMEOUT = 15L

        private val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    /*
     *  判断：用户or节点导入人 是机器的主备负责人（用户：函数中形参userId；节点导入人：T_NODE表中的createdUser）
     *  ext中实现：从cmdb中 用serverId查询机器的主备负责人（提示用户时，还是用ip标识机器）
     */
    fun isOperatorOrBakOperator(userId: String, nodeRecords: Set<Record6<Long, String, Long, Long, String, Long>>) {
        val nodeServerIdSet = nodeRecords.mapNotNull { it[T_NODE_SERVER_ID] as? Long }.toSet() // 所有host对应的serverId
        val nodeServerIdToNodeMap = nodeRecords.associateBy {
            it[T_NODE_SERVER_ID] as? Long
        } // 所有host的：serverId - 记录 映射
        val cmdbInfoList = queryCmdbInfo(
            keyValues = CmdbKeyValues(
                serverIdStrList = nodeServerIdSet.joinToString(separator = ";")
            ),
            COLUMN_SVR_BAK_OPERATOR, COLUMN_SVR_OPERATOR, COLUMN_SVR_IP, COLUMN_SEVER_LAN_IP,
            COLUMN_SVR_NAME, COLUMN_SFW_NAME, COLUMN_SERVER_ID
        )
        val cmdbServerIdToCmdbDataMap = cmdbInfoList?.associateBy { it.serverId!! } ?: mapOf()
        // 没有serverId的节点记录，也认为该节点不在CMDB中
        val ipNotInCmdb = nodeRecords.filterNot { nodeServerIdSet.contains(it[T_NODE_SERVER_ID] as? Long) }.map {
            it[T_NODE_NODE_IP] as String
        }.toMutableList()
        val unauthorisedServerIdList = nodeServerIdSet.filter {
            if (null != cmdbServerIdToCmdbDataMap[it]) {
                val createdUser = nodeServerIdToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String
                val isOperator =
                    userId == cmdbServerIdToCmdbDataMap[it]?.svrOperator ||
                        createdUser == cmdbServerIdToCmdbDataMap[it]?.svrOperator
                val isBakOperator =
                    cmdbServerIdToCmdbDataMap[it]?.svrBakOperator?.split(";")?.contains(userId) ?: false ||
                        cmdbServerIdToCmdbDataMap[it]?.svrBakOperator?.split(";")?.contains(createdUser) ?: false
                !isOperator && !isBakOperator
            } else { // 机器不在CMDB中
                ipNotInCmdb.add(nodeServerIdToNodeMap[it]?.get(T_NODE_NODE_IP) as String)
                false
            }
        }
        if (unauthorisedServerIdList.isNotEmpty() || ipNotInCmdb.isNotEmpty()) {
            val unauthorisedIpToNodeMap = unauthorisedServerIdList.map {
                nodeServerIdToNodeMap[it]
            }.associateBy { it?.get(T_NODE_NODE_IP) as String }
            val unauthorisedIpList = unauthorisedIpToNodeMap.keys
            logger.warn(
                "[isOperatorOrBakOperator] unauthorisedIpList: ${unauthorisedIpList.joinToString()}, " +
                    "notInCmdbIpList: ${ipNotInCmdb.joinToString()}"
            )
            throw ErrorCodeException(
                errorCode = ERROR_NODE_IP_ILLEGAL,
                params = arrayOf(
                    ipNotInCmdb.joinToString(","),
                    unauthorisedIpList.joinToString(","),
                    userId,
                    unauthorisedIpList.joinToString(", ") {
                        it + " - " + unauthorisedIpToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String
                    }
                )
            )
        }
    }

    fun queryCmdbInfo(keyValues: CmdbKeyValues, vararg reqColumn: String): List<CmdbDataIns>? {
        val cmdbGetQueryInfoReq = CmdbGetQueryInfoReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            operator = DEFAULT_SYTEM_USER,
            reqColumn = reqColumn.toList(),
            keyValues = keyValues,
            pagingInfo = CmdbPagingInfo(DEFAULT_START_INDEX, PAGE_SIZE, DEFAULT_RETURN_TOTAL_ROWS)
        )
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        val responseBody = executePostRequest(
            headers, cmdbGetQueryInfoBaseUrl + cmdbGetQueryInfoPath, cmdbGetQueryInfoReq
        )
        return getNodeCmdbData(responseBody)
    }

    private fun <T : Any> executePostRequest(headers: Map<String, String>, url: String, req: T): String? {
        val requestContent = mapper.writeValueAsString(req)
        logger.info("POST url: $url, req: ${logWithLengthLimit(JsonUtil.skipLogFields(req) ?: "")}")

        val ccPostRes: String?
        try {
            ccPostRes = OkhttpUtils.doCustomTimeoutPost(
                connectTimeout = CONNECT_TIMEOUT,
                readTimeout = READ_TIMEOUT,
                writeTimeout = WRITE_TIMEOUT,
                url = url,
                jsonParam = requestContent,
                headers = headers
            ).body?.string()
            logger.info("POST res: ${logWithLengthLimit(ccPostRes ?: "")}")
        } catch (timeoutError: SocketTimeoutException) {
            logger.error("Query CMDB interface time out. Error:", timeoutError)
            throw ErrorCodeException(
                errorCode = ERROR_CMDB_INTERFACE_TIME_OUT,
                defaultMessage = I18nUtil.getCodeLanMessage(ERROR_CMDB_INTERFACE_TIME_OUT)
            )
        } catch (error: Exception) {
            logger.error("Query CMDB interface error. Error:", error)
            throw ErrorCodeException(
                errorCode = ERROR_CMDB_RESPONSE,
                defaultMessage = I18nUtil.getCodeLanMessage(ERROR_CMDB_RESPONSE)
            )
        }
        return ccPostRes
    }

    private fun getNodeCmdbData(responseBody: String?): List<CmdbDataIns>? {
        val cmdbData: List<CmdbDataIns>?
        try {
            cmdbData = mapper.readValue<CmdbResp>(responseBody!!).data.data
        } catch (e: Exception) {
            throw CustomException(
                Response.Status.INTERNAL_SERVER_ERROR,
                "CMDB api response error."
            )
        }
        return cmdbData
    }

    private fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }
}
