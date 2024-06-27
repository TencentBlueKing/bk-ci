package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.Constants.NEW_COLUMN_BAK_MAINTAINER
import com.tencent.devops.environment.constant.Constants.NEW_COLUMN_HOST_NAME
import com.tencent.devops.environment.constant.Constants.NEW_COLUMN_INNER_SERVER_IPV4
import com.tencent.devops.environment.constant.Constants.NEW_COLUMN_MAINTAINER
import com.tencent.devops.environment.constant.Constants.NEW_COLUMN_OS_NAME
import com.tencent.devops.environment.constant.Constants.NEW_COLUMN_SERVER_ID
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
import com.tencent.devops.environment.pojo.job.cmdbreq.NewCmdbCondition
import com.tencent.devops.environment.pojo.job.cmdbreq.NewCmdbQueryInfoReq
import com.tencent.devops.environment.pojo.job.cmdbreq.NewCmdbConditionValue
import com.tencent.devops.environment.pojo.job.cmdbres.CmdbDataIns
import com.tencent.devops.environment.pojo.job.cmdbres.CmdbResp
import com.tencent.devops.environment.pojo.job.cmdbres.NewCmdbData
import com.tencent.devops.environment.pojo.job.cmdbres.NewCmdbDataIns
import com.tencent.devops.environment.pojo.job.cmdbres.NewCmdbResp
import com.tencent.devops.environment.service.CmdbNodeService
import org.apache.commons.codec.digest.DigestUtils
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

    @Value("\${environment.newCmdb.newCmdbBaseUrl:}")
    private val newCmdbBaseUrl = ""

    @Value("\${job.newCmdbQueryAllServerPath:#{\"/cmdb-service-federal-query/queryAllServerByBaseCondition\"}}")
    private val newCmdbQueryAllServerByBaseConditionPath = ""

    @Value("\${job.newCmdbQueryAllServerPath:#{\"/cmdb-service-federal-query/queryAllServerByBusiness\"}}")
    private val newCmdbQueryAllServerByBusinessPath = ""

    @Value("\${environment.newCmdb.appId:}")
    private val appId = ""

    @Value("\${environment.newCmdb.appKey:}")
    private val appKey = ""

    companion object {
        private val logger = LoggerFactory.getLogger(TencentQueryFromCmdbService::class.java)
        private const val LOG_OUTPUT_MAX_LENGTH = 4000

        const val PAGE_SIZE = 1000
        const val DEFAULT_START_INDEX = 0
        const val DEFAULT_RETURN_TOTAL_ROWS = 1

        const val DEFAULT_SIZE = 200
        const val DEFAULT_SCROLL_ID = "0"

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
        val cmdbInfoList = queryNewCmdbInfoByBaseCondition(
            newCmdbCondition = NewCmdbCondition(
                serverId = NewCmdbConditionValue(
                    operator = CmdbNodeService.CMDB_QUERY_OPERATION_IN,
                    value = nodeServerIdSet.toList()
                )
            ),
            NEW_COLUMN_MAINTAINER, NEW_COLUMN_BAK_MAINTAINER, NEW_COLUMN_INNER_SERVER_IPV4,
            NEW_COLUMN_HOST_NAME, NEW_COLUMN_OS_NAME, NEW_COLUMN_SERVER_ID
        )
        val cmdbServerIdToCmdbDataMap = cmdbInfoList?.associateBy { it.serverId } ?: mapOf()
        // 没有serverId的节点记录，也认为该节点不在CMDB中
        val ipNotInCmdb = nodeRecords.filterNot { nodeServerIdSet.contains(it[T_NODE_SERVER_ID] as? Long) }.map {
            it[T_NODE_NODE_IP] as String
        }.toMutableList()
        val unauthorisedServerIdList = nodeServerIdSet.filter {
            if (null != cmdbServerIdToCmdbDataMap[it]) {
                val createdUser = nodeServerIdToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String
                val isOperator =
                    userId == cmdbServerIdToCmdbDataMap[it]?.maintainer ||
                        createdUser == cmdbServerIdToCmdbDataMap[it]?.maintainer
                val isBakOperator =
                    cmdbServerIdToCmdbDataMap[it]?.maintainerBak?.split(";")?.contains(userId) ?: false ||
                        cmdbServerIdToCmdbDataMap[it]?.maintainerBak?.split(";")?.contains(createdUser) ?: false
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

    fun <T> queryNewCmdbInfoByBaseCondition(
        newCmdbCondition: NewCmdbCondition<T>,
        vararg newReqColumn: String
    ): List<NewCmdbDataIns>? {
        return queryNewCmdbInfo(
            newCmdbQueryAllServerByBaseConditionPath, newCmdbCondition, DEFAULT_SIZE, DEFAULT_SCROLL_ID, *newReqColumn
        ).list
    }

    fun <T> queryNewCmdbInfoByBusiness(
        newCmdbCondition: NewCmdbCondition<T>,
        size: Int,
        scrollId: String,
        vararg newReqColumn: String
    ): NewCmdbData? {
        return queryNewCmdbInfo(newCmdbQueryAllServerByBusinessPath, newCmdbCondition, size, scrollId, *newReqColumn)
    }

    private fun <T> queryNewCmdbInfo(
        path: String,
        newCmdbCondition: NewCmdbCondition<T>,
        size: Int,
        scrollId: String,
        vararg newReqColumn: String
    ): NewCmdbData {
        val newCmdbQueryInfoReq = NewCmdbQueryInfoReq(
            resultColumn = newReqColumn.toList(),
            condition = newCmdbCondition,
            size = size,
            scrollId = scrollId
        )
        val responseBody = executePostRequest(
            getQueryNewCmdbInfoHeaders(), newCmdbBaseUrl + path, newCmdbQueryInfoReq
        )
        return getNodeNewCmdbData(responseBody)
    }

    private fun getQueryNewCmdbInfoHeaders(): MutableMap<String, String> {
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        // 接入系统英文名称
        val appId = appId
        // 接入完毕后自动生成的appKey
        val appKey = appKey
        // 获取当前时间戳
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        // 使用标准算法库生成签名
        val signature: String = DigestUtils.sha256Hex(timestamp + appKey)
        // 添加请求头
        headers["x-timestamp"] = timestamp
        headers["x-signature"] = signature
        headers["x-app-id"] = appId
        return headers
    }

    private fun <T : Any> executePostRequest(headers: Map<String, String>, url: String, req: T): String? {
        val requestContent = mapper.writeValueAsString(req)
        logger.info("POST url: $url, req: ${logWithLengthLimit(JsonUtil.skipLogFields(req) ?: "")}")

        val cmdbPostRes: String?
        try {
            cmdbPostRes = OkhttpUtils.doCustomTimeoutPost(
                connectTimeout = CONNECT_TIMEOUT,
                readTimeout = READ_TIMEOUT,
                writeTimeout = WRITE_TIMEOUT,
                url = url,
                jsonParam = requestContent,
                headers = headers
            ).body?.string()
            logger.info("POST res: ${logWithLengthLimit(cmdbPostRes ?: "")}")
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
        return cmdbPostRes
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

    private fun getNodeNewCmdbData(responseBody: String?): NewCmdbData {
        val cmdbData: NewCmdbData?
        try {
            cmdbData = mapper.readValue<NewCmdbResp>(responseBody!!).data
        } catch (e: Exception) {
            logger.error("[getNodeNewCmdbData]readValue error: ", e)
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
