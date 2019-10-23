package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.third.wetest.WetestCallback
import com.tencent.devops.process.service.wetest.WetestService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalWetestResourceImpl @Autowired constructor(
    private val wetestService: WetestService
) : ExternalWetestResource {

    override fun callback(
        productID: String,
        jobID: String,
        buildID: String,
        taskID: String,
        sodaID: String,
        resultQuality: String,
        resultDevNum: String,
        resultRate: String,
        resultProblems: String,
        resultSerious: String,
        startTime: String,
        endTime: String
    ): Result<Boolean> {
        val callback = WetestCallback(productID, jobID, buildID, taskID, sodaID, resultQuality,
                resultDevNum, resultRate, resultProblems, resultSerious,
                startTime, endTime)
        wetestService.saveCallback(callback)
        return Result(true)
    }
}