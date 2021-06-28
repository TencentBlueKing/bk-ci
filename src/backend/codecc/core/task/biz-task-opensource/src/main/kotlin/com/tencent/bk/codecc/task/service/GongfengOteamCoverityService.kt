package com.tencent.bk.codecc.task.service

import com.tencent.bk.codecc.task.model.CustomProjEntity
import com.tencent.bk.codecc.task.pojo.CodeCCPipelineReq
import com.tencent.devops.common.pipeline.container.Stage

interface GongfengOteamCoverityService {

    /**
     * 解析ci.yml文件，并且根据文件配置流水线编排
     */
    fun parseCiYml(
        codeCCPipelineReq: CodeCCPipelineReq,
        customProjEntity: CustomProjEntity,
        userName: String
    ): List<Stage>
}