package com.tencent.devops.process.yaml.transfer.pojo

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper

data class TemplateModelTransferInput(
    val userId: String,
    val projectId: String,
    var model: ITemplateModel,
    val setting: PipelineSetting?,
    val version: YamlVersion,
    val aspectWrapper: PipelineTransferAspectWrapper,
    val defaultScmType: ScmType = ScmType.CODE_GIT
)
