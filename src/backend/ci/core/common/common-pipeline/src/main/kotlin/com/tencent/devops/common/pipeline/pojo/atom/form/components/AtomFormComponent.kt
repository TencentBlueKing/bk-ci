package com.tencent.devops.common.pipeline.pojo.atom.form.components

import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentRelyOperation
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 插件输入项组件接口，定义组件通用配置属性
 */
@Schema(title = "插件表单-字段组件")
interface AtomFormComponent {
    val label: String?
    val type: String
    val default: Any?
    val placeholder: String?
    val groupName: String?
    val desc: String?
    val required: Boolean?
    val disabled: Boolean?
    val hidden: Boolean?
    val sensitive: Boolean?
    val rely: AtomFormComponentRely?
    // 后端生成的key，前端组件没有，用于标识插件入参对应的字段
    val key: String
}

@Schema(title = "插件表单-条件配置")
data class AtomFormComponentRely(
    @get:Schema(title = "条件关系")
    val operation: AtomFormComponentRelyOperation,
    @get:Schema(title = "条件匹配规则")
    val expression: List<AtomFormComponentRelyExpression>
)

@Schema(title = "插件表单-条件匹配规则")
data class AtomFormComponentRelyExpression(
    @get:Schema(title = "目标字段key")
    val key: String,
    @get:Schema(title = "目标字段值")
    val value: String
)
