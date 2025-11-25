package com.tencent.devops.common.pipeline.pojo.atom.form.components

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件表单-下拉框组件")
data class SelectInputComponent(
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
    override val type: String = AtomFormComponentType.SELECT_INPUT.value,
    @JsonIgnore
    override val key: String,
    @get:Schema(description = "下拉选配置")
    val optionsConf: SelectInputComponentConfig? = null,
    @get:Schema(description = "下拉选选项集合")
    val options: List<SelectInputComponentOption>? = null
) : AtomFormComponent {
    companion object {
        const val classType = "select-input"
    }
}

/**
 * select-input/devops-select组件的配置参数
 */
data class SelectInputComponentConfig(
    @get:Schema(description = "是否可搜索")
    val searchable: Boolean? = false,
    @get:Schema(description = "是否为多选")
    val multiple: Boolean? = false,
    @get:Schema(description = "蓝盾服务链接或蓝鲸网关API链接，支持{变量名}格式引用变量（projectId、pipelineId、buildId）")
    val url: String? = "",
    @get:Schema(description = "选项列表数据在API返回体json中的路径，默认值为\"data\"（示例：data.detail.list）")
    val dataPath: String? = "data",
    @get:Schema(description = "url返回规范中，用于下拉列表选项key的字段名")
    val paramId: String? = "",
    @get:Schema(description = "url返回规范中，用于下拉列表选项label的字段名")
    val paramName: String? = "",
    @get:Schema(description = "是否有新增按钮（type=selector/select-input时生效）")
    val hasAddItem: Boolean? = false,
    @get:Schema(description = "新增按钮文字描述（type=selector/select-input时生效）")
    val itemText: String? = null,
    @get:Schema(description = "点击新增按钮的跳转地址（type=selector/select-input时生效）")
    val itemTargetUrl: String? = null
)

data class SelectInputComponentOption(
    @get:Schema(description = "选项ID")
    val id: String,
    @get:Schema(description = "选项名称")
    val name: String,
    @get:Schema(description = "选项说明")
    val desc: String? = null,
    @get:Schema(description = "是否可选")
    val disabled: Boolean? = false
)
