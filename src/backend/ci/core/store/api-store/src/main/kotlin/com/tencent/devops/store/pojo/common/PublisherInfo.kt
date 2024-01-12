package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "发布者信息")
data class PublisherInfo(
    @Schema(description = "ID")
    val id: String,
    @Schema(description = "发布者标识")
    val publisherCode: String,
    @Schema(description = "发布者名称")
    val publisherName: String,
    @Schema(description = "发布者类型")
    val publisherType: PublisherType,
    @Schema(description = "主体负责人")
    val owners: String,
    @Schema(description = "技术支持")
    val helper: String? = null,
    @Schema(description = "一级部门ID")
    val firstLevelDeptId: Int,
    @Schema(description = "一级部门名称")
    val firstLevelDeptName: String,
    @Schema(description = "二级部门ID")
    val secondLevelDeptId: Int,
    @Schema(description = "二级部门名称")
    val secondLevelDeptName: String,
    @Schema(description = "三级部门ID")
    val thirdLevelDeptId: Int,
    @Schema(description = "三级部门名称")
    val thirdLevelDeptName: String,
    @Schema(description = "四级部门ID")
    val fourthLevelDeptId: Int? = null,
    @Schema(description = "四级部门名称")
    val fourthLevelDeptName: String? = null,
    @Schema(description = "实体组织架构")
    val organizationName: String,
    @Schema(description = "所属工作组BG")
    val bgName: String,
    @Schema(description = "是否认证")
    val certificationFlag: Boolean,
    @Schema(description = "组件类型")
    val storeType: StoreTypeEnum,
    @Schema(description = "创建人")
    val creator: String,
    @Schema(description = "最近修改人")
    val modifier: String,
    @Schema(description = "创建时间")
    val createTime: LocalDateTime,
    @Schema(description = "更新时间")
    val updateTime: LocalDateTime
)
