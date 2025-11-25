package com.tencent.devops.common.pipeline.pojo.atom.form.components

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件表单-复选框（布尔）组件")
data class CheckboxListComponent(
    @get:Schema(title = "中文名")
    override val label: String,
    @get:Schema(title = "默认值")
    override val default: Any? = null,
    @get:Schema(title = "输入框占位文本")
    override val placeholder: String? = null,
    @get:Schema(title = "所属组", description = "对应inputGroups中定义的name")
    override val groupName: String? = null,
    @get:Schema(title = "字段说明", description = "支持\\n换行")
    override val desc: String? = null,
    @get:Schema(title = "是否必填")
    override val required: Boolean? = false,
    @get:Schema(title = "是否可编辑")
    override val disabled: Boolean? = null,
    @get:Schema(title = "是否隐藏")
    override val hidden: Boolean? = null,
    @get:Schema(title = "是否敏感信息")
    override val sensitive: Boolean? = null,
    @get:Schema(title = "根据条件显示/隐藏当前字段")
    override val rely: AtomFormComponentRely? = null,
    override val type: String = AtomFormComponentType.ATOM_CHECKBOX_LIST.value,
    @JsonIgnore
    override val key: String,
    val list: List<CheckBoxListComponentOption>? = null
) : AtomFormComponent {
    companion object {
        const val classType = "atom-checkbox-list"
    }
}

data class CheckBoxListComponentOption(
    @get:Schema(description = "选项ID")
    val id: String,
    @get:Schema(description = "选项名称")
    val name: String,
    @get:Schema(description = "选项说明")
    val desc: String? = ""
)
