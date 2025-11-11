package com.tencent.devops.common.pipeline.pojo.atom.form

import com.tencent.devops.common.pipeline.pojo.atom.form.components.AtomFormComponent
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件表单")
data class AtomForm(
    @get:Schema(title = "插件唯一标识")
    var atomCode: String,
    @get:Schema(title = "名称")
    var inputGroup: List<AtomFromInputGroups>? = null,
    @get:Schema(title = "执行命令入口")
    var execution: AtomFromExecution? = null,
    @get:Schema(title = "插件输入字段")
    var input: Map<String, AtomFormComponent>,
    @get:Schema(title = "插件输出字段")
    var output: Map<String, AtomFromOutputItem>? = null
)
