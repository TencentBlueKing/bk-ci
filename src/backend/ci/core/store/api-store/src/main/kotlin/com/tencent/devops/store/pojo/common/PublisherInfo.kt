package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "发布者信息")
data class PublisherInfo(
    @Schema(name = "ID")
    val id: String,
    @Schema(name = "发布者标识")
    val publisherCode: String,
    @Schema(name = "发布者名称")
    val publisherName: String,
    @Schema(name = "发布者类型")
    val publisherType: PublisherType,
    @Schema(name = "主体负责人")
    val owners: String,
    @Schema(name = "技术支持")
    val helper: String? = null,
    @Schema(name = "一级部门ID")
    val firstLevelDeptId: Int,
    @Schema(name = "一级部门名称")
    val firstLevelDeptName: String,
    @Schema(name = "二级部门ID")
    val secondLevelDeptId: Int,
    @Schema(name = "二级部门名称")
    val secondLevelDeptName: String,
    @Schema(name = "三级部门ID")
    val thirdLevelDeptId: Int,
    @Schema(name = "三级部门名称")
    val thirdLevelDeptName: String,
    @Schema(name = "四级部门ID")
    val fourthLevelDeptId: Int? = null,
    @Schema(name = "四级部门名称")
    val fourthLevelDeptName: String? = null,
    @Schema(name = "实体组织架构")
    val organizationName: String,
    @Schema(name = "所属工作组BG")
    val bgName: String,
    @Schema(name = "是否认证")
    val certificationFlag: Boolean,
    @Schema(name = "组件类型")
    val storeType: StoreTypeEnum,
    @Schema(name = "创建人")
    val creator: String,
    @Schema(name = "最近修改人")
    val modifier: String,
    @Schema(name = "创建时间")
    val createTime: LocalDateTime,
    @Schema(name = "更新时间")
    val updateTime: LocalDateTime
)
