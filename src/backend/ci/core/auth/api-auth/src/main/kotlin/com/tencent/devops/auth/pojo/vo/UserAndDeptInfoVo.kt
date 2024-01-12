package com.tencent.devops.auth.pojo.vo

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.pojo.BkUserDeptInfo
import com.tencent.devops.auth.pojo.BkUserExtras
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "用户和组织信息返回实体")
data class UserAndDeptInfoVo(
    @Schema(description = "id")
    val id: Int,
    @Schema(description = "名称")
    val name: String,
    @Schema(description = "信息类型")
    val type: ManagerScopesEnum,
    @Schema(description = "是否拥有子级")
    val hasChild: Boolean? = false,
    @Schema(description = "用户部门详细信息")
    val deptInfo: List<BkUserDeptInfo>? = null,
    @Schema(description = "用户额外详细信息")
    val extras: BkUserExtras? = null,
    @Schema(description = "水印信息")
    val waterMark: String? = null,
    @Schema(description = "是否是项目成员")
    val belongProjectMember: Boolean? = null
)
