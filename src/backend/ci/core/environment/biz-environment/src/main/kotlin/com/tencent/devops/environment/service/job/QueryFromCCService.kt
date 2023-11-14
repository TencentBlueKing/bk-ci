package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.job.ccreq.CCAddHostReq
import com.tencent.devops.environment.pojo.job.ccreq.CCHostPropertyFilter
import com.tencent.devops.environment.pojo.job.ccreq.CCListHostWithoutBizReq
import com.tencent.devops.environment.pojo.job.ccreq.CCPage
import com.tencent.devops.environment.pojo.job.ccreq.CCRules
import com.tencent.devops.environment.pojo.job.ccres.CCAndHostRes
import com.tencent.devops.environment.pojo.job.ccres.CCResp
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class QueryFromCCService : QueryOperatorService {
    @Value("\${job.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${job.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.bkSupplierAccount:}")
    private val bkSupplierAccount = ""

    @Value("\${job.bkccListHostWithoutBizReqUrl:}")
    private val bkccListHostWithoutBizReqUrl = ""

    @Value("\${job.bkccAddHostToCiBizUrl:}")
    private val bkccAddHostToCiBizUrl = ""

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

    /*
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

    fun <T> queryCCListHostWithoutBizByInRules(fields: List<String>, inValueList: T, field: String): CCResp {
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
        val requestContent = jacksonObjectMapper().writeValueAsString(ccListHostWithoutBizReq)
        if (logger.isDebugEnabled) logger.debug("[queryCCListHostWithoutBizByBkHostId] requestContent: $requestContent")
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        val ccListHostWithoutBizRes = OkhttpUtils.doPost(bkccListHostWithoutBizReqUrl, requestContent, headers)
        val responseBody = ccListHostWithoutBizRes.body?.string()
        val ccResp = jacksonObjectMapper().readValue<CCResp>(responseBody!!)
        if (logger.isDebugEnabled) logger.debug("[queryCCListHostWithoutBizByBkHostId] ccResp: $ccResp")
        return ccResp
    }

    fun addHostToCiBiz(svrIds: List<Long>): CCAndHostRes {
        val ccAddHostReq = CCAddHostReq(svrIds)
        val requestContent = jacksonObjectMapper().writeValueAsString(ccAddHostReq)
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        val ccAddHostToCiBizRes = OkhttpUtils.doPost(bkccAddHostToCiBizUrl, requestContent, headers)
        val responseBody = ccAddHostToCiBizRes.body?.string()
        val ccResp = jacksonObjectMapper().readValue<CCAndHostRes>(responseBody!!)
        if (logger.isDebugEnabled) logger.debug("[addHostToCiBiz] ccResp: $ccResp")
        return ccResp
    }
}