package com.tencent.devops.auth.pojo.vo

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.pojo.BkUserDeptInfo
import com.tencent.devops.auth.pojo.BkUserExtras
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户和组织信息返回实体")
data class UserAndDeptInfoVo(
    @get:Schema(title = "id")
    val id: Int,
    @get:Schema(title = "名称（RTX）")
    val name: String,
    @get:Schema(title = "中文名称")
    val displayName: String,
    @get:Schema(title = "信息类型")
    val type: ManagerScopesEnum,
    @get:Schema(title = "是否拥有子级")
    val hasChild: Boolean? = false,
    @get:Schema(title = "用户部门详细信息")
    val deptInfo: List<BkUserDeptInfo>? = null,
    @get:Schema(title = "用户额外详细信息")
    val extras: BkUserExtras? = null,
    @get:Schema(title = "水印信息")
    val waterMark: String? = null,
    @get:Schema(title = "是否是项目成员")
    val belongProjectMember: Boolean? = null,
    @get:Schema(title = "是否离职")
    val departed: Boolean? = null
)
