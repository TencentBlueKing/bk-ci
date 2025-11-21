package com.tencent.devops.process.pojo.`var`

import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "变量组处理上下文")
data class VarGroupProcessContext(
    @get:Schema(title = "用户ID", description = "操作用户的唯一标识", required = true)
    val userId: String,
    @get:Schema(title = "项目ID", description = "所属项目的唯一标识", required = true)
    val projectId: String,
    @get:Schema(title = "资源ID", description = "引用资源的唯一标识（如流水线ID、模板ID）", required = true)
    val resourceId: String,
    @get:Schema(title = "引用类型", description = "资源引用类型（PIPELINE/TEMPLATE）", required = true)
    val referType: PublicVerGroupReferenceTypeEnum,
    @get:Schema(title = "资源版本", description = "引用资源的版本号", required = true)
    val resourceVersion: Int,
    @get:Schema(title = "模型变量组列表", description = "Model中的变量组列表", required = true)
    val modelVarGroups: List<PublicVarGroupRef>,
    @get:Schema(
        title = "公共变量映射",
        description = "变量组名称到变量名集合的映射，格式：Map<varGroupName, Set<varName>>",
        required = true
    )
    val publicVarMap: Map<String, Set<String>>,
    @get:Schema(title = "被引用的变量名集合", description = "所有被引用的变量名称集合", required = true)
    val referencedVarNames: Set<String>,
    @get:Schema(
        title = "最新版本映射",
        description = "变量组名称到最新版本号的映射，格式：Map<groupName, latestVersion>",
        required = true
    )
    val latestVersionMap: Map<String, Int>,
    @get:Schema(
        title = "已存在的变量名映射",
        description = "已存在的变量名映射，key为PublicGroupKey，value为变量名集合",
        required = true
    )
    val allExistingVarNames: Map<PublicGroupKey, Set<String>>
)
