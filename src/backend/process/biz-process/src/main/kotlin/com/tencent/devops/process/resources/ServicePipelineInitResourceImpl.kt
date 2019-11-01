package com.tencent.devops.process.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.AtomMarketInitPipelineReq
import com.tencent.devops.common.pipeline.pojo.CheckImageInitPipelineReq
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineInitResource
import com.tencent.devops.process.pojo.AtomMarketInitPipelineResp
import com.tencent.devops.process.pojo.CheckImageInitPipelineResp
import com.tencent.devops.process.service.AtomMarketInitPipelineService
import com.tencent.devops.process.service.CheckImageInitPipelineService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePipelineInitResourceImpl @Autowired constructor(
    private val atomMarketInitPipelineService: AtomMarketInitPipelineService,
    private val checkImageInitPipelineService: CheckImageInitPipelineService
) : ServicePipelineInitResource {

    override fun initAtomMarketPipeline(
        userId: String,
        projectCode: String,
        atomMarketInitPipelineReq: AtomMarketInitPipelineReq
    ): Result<AtomMarketInitPipelineResp> {
        return atomMarketInitPipelineService.initPipeline(
            userId = userId,
            projectCode = projectCode,
            atomBaseInfo = atomMarketInitPipelineReq.atomBaseInfo,
            repositoryHashId = atomMarketInitPipelineReq.repositoryHashId,
            repositoryPath = atomMarketInitPipelineReq.repositoryPath,
            script = atomMarketInitPipelineReq.script,
            buildEnv = atomMarketInitPipelineReq.buildEnv
        )
    }

    override fun initCheckImagePipeline(
        userId: String,
        projectCode: String,
        checkImageInitPipelineReq: CheckImageInitPipelineReq
    ): Result<CheckImageInitPipelineResp> {
        return checkImageInitPipelineService.initCheckImagePipeline(
            userId = userId,
            projectCode = projectCode,
            checkImageInitPipelineReq = checkImageInitPipelineReq
        )
    }
}
