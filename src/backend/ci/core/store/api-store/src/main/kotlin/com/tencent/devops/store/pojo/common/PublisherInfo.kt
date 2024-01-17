package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "发布者信息")
data class PublisherInfo(
    @Schema(title = "ID")
    val id: String,
    @Schema(title = "发布者标识")
    val publisherCode: String,
    @Schema(title = "发布者名称")
    val publisherName: String,
    @Schema(title = "发布者类型")
    val publisherType: PublisherType,
    @Schema(title = "主体负责人")
    val owners: String,
    @Schema(title = "技术支持")
    val helper: String? = null,
    @Schema(title = "一级部门ID")
    val firstLevelDeptId: Int,
    @Schema(title = "一级部门名称")
    val firstLevelDeptName: String,
    @Schema(title = "二级部门ID")
    val secondLevelDeptId: Int,
    @Schema(title = "二级部门名称")
    val secondLevelDeptName: String,
    @Schema(title = "三级部门ID")
    val thirdLevelDeptId: Int,
    @Schema(title = "三级部门名称")
    val thirdLevelDeptName: String,
    @Schema(title = "四级部门ID")
    val fourthLevelDeptId: Int? = null,
    @Schema(title = "四级部门名称")
    val fourthLevelDeptName: String? = null,
    @Schema(title = "实体组织架构")
    val organizationName: String,
    @Schema(title = "所属工作组BG")
    val bgName: String,
    @Schema(title = "是否认证")
    val certificationFlag: Boolean,
    @Schema(title = "组件类型")
    val storeType: StoreTypeEnum,
    @Schema(title = "创建人")
    val creator: String,
    @Schema(title = "最近修改人")
    val modifier: String,
    @Schema(title = "创建时间")
    val createTime: LocalDateTime,
    @Schema(title = "更新时间")
    val updateTime: LocalDateTime
)
