package com.tencent.devops.store.trigger.utils

import com.tencent.devops.common.api.auth.AUTH_HEADER_CDS_IP
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomForm
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomFromInputGroup
import com.tencent.devops.common.pipeline.pojo.atom.form.components.AtomFormComponent
import com.tencent.devops.common.pipeline.pojo.atom.form.components.CheckBoxListComponentOption
import com.tencent.devops.common.pipeline.pojo.atom.form.components.CheckboxListComponent
import com.tencent.devops.common.pipeline.pojo.atom.form.components.EnumInputComponent
import com.tencent.devops.common.pipeline.pojo.atom.form.components.EnumInputOptions
import com.tencent.devops.common.pipeline.pojo.atom.form.components.GroupComponent
import com.tencent.devops.common.pipeline.pojo.atom.form.components.GroupItemComponent
import com.tencent.devops.common.pipeline.pojo.atom.form.components.SelectInputComponent
import com.tencent.devops.common.pipeline.pojo.atom.form.components.SelectInputComponentOption
import com.tencent.devops.common.pipeline.pojo.atom.form.components.SelectInputComponentConfig
import com.tencent.devops.common.pipeline.pojo.atom.form.components.VueInputComponent
import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentGroupType
import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import com.tencent.devops.store.pojo.trigger.EventFieldMappingItem
import com.tencent.devops.store.pojo.trigger.TriggerEventConfig
import com.tencent.devops.store.pojo.trigger.conditions.CheckboxListCondition
import com.tencent.devops.store.pojo.trigger.conditions.EnumInputCondition
import com.tencent.devops.store.pojo.trigger.conditions.InputCondition
import com.tencent.devops.store.pojo.trigger.conditions.SelectCondition
import com.tencent.devops.store.pojo.trigger.conditions.TriggerCondition
import com.tencent.devops.store.pojo.trigger.enums.ConditionOperatorEnum
import com.tencent.devops.store.pojo.trigger.enums.MappingSource

object TriggerEventConverter {
    fun convertAtomForm(triggerEventConfig: TriggerEventConfig, storeCode: String): AtomForm {
        return convertAtomForm(triggerEventConfig.conditions, storeCode)
    }

    fun convertAtomForm(
        conditions: List<TriggerCondition>,
        storeCode: String,
        groupType: AtomFormComponentGroupType = AtomFormComponentGroupType.STORE_GROUP
    ): AtomForm {
        val groupNames = conditions.map { it.group }
        val inputs = mutableListOf<AtomFormComponent>()
        val inputGroups = if (groupType == AtomFormComponentGroupType.STORE_GROUP) {
            conditions.map { it.group }
                    .filter { !it.isNullOrBlank() }
                    .toSet()
                    .map {
                        AtomFromInputGroup(
                            label = it!!,
                            name = it,
                            expanded = true
                        )
                    }
        } else {
            listOf()
        }
        // 已处理分组
        val existsGroup = mutableListOf<String>()
        conditions.forEachIndexed { index, condition ->
            val currentGroup = condition.group
            // 已处理则跳过
            if (existsGroup.contains(currentGroup)) {
                return@forEachIndexed
            }
            // 生成对应的组件
            if (
                    currentGroup.isNullOrBlank() ||
                    !groupNames.contains(currentGroup) ||
                    groupType == AtomFormComponentGroupType.STORE_GROUP
            ) {
                convertComponent(condition)
            } else {
                val groupFields = conditions.subList(index + 1, conditions.size).filter { it.group == currentGroup }
                val children = listOf(condition).plus(groupFields).map {
                    convertComponent(it)
                }.map { covertTriggerGroupComponent(it) }
                // 记录已处理的分组
                existsGroup.add(currentGroup)
                GroupComponent(
                    label = currentGroup,
                    key = condition.key(),
                    children = children
                )
            }.let {
                inputs.add(it)
            }
        }

        return AtomForm(
            atomCode = storeCode,
            input = inputs.associateBy { it.key ?: "" },
            inputGroups = inputGroups
        )
    }

    private fun covertTriggerGroupComponent(condition: AtomFormComponent) = when (condition) {
        is VueInputComponent -> {
            GroupItemComponent(
                key = condition.key,
                prependText = condition.label,
                placeholder = condition.placeholder,
                required = condition.required,
                label = "",
                component = AtomFormComponentType.COMPOSITE_INPUT.value
            )
        }

        else -> GroupItemComponent(
            key = condition.key,
            prependText = condition.label ?: "",
            placeholder = condition.placeholder,
            required = condition.required,
            label = "",
            component = condition.type
        )
    }

    private fun convertComponent(condition: TriggerCondition): AtomFormComponent {
        return when (condition) {
            is CheckboxListCondition -> {
                CheckboxListComponent(
                    label = condition.label,
                    default = condition.defaultValue,
                    list = condition.options?.map {
                        CheckBoxListComponentOption(
                            id = it.value,
                            name = it.label
                        )
                    } ?: listOf(),
                    desc = condition.desc,
                    required = condition.required,
                    key = condition.key(),
                    groupName = condition.group
                )
            }

            is EnumInputCondition -> {
                EnumInputComponent(
                    label = condition.label,
                    default = condition.defaultValue,
                    list = condition.options?.map {
                        EnumInputOptions(
                            value = it.value,
                            label = it.label
                        )
                    } ?: listOf(),
                    desc = condition.desc,
                    required = condition.required,
                    key = condition.key(),
                    groupName = condition.group
                )
            }

            is InputCondition -> {
                VueInputComponent(
                    label = condition.label,
                    default = condition.defaultValue,
                    desc = condition.desc,
                    required = condition.required,
                    key = condition.key(),
                    groupName = condition.group
                )
            }

            is SelectCondition -> {
                SelectInputComponent(
                    label = condition.label,
                    default = condition.defaultValue,
                    options = condition.options?.map {
                        SelectInputComponentOption(
                            id = it.value,
                            name = it.label
                        )
                    } ?: listOf(),
                    desc = condition.desc,
                    required = condition.required,
                    optionsConf = SelectInputComponentConfig(
                        multiple = condition.multiple
                    ),
                    key = condition.key(),
                    groupName = condition.group
                )
            }

            else -> {
                throw IllegalArgumentException("Unsupported condition type")
            }
        }
    }
}

fun main() {
    val triggerEventConfig = TriggerEventConfig(
        conditions = listOf(
            InputCondition(
                label = "监听的云桌面",
                group = "云桌面设置",
                desc = "默认监听当前创作环境下的所有云桌面，范围不超过当前创作环境",
                targetField = "cdsIp",
                defaultValue = null,
                operator = ConditionOperatorEnum.LIKE,
                required = false
            ),
            InputCondition(
                label = "忽略的云桌面",
                group = "云桌面设置",
                desc = "多个云桌面间以英文逗号分隔",
                targetField = "cdsIp",
                defaultValue = null,
                operator = ConditionOperatorEnum.NOT_LIKE,
                required = false
            ),
            InputCondition(
                label = "触发用户",
                group = "触发用户设置",
                desc = "多个用户间以英文逗号分隔符",
                targetField = "userId",
                defaultValue = null,
                operator = ConditionOperatorEnum.LIKE,
                required = false
            ),
            InputCondition(
                label = "忽略用户",
                group = "触发用户设置",
                desc = "多个用户间以英文逗号分隔符",
                targetField = "userId",
                defaultValue = null,
                operator = ConditionOperatorEnum.NOT_LIKE,
                required = false
            )
        ),
        fieldMapping = listOf(
            EventFieldMappingItem(
                sourcePath = AUTH_HEADER_USER_ID,
                targetField = "userId",
                source = MappingSource.HEADER
            ),
            EventFieldMappingItem(
                sourcePath = AUTH_HEADER_CDS_IP,
                targetField = "cdsIp",
                source = MappingSource.HEADER
            )
        )
    )

    println(JsonUtil.toJson(triggerEventConfig))
    val atomForm = TriggerEventConverter.convertAtomForm(
        triggerEventConfig,
        ""
    )

    println(JsonUtil.toJson(atomForm))
}
