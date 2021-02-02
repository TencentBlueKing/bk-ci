package com.tencent.devops.lambda.resource

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.lambda.api.user.UserLambdaResource
import com.tencent.devops.lambda.pojo.MakeUpBuildVO
import com.tencent.devops.lambda.service.LambdaDataService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserLambdaResourceImpl @Autowired constructor(private val lambdaDataService: LambdaDataService) : UserLambdaResource {
    override fun manualMakeUpBuildHistory(userId: String, makeUpBuildVOs: List<MakeUpBuildVO>): Result<Boolean> {
        return Result(lambdaDataService.makeUpBuildHistory(userId, makeUpBuildVOs))
    }

    override fun manualMakeUpBuildTasks(userId: String, makeUpBuildVOs: List<MakeUpBuildVO>): Result<Boolean> {
        return Result(lambdaDataService.makeUpBuildTasks(userId, makeUpBuildVOs))
    }
}
