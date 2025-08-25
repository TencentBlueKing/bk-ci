package com.tencent.devops.common.pipeline.pojo.element

import com.tencent.devops.common.pipeline.TemplateDescriptor
import com.tencent.devops.common.pipeline.pojo.TemplateVariable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "step模板模型")
data class StepTemplateElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "step模版",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "来源于模版", required = false)
    override var fromTemplate: Boolean? = true,
    @get:Schema(title = "模板路径", required = false)
    override var templatePath: String? = null,
    @get:Schema(title = "模板版本引用,分支/tag/commit", required = false)
    override var templateRef: String? = null,
    @get:Schema(title = "模板ID", required = false)
    override var templateId: String? = null,
    @get:Schema(title = "模版版本名称", required = false)
    override var templateVersionName: String? = null,
    @get:Schema(title = "模板参数构建", required = false)
    override var templateVariables: List<TemplateVariable>? = null
) : Element(name, id, status), TemplateDescriptor {
    companion object {
        const val classType = "stepTemplate"
    }

    override fun getClassType(): String = classType
}
