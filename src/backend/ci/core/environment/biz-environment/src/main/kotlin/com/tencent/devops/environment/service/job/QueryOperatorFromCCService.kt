package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.job.ccreq.CCHostPropertyFilter
import com.tencent.devops.environment.pojo.job.ccreq.CCListHostWithoutBizReq
import com.tencent.devops.environment.pojo.job.ccreq.CCPage
import com.tencent.devops.environment.pojo.job.ccreq.CCRules
import com.tencent.devops.environment.pojo.job.ccres.CCResp
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class QueryOperatorFromCCService : QueryOperatorService {
    @Value("\${job.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${job.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.bkSupplierAccount:}")
    private val bkSupplierAccount = ""

    @Value("\${job.bkccListHostWithoutBizReqUrl:}")
    private val bkccListHostWithoutBizReqUrl = ""

    companion object {
        private val logger = LoggerFactory.getLogger(QueryOperatorFromCCService::class.java)
        const val PAGE_LIMIT = 3
    }

    /*
     *  判断：用户or节点导入人 是机器的主备负责人（用户：函数中形参userId；节点导入人：T_NODE表中的createdUser）
     *  core中实现：从CC中 用对应T_NODE表中记录的host_id查询机器的主备负责人
     */
    override fun isOperatorOrBakOperator(userId: String, nodeRecords: Set<TNodeRecord>) {
        val nodeIpList: List<String> = nodeRecords.map { it.nodeIp } // 所有host对应的ip
        val nodeIpToNodeMap = nodeRecords.associateBy { it.nodeIp } // 所有host的：ip - 记录 映射
        val nodeHostIdList: List<Long> = nodeRecords.map { it.hostId } // 所有host对应的id

        val ccListHostWithoutBizReq = CCListHostWithoutBizReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
            bkSupplierAccount = bkSupplierAccount,
            page = CCPage(0, PAGE_LIMIT),
            fields = listOf("bk_host_id", "bk_cloud_id", "bk_host_innerip", "operator", "bk_bak_operator"),
            hostPropertyFilter = CCHostPropertyFilter(
                condition = "OR",
                rules = nodeHostIdList.map {
                    CCRules(
                        field = "bk_host_id",
                        operator = "equal",
                        value = it
                    )
                }
            )
        )
        val requestContent = jacksonObjectMapper().writeValueAsString(ccListHostWithoutBizReq)
        if (logger.isDebugEnabled) logger.debug("[isOperatorOrBakOperator] requestContent: $requestContent")
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        val ccListHostWithoutBizRes = OkhttpUtils.doPost(bkccListHostWithoutBizReqUrl, requestContent, headers)
        val responseBody = ccListHostWithoutBizRes.body?.string()
        val ccResp = jacksonObjectMapper().readValue<CCResp>(responseBody!!)
        if (logger.isDebugEnabled) logger.debug("[isOperatorOrBakOperator] ccResp: $ccResp")
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