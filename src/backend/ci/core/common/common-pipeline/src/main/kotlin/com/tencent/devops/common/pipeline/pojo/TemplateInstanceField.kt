package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSettingGroupType

/**
 * 模版实例化时的字段,当流水线从模版实例化时,表示哪些字段可以被实例化,实例化时可以选择跟随模版的值，还是流水线自定义
 */
data class TemplateInstanceField(
    // 流水线需要自定义的参数
    val paramIds: List<String>? = null,
    // 流水线指定的自定义的触发器,只能自定义启用/禁用,不能新增修改删除触发器
    val triggerStepIds: List<String>? = null,
    // 流水线需要自定义的触发器组
    val settingGroups: List<PipelineSettingGroupType>? = null
) {
    companion object {
        // 推荐版本号
        const val BK_CI_BUILD_NO = "BK_CI_BUILD_NO"

        /**
         * model应该传模版的,不要传流水线的
         */
        fun initFromTrigger(
            model: Model
        ): TemplateInstanceField {
            val triggerContainer = model.getTriggerContainer()
            val paramIds = triggerContainer.params.filter {
                it.constant != true && it.required
            }.map { it.id }.toMutableList()
            if (triggerContainer.buildNo != null) {
                paramIds.add(BK_CI_BUILD_NO)
            }
            return TemplateInstanceField(
                paramIds = paramIds,
                settingGroups = PipelineSettingGroupType.values().map { it }
            )
        }
    }

    fun overrideParam(paramId: String): Boolean {
        return paramIds?.contains(paramId) == true
    }

    /**
     * 是否覆盖推荐版本,推荐版本号也放在参数中传递
     */
    fun overrideBuildNo(): Boolean {
        return overrideParam(BK_CI_BUILD_NO)
    }

    fun overrideTrigger(triggerStepId: String): Boolean {
        return triggerStepIds?.contains(triggerStepId) == true
    }

    fun overrideSetting(settingGroup: PipelineSettingGroupType): Boolean {
        return settingGroups?.contains(settingGroup) == true
    }
}
