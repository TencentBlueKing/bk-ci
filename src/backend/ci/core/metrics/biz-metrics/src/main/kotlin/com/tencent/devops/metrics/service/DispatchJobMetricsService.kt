package com.tencent.devops.metrics.service

import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.MaxJobConcurrencyVO

interface DispatchJobMetricsService {
    /**
     * 获取job最大并发
     * @param dispatchJobReq DispatchJob查询请求报文
     * @return JOB执行最大并发
     */
    fun getMaxJobConcurrency(dispatchJobReq: BaseQueryReqVO): MaxJobConcurrencyVO?
}
