package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.ccreq.CCListHostWithoutBizReq
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("BkCCService")
class BkCCService {
    companion object {
        private val logger = LoggerFactory.getLogger(BkCCService::class.java)
    }

    fun getOperatorAndBakFromBkHostId(ccListHostWithoutBizReq: CCListHostWithoutBizReq) {

    }
}