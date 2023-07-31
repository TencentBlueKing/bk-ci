package com.tencent.devops.store.pojo.atom

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件信息")
data class AtomListInfo(
    @ApiModelProperty("插件标识")
    val atomCode: String,
    @ApiModelProperty("名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @ApiModelProperty("版本号")
    val version: String,
    @ApiModelProperty(
        "插件状态，INIT：初始化|COMMITTING：提交中|BUILDING：构建中|BUILD_FAIL：构建失败|TESTING：测试中|" +
                "AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|" +
                "UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架",
        required = true
    )
    val atomStatus: String,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("创建时间")
    val createTime: String
)