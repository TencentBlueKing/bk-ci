package com.tencent.bk.codecc.task.service.impl

import com.tencent.bk.codecc.task.config.BuildConfig
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.model.CustomProjEntity
import com.tencent.bk.codecc.task.pojo.CodeCCPipelineReq
import com.tencent.bk.codecc.task.service.GongfengOteamCoverityService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Stage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GongfengOteamCoverityServiceImpl @Autowired constructor(
    private val client: Client,
    private val buildConfig: BuildConfig,
    private val baseRepository: BaseDataRepository
) : GongfengOteamCoverityService {

    override fun parseCiYml(
        codeCCPipelineReq: CodeCCPipelineReq,
        customProjEntity: CustomProjEntity,
        userName: String
    ): List<Stage> {
        // TODO("not implemented")
        return emptyList()
    }
}
