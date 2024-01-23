package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.job.ccreq.CCAddHostReq
import com.tencent.devops.environment.pojo.job.ccreq.CCDeleteHostReq
import com.tencent.devops.environment.pojo.job.ccreq.CCFindHostBizRelationsReq
import com.tencent.devops.environment.pojo.job.ccreq.CCHostPropertyFilter
import com.tencent.devops.environment.pojo.job.ccreq.CCListHostWithoutBizReq
import com.tencent.devops.environment.pojo.job.ccreq.CCPage
import com.tencent.devops.environment.pojo.job.ccreq.CCRules
import com.tencent.devops.environment.pojo.job.ccres.CCBkHost
import com.tencent.devops.environment.pojo.job.ccres.QueryCCListHostWithoutBizData
import com.tencent.devops.environment.pojo.job.ccres.CCResp
import com.tencent.devops.environment.pojo.job.ccres.HostBizRelation
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class QueryFromCCService : IQueryOperatorService {
    @Value("\${environment.apigw.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${environment.apigw.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${environment.cc.bkSupplierAccount:}")
    private val bkSupplierAccount = ""

    @Value("\${environment.cc.bkccQueryBaseUrl:}")
    private val bkccQueryBaseUrl = ""

    @Value("\${environment.cc.bkccListHostWithoutBizPath:#{\"/list_hosts_without_biz\"}}")
    private val bkccListHostWithoutBizPath = ""

    @Value("\${environment.cc.bkccFindHostBizRelationsPath:#{\"/find_host_biz_relations\"}}")
    private val bkccFindHostBizRelationsPath = ""

    @Value("\${environment.cc.bkccExecuteBaseUrl:}")
    private val bkccExecuteBaseUrl = ""

    @Value("\${environment.cc.bkccAddHostToCiBizPath:#{\"/sync/cmdb/add_host_to_ci_biz\"}}")
    private val bkccAddHostToCiBizPath = ""

    @Value("\${environment.cc.bkccDeleteHostFromCiBizPath:#{\"/delete/cmdb/delete_host_from_ci_biz\"}}")
    private val bkccDeleteHostFromCiBizPath = ""

    companion object {
        private val logger = LoggerFactory.getLogger(QueryFromCCService::class.java)
        const val DEFAULT_PAGE_LIMIT = 500
        const val DEFAULT_PAGE_START = 0
        const val FIELD_BK_HOST_ID = "bk_host_id"
        const val FIELD_BK_CLOUD_ID = "bk_cloud_id"
        const val FIELD_BK_HOST_INNERIP = "bk_host_innerip"
        const val FIELD_OPERATOR = "operator"
        const val FIELD_BAK_OPERATOR = "bk_bak_operator"
        const val AND_CONDITATION = "AND"
        const val IN_OPERATION = "in"
    }

    /**
     *  判断：用户or节点导入人 是机器的主备负责人（用户：函数中形参userId；节点导入人：T_NODE表中的createdUser）
     *  core中实现：从CC中 用对应T_NODE表中记录的host_id查询机器的主备负责人
     */
    override fun isOperatorOrBakOperator(userId: String, nodeRecords: Set<TNodeRecord>) {
        val nodeIpList: List<String> = nodeRecords.map { it.nodeIp } // 所有host对应的ip
        val nodeIpToNodeMap = nodeRecords.associateBy { it.nodeIp } // 所有host的：ip - 记录 映射
        val nodeHostIdList: List<Long> = nodeRecords.map { it.hostId } // 所有host对应的id

        val ccResp = queryCCListHostWithoutBizByInRules(
            listOf(FIELD_BK_HOST_ID, FIELD_BK_CLOUD_ID, FIELD_BK_HOST_INNERIP, FIELD_OPERATOR, FIELD_BAK_OPERATOR),
            nodeHostIdList, FIELD_BK_HOST_ID
        )
        if (null != ccResp.data) {
            val ccData = ccResp.data.info
            val ccIpToNodeMap = ccData.associateBy { it.bkHostInnerip }
            val invalidIpList = nodeIpList.filter {
                val isOperator = userId == ccIpToNodeMap[it]?.operator ||
                    nodeIpToNodeMap[it]?.createdUser == ccIpToNodeMap[it]?.operator
                val isBakOpertor = ccIpToNodeMap[it]?.bkBakOperator?.split(",")?.contains(userId)!! ||
                    ccIpToNodeMap[it]?.bkBakOperator?.split(",")?.contains(nodeIpToNodeMap[it]?.createdUser)!!
                !isOperator && !isBakOpertor
            }
            if (logger.isDebugEnabled) logger.debug("[isOperatorOrBakOperator] invalidIpList: $invalidIpList")

            if (invalidIpList.isNotEmpty()) {
                throw ErrorCodeException(
                    errorCode = EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL_USER,
                    params = arrayOf(invalidIpList.joinToString(","))
                )
            }
        }
    }

    fun <T> queryCCListHostWithoutBizByInRules(
        fields: List<String>,
        inValueList: T,
        field: String
    ): CCResp<QueryCCListHostWithoutBizData> {
        val ccListHostWithoutBizReq = CCListHostWithoutBizReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
            bkSupplierAccount = bkSupplierAccount,
            page = CCPage(DEFAULT_PAGE_START, DEFAULT_PAGE_LIMIT),
            fields = fields,
            hostPropertyFilter = CCHostPropertyFilter(
                condition = AND_CONDITATION,
                rules = listOf(
                    CCRules(
                        field = field,
                        operator = IN_OPERATION,
                        value = inValueList
                    )
                )
            )
        )
        if (logger.isDebugEnabled) logger.debug("[queryCCListHostWithoutBizByInRules] req: $ccListHostWithoutBizReq")
        if (logger.isDebugEnabled) logger.debug(
            "[queryCCListHostWithoutBizByInRules] url: ${bkccQueryBaseUrl + bkccListHostWithoutBizPath}"
        )
        val resBody = executePostRequest(
            getcommonHeaders(), bkccQueryBaseUrl + bkccListHostWithoutBizPath, ccListHostWithoutBizReq
        )
        if (logger.isDebugEnabled) logger.debug("[queryCCListHostWithoutBizByInRules] resBody:$resBody")
        return jacksonObjectMapper().readValue(resBody!!)
    }

    fun addHostToCiBiz(svrIdList: List<Long>): CCResp<CCBkHost> {
        val ccAddHostReq = CCAddHostReq(svrIdList)
        val resBody = executePostRequest(
            getAuthHeaders(), bkccExecuteBaseUrl + bkccAddHostToCiBizPath, ccAddHostReq
        )
        if (logger.isDebugEnabled) logger.debug("[addHostToCiBiz]resBody:$resBody")
        val deserializedResBody = jacksonObjectMapper().readValue<CCResp<CCBkHost>>(resBody!!)
        if (logger.isDebugEnabled) logger.debug("[addHostToCiBiz]deserializedResBody:$deserializedResBody")
        return deserializedResBody
    }

    fun deleteHostFromCiBiz(hostIdList: Set<Long>): CCResp<Nothing> {
        val ccDeleteHostReq = CCDeleteHostReq(hostIdList)
        val resBody = executeDeleteRequest(
            getAuthHeaders(), bkccExecuteBaseUrl + bkccDeleteHostFromCiBizPath, ccDeleteHostReq
        )
        if (logger.isDebugEnabled) logger.debug("[deleteHostFromCiBiz]resBody:$resBody")
        return jacksonObjectMapper().readValue(resBody!!)
    }

    fun queryCCFindHostBizRelations(hostIdList: List<Long>): CCResp<List<HostBizRelation>> {
        val ccFindHostBizRelationsReq = CCFindHostBizRelationsReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
            bkHostId = hostIdList
        )
        val resBody = executePostRequest(
            getcommonHeaders(), bkccQueryBaseUrl + bkccFindHostBizRelationsPath, ccFindHostBizRelationsReq
        )
        return jacksonObjectMapper().readValue(resBody!!)
    }

    private fun getcommonHeaders(): Map<String, String> {
        return mapOf("accept" to "*/*", "Content-Type" to "application/json")
    }

    private fun getAuthHeaders(): Map<String, String> {
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"bk_username\": \"$AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE\"}"
        return mapOf(
            "accept" to "*/*",
            "Content-Type" to "application/json",
            "X-Bkapi-Authorization" to bkAuthorization
        )
    }

    private fun <T> executePostRequest(headers: Map<String, String>, url: String, req: T): String? {
        if (logger.isDebugEnabled) logger.debug("[executePostRequest] url: $url")
        val requestContent = jacksonObjectMapper().writeValueAsString(req)
        if (logger.isDebugEnabled) logger.debug("[executePostRequest] requestContent: $requestContent")
        val ccPostRes = OkhttpUtils.doPost(url, requestContent, headers)
        if (logger.isDebugEnabled) logger.debug("[executePostRequest] ccPostRes: $ccPostRes")
        val ccPostResBody = ccPostRes.body?.string()
        if (logger.isDebugEnabled) logger.debug("[executePostRequest] ccPostRes.body.string(): $ccPostResBody")
        return ccPostResBody
    }

    private fun <T> executeDeleteRequest(headers: Map<String, String>, url: String, req: T): String? {
        val requestContent = jacksonObjectMapper().writeValueAsString(req)
        val requestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), requestContent)
        val deleteReq: Request = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .delete(requestBody)
            .build()
        val ccDeleteRes = OkhttpUtils.doHttp(deleteReq)
        return ccDeleteRes.body?.string()
    }
}