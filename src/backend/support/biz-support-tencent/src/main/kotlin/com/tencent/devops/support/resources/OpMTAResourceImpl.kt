package com.tencent.devops.support.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.OpMTAResource
import com.tencent.devops.support.model.mta.h5.base.IdxResult
import com.tencent.devops.support.model.mta.h5.message.CoreDataMessage
import com.tencent.devops.support.services.MTAH5Service
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpMTAResourceImpl @Autowired constructor(private val mtaH5Service: MTAH5Service) : OpMTAResource {

    private val logger = LoggerFactory.getLogger(OpMTAResourceImpl::class.java)

    override fun getCoreData(coreDataMessage: CoreDataMessage): Result<Map<String, IdxResult>?> {
        var coreDataResponse = mtaH5Service.getCoreData(coreDataMessage)
        if (coreDataResponse != null) {
            return Result(
                    status = coreDataResponse.code,
                    message = coreDataResponse.info,
                    data = coreDataResponse.data
                    )
        } else {
            return Result(data = null)
        }
    }
}