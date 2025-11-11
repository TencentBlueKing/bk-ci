package com.tencent.devops.store.event.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomForm
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
import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import com.tencent.devops.store.pojo.event.TriggerEventConfig
import com.tencent.devops.store.pojo.event.conditions.CheckboxListCondition
import com.tencent.devops.store.pojo.event.conditions.ConditionOption
import com.tencent.devops.store.pojo.event.conditions.EnumInputCondition
import com.tencent.devops.store.pojo.event.conditions.InputCondition
import com.tencent.devops.store.pojo.event.conditions.SelectCondition
import com.tencent.devops.store.pojo.event.conditions.TriggerCondition
import com.tencent.devops.store.pojo.event.enums.ConditionOperator

object TriggerEventConverter {
    fun convertAtomForm(triggerEventConfig: TriggerEventConfig): AtomForm {
        with(triggerEventConfig) {
            val groupNames = conditions.map { it.group }
            val inputs = mutableListOf<AtomFormComponent>()
            // 已处理分组
            val existsGroup = mutableListOf<String>()
            conditions.forEachIndexed { index, condition ->
                val currentGroup = condition.group
                // 已处理则跳过
                if (existsGroup.contains(currentGroup)) {
                    return@forEachIndexed
                }
                val component = if (currentGroup.isNullOrBlank() || !groupNames.contains(currentGroup)) {
                    convertComponent(condition)
                } else {
                    val groupFields = conditions.subList(index + 1, conditions.size).filter { it.group == currentGroup }
                    val children = listOf(condition).plus(groupFields).map {
                        convertComponent(it)
                    }.map { covertGroupComponent(it) }
                    // 记录已处理的分组
                    existsGroup.add(currentGroup)
                    GroupComponent(
                        label = currentGroup,
                        key = condition.key(),
                        children = children
                    )
                }
                inputs.add(component)
            }

            return AtomForm(
                atomCode = "",
                input = inputs.associateBy { it.key }
            )
        }
    }

    private fun covertGroupComponent(condition: AtomFormComponent) = when (condition) {
        is VueInputComponent -> {
            GroupItemComponent(
                key = condition.key,
                prependText = condition.label,
                placeholder = "请输入",
                required = condition.required,
                label = "",
                component = AtomFormComponentType.COMPOSITE_INPUT.value
            )
        }

        else -> GroupItemComponent(
            key = condition.key,
            prependText = condition.label ?: "",
            placeholder = "请输入",
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
                    default = condition.default,
                    list = condition.options?.map {
                        CheckBoxListComponentOption(
                            id = it.value,
                            name = it.label
                        )
                    } ?: listOf(),
                    desc = condition.desc,
                    required = condition.required,
                    key = condition.key()
                )
            }

            is EnumInputCondition -> {
                EnumInputComponent(
                    label = condition.label,
                    default = condition.default,
                    list = condition.options?.map {
                        EnumInputOptions(
                            value = it.value,
                            label = it.label
                        )
                    } ?: listOf(),
                    desc = condition.desc,
                    required = condition.required,
                    key = condition.key()
                )
            }

            is InputCondition -> {
                VueInputComponent(
                    label = condition.label,
                    default = condition.default,
                    desc = condition.desc,
                    required = condition.required,
                    key = condition.key()
                )
            }

            is SelectCondition -> {
                SelectInputComponent(
                    label = condition.label,
                    default = condition.default,
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
                    key = condition.key()
                )
            }

            else -> {
                throw IllegalArgumentException("Unsupported condition type")
            }
        }
    }
}

fun main() {
    val atomForm = TriggerEventConverter.convertAtomForm(
        TriggerEventConfig(
            conditions = listOf(
                CheckboxListCondition(
                    label = "动作",
                    options = listOf(
                        ConditionOption(
                            label = "open",
                            value = "open"
                        ),
                        ConditionOption(
                            label = "update",
                            value = "update"
                        )
                    ),
                    desc = "触发动作",
                    refField = "ci.action",
                    default = null,
                    operator = ConditionOperator.NOT_IN,
                    required = false
                ),
                EnumInputCondition(
                    label = "事件",
                    options = listOf(
                        ConditionOption(
                            label = "push",
                            value = "push"
                        ),
                        ConditionOption(
                            label = "mr",
                            value = "mr"
                        )
                    ),
                    desc = "触发事件",
                    refField = "ci.action",
                    default = null,
                    operator = ConditionOperator.IN,
                    required = false
                ),
                InputCondition(
                    label = "路径",
                    group = "路径",
                    desc = "触发路径",
                    refField = "ci.action",
                    default = null,
                    operator = ConditionOperator.LIKE,
                    required = false
                ),
                InputCondition(
                    label = "排除路径",
                    group = "路径",
                    desc = "排除路径",
                    refField = "ci.action",
                    default = null,
                    operator = ConditionOperator.NOT_LIKE,
                    required = false
                ),
                SelectCondition(
                    label = "评论来源",
                    options = listOf(
                        ConditionOption(
                            label = "commit",
                            value = "commit"
                        ),
                        ConditionOption(
                            label = "mr",
                            value = "mr"
                        )
                    ),
                    desc = "评论来源",
                    refField = "ci.action",
                    default = null,
                    operator = ConditionOperator.IN,
                    required = false,
                    multiple = true
                )
            ),
            fieldMapping = listOf()
        )
    )

    println(JsonUtil.toJson(atomForm))
}
