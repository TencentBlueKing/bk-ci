package com.tencent.devops.common.auth.api.pojo

import com.tencent.devops.common.auth.enums.AuthSystemType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "筛选项目条件实体")
data class ProjectConditionDTO(
    @get:Schema(title = "中心ID")
    val centerId: Long? = null,
    @get:Schema(title = "部门ID")
    val deptId: Long? = null,
    @get:Schema(title = "bgId")
    val bgId: Long? = null,
    @get:Schema(title = "businessLineId")
    val businessLineId: Long? = null,
    @get:Schema(title = "项目名称")
    val projectName: String? = null,
    @get:Schema(title = "项目英文名称")
    val englishName: String? = null,
    @get:Schema(title = "bg列表")
    val bgIdList: List<Long>? = null,
    @get:Schema(title = "项目创建人")
    val projectCreator: String? = null,
    @get:Schema(title = "排除项目code")
    val excludedProjectCodes: List<String>? = null,
    @get:Schema(title = "项目ID列表")
    var projectCodes: List<String>? = null,
    @get:Schema(title = "资源类型")
    val resourceType: String? = null,
    @get:Schema(title = "路由tag")
    val routerTag: AuthSystemType? = null,
    @get:Schema(title = "是否包含router_tag为null")
    val includeNullRouterTag: Boolean? = false,
    @get:Schema(title = "是否关联产品")
    val relatedProduct: Boolean? = null,
    @get:Schema(title = "排除创建时间大于该值的项目")
    val excludedCreateTime: String? = null,
    @get:Schema(title = "是否启用")
    val enabled: Boolean? = null,
    @get:Schema(title = "渠道代码")
    val channelCode: String? = null,
    @get:Schema(title = "remoteDev相关")
    val queryRemoteDevFlag: Boolean? = null,
)
