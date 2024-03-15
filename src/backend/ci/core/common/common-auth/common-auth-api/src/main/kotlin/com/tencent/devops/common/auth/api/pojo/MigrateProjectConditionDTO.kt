package com.tencent.devops.common.auth.api.pojo

import com.tencent.devops.common.auth.enums.AuthSystemType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "条件迁移项目实体")
data class MigrateProjectConditionDTO(
    @get:Schema(title = "中心ID")
    val centerId: Long? = null,
    @get:Schema(title = "部门ID")
    val deptId: Long? = null,
    @get:Schema(title = "bgId")
    val bgId: Long? = null,
    @get:Schema(title = "bg列表")
    val bgIdList: List<Long>? = null,
    @get:Schema(title = "项目创建人")
    val projectCreator: String? = null,
    @get:Schema(title = "排除项目code")
    val excludedProjectCodes: List<String>? = null,
    @get:Schema(title = "项目ID列表")
    val projectCodes: List<String>? = null,
    @get:Schema(title = "资源类型")
    val resourceType: String? = null,
    @get:Schema(title = "路由tag")
    val routerTag: AuthSystemType? = null,
    @get:Schema(title = "是否关联产品")
    val relatedProduct: Boolean? = null,
    @get:Schema(title = "排除创建时间大于该值的项目")
    val excludedCreateTime: String? = null
)
