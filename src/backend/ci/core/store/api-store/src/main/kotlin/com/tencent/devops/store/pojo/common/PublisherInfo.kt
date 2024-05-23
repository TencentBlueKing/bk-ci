package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "发布者信息")
data class PublisherInfo(
    @get:Schema(title = "ID")
    val id: String,
    @get:Schema(title = "发布者标识")
    val publisherCode: String,
    @get:Schema(title = "发布者名称")
    val publisherName: String,
    @get:Schema(title = "发布者类型")
    val publisherType: PublisherType,
    @get:Schema(title = "主体负责人")
    val owners: String,
    @get:Schema(title = "技术支持")
    val helper: String? = null,
    @get:Schema(title = "一级部门ID")
    val firstLevelDeptId: Int,
    @get:Schema(title = "一级部门名称")
    val firstLevelDeptName: String,
    @get:Schema(title = "二级部门ID")
    val secondLevelDeptId: Int,
    @get:Schema(title = "二级部门名称")
    val secondLevelDeptName: String,
    @get:Schema(title = "三级部门ID")
    val thirdLevelDeptId: Int,
    @get:Schema(title = "三级部门名称")
    val thirdLevelDeptName: String,
    @get:Schema(title = "四级部门ID")
    val fourthLevelDeptId: Int? = null,
    @get:Schema(title = "四级部门名称")
    val fourthLevelDeptName: String? = null,
    @get:Schema(title = "实体组织架构")
    val organizationName: String,
    @get:Schema(title = "所属工作组BG")
    val bgName: String,
    @get:Schema(title = "是否认证")
    val certificationFlag: Boolean,
    @get:Schema(title = "组件类型")
    val storeType: StoreTypeEnum,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "最近修改人")
    val modifier: String,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime,
    @get:Schema(title = "更新时间")
    val updateTime: LocalDateTime
)
