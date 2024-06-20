package com.tencent.devops.metrics.service.impl

import com.tencent.devops.metrics.dao.DispatchJobMetricsDao
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.MaxJobConcurrencyVO
import com.tencent.devops.metrics.service.DispatchJobMetricsService
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class DispatchJobMetricsServiceImpl constructor(
    val dslContext: DSLContext,
    val dispatchJobMetricsDao: DispatchJobMetricsDao
) : DispatchJobMetricsService {
    override fun getMaxJobConcurrency(dispatchJobReq: BaseQueryReqVO): MaxJobConcurrencyVO? {
        return dispatchJobMetricsDao.getMaxJobConcurrency(
            dslContext = dslContext,
            dispatchJobReq = dispatchJobReq
        )
    }
}
