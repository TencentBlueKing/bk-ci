package com.tencent.devops.process.yaml.modelTransfer.pojo

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.v2.models.IPreTemplateScriptBuildYaml

data class YamlTransferInput(
    val userId: String,
    val projectCode: String,
    val pipelineInfo: PipelineInfo?,
    val yaml: IPreTemplateScriptBuildYaml,
    val defaultScmType: ScmType = ScmType.CODE_GIT,
    val asCodeSettings: PipelineAsCodeSettings? = null,
    val jobTemplateAcrossInfo: Map<String, BuildTemplateAcrossInfo>? = null
)
