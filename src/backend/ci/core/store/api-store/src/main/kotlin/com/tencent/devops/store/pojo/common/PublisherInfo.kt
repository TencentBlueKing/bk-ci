package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("发布者信息")
data class PublisherInfo(
    @ApiModelProperty("ID")
    val id: String,
    @ApiModelProperty("发布者标识")
    val publisherCode: String,
    @ApiModelProperty("发布者名称")
    val publisherName: String,
    @ApiModelProperty("发布者类型")
    val publisherType: PublisherType,
    @ApiModelProperty("主体负责人")
    val owners: String,
    @ApiModelProperty("技术支持")
    val helper: String? = null,
    @ApiModelProperty("一级部门ID")
    val firstLevelDeptId: Int,
    @ApiModelProperty("一级部门名称")
    val firstLevelDeptName: String,
    @ApiModelProperty("二级部门ID")
    val secondLevelDeptId: Int,
    @ApiModelProperty("二级部门名称")
    val secondLevelDeptName: String,
    @ApiModelProperty("三级部门ID")
    val thirdLevelDeptId: Int,
    @ApiModelProperty("三级部门名称")
    val thirdLevelDeptName: String,
    @ApiModelProperty("四级部门ID")
    val fourthLevelDeptId: Int? = null,
    @ApiModelProperty("四级部门名称")
    val fourthLevelDeptName: String? = null,
    @ApiModelProperty("实体组织架构")
    val organizationName: String,
    @ApiModelProperty("所属工作组BG")
    val bgName: String,
    @ApiModelProperty("是否认证")
    val certificationFlag: Boolean,
    @ApiModelProperty("组件类型")
    val storeType: StoreTypeEnum,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("最近修改人")
    val modifier: String,
    @ApiModelProperty("创建时间")
    val createTime: LocalDateTime,
    @ApiModelProperty("更新时间")
    val updateTime: LocalDateTime
)
