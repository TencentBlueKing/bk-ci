package com.tencent.devops.process.yaml.transfer.pojo

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSettingGroupType
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.pojo.YamlVersion

data class ModelTransferInput(
    val userId: String,
    var model: Model,
    val setting: PipelineSetting,
    val pipelineInfo: PipelineInfo?,
    val version: YamlVersion,
    val aspectWrapper: PipelineTransferAspectWrapper,
    val defaultScmType: ScmType = ScmType.CODE_GIT,
    val channelCode: ChannelCode = ChannelCode.BS
) {
    fun fromTemplate() = model.template != null

    fun overrideTemplateSettingGroups(type: PipelineSettingGroupType) =
        model.overrideTemplateField?.overrideSetting(type) ?: true
}
