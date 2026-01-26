package com.tencent.devops.process.pojo.`var`

import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "变量组版本处理信息")
data class VarGroupVersionChangeInfo(
    @get:Schema(title = "变量组名称")
    val groupName: String,
    @get:Schema(title = "版本号")
    val version: Int,
    @get:Schema(title = "变量组所属项目")
    val sourceProjectId: String,
    @get:Schema(title = "关联引用ID")
    val referId: String,
    @get:Schema(title = "关联引用类型")
    val referType: PublicVerGroupReferenceTypeEnum,
    @get:Schema(title = "关联引用版本")
    val referVersion: Int,
    @get:Schema(title = "待删除的引用记录")
    var referInfoToDelete: ResourcePublicVarGroupReferPO? = null,
    @get:Schema(title = "待新增的引用记录")
    var referInfoToAdd: ResourcePublicVarGroupReferPO? = null,
    @get:Schema(title = "引用计数变化量（正数增加，负数减少）")
    var countChange: Int = 0
) {
    /**
     * 设置删除操作
     * @param referInfo 待删除的引用记录
     */
    fun setDeleteOperation(referInfo: ResourcePublicVarGroupReferPO) {
        this.referInfoToDelete = referInfo
        this.countChange -= 1
    }

    /**
     * 设置新增操作
     * @param referInfo 待新增的引用记录
     */
    fun setAddOperation(referInfo: ResourcePublicVarGroupReferPO) {
        this.referInfoToAdd = referInfo
        this.countChange += 1
    }

    /**
     * 判断是否有实际变化
     * @return true表示有变化（存在删除或新增操作），false表示无变化
     */
    fun hasChanges(): Boolean {
        return referInfoToDelete != null || referInfoToAdd != null
    }
}
