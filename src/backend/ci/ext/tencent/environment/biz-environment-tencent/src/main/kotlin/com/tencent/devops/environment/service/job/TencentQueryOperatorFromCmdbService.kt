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
class TencentQueryOperatorFromCmdbService : QueryOperatorService {
    @Value("\${job.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${job.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.cmdbGetQueryInfoReqUrl:}")
    private val cmdbGetQueryInfoReqUrl = ""

    companion object {
        private val logger = LoggerFactory.getLogger(TencentQueryOperatorFromCmdbService::class.java)
        const val PAGE_SIZE = 1000
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
            reqColumn = listOf("SvrBakOperator", "SvrOperator", "SvrIp", "SvrName", "SfwName", "serverLanIP"),
            keyValues = CmdbKeyValues(
                svrIp = nodeIpList.joinToString(separator = ";")
            ),
            pagingInfo = CmdbPagingInfo(0, PAGE_SIZE, 1)
        )
        val requestContent = jacksonObjectMapper().writeValueAsString(cmdbGetQueryInfoReq)
        if (logger.isDebugEnabled) logger.debug("[isOperatorOrBakOperator] requestContent: $requestContent")
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        val cmdbGetQueryInfoRes = OkhttpUtils.doPost(cmdbGetQueryInfoReqUrl, requestContent, headers)
        val responseBody = cmdbGetQueryInfoRes.body?.string()
        if (logger.isDebugEnabled) logger.debug("[isOperatorOrBakOperator] responseBody: $responseBody")
        val cmdbResp = jacksonObjectMapper().readValue<CmdbResp>(responseBody!!)
        if (logger.isDebugEnabled) logger.debug("[isOperatorOrBakOperator] cmdbResp: $cmdbResp")
        val cmdbData = cmdbResp.data.data
        val cmdbIpToCmdbDataMap: Map<String, CmdbDataIns> = cmdbData?.associateBy { it.SvrIp } ?: mapOf() // ip - 记录 映射

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
}