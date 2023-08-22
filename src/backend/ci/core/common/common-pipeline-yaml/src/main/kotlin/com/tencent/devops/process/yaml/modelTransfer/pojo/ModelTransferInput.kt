package com.tencent.devops.process.yaml.modelTransfer.pojo

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.yaml.pojo.YamlVersion

data class ModelTransferInput(
    var model: Model,
    val setting: PipelineSetting,
    val version: YamlVersion.Version,
    val defaultScmType: ScmType = ScmType.CODE_GIT
)
