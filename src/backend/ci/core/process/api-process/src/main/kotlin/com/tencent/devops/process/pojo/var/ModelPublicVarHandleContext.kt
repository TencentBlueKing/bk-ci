package com.tencent.devops.process.pojo.`var`

import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Model公共变量组处理上下文")
data class ModelPublicVarHandleContext(
    @get:Schema(title = "关联资源ID", required = true)
    val referId: String,
    @get:Schema(title = "关联资源类型", required = true)
    val referType: PublicVerGroupReferenceTypeEnum,
    @get:Schema(title = "关联资源版本", required = false)
    val referVersion: Int? = null,
    @get:Schema(title = "Model公共变量组引用", required = true)
    var publicVarGroups: List<PublicVarGroupRef>,
    @get:Schema(title = "Model构建参数", required = true)
    val params: List<BuildFormProperty>,
)
