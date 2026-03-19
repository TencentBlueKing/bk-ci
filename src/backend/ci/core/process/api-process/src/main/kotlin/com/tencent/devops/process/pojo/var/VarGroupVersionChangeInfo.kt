package com.tencent.devops.process.pojo.`var`

import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "变量组版本处理信息")
class VarGroupVersionChangeInfo(
    @get:Schema(title = "变量组名称")
    val groupName: String,
    @get:Schema(title = "版本号")
    val version: Int,
    @get:Schema(title = "关联引用ID")
    val referId: String,
    @get:Schema(title = "关联引用类型")
    val referType: PublicVerGroupReferenceTypeEnum,
    @get:Schema(title = "关联引用版本")
    val referVersion: Int,
    @get:Schema(title = "待删除的引用记录")
    var referInfoToDelete: ResourcePublicVarGroupReferPO? = null,
    @get:Schema(title = "待新增的引用记录")
    var referInfoToAdd: ResourcePublicVarGroupReferPO? = null
) {
    /**
     * Returns true if there are actual changes (delete or add operation exists).
     */
    fun hasChanges(): Boolean = referInfoToDelete != null || referInfoToAdd != null
}
