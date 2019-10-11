package com.tencent.devops.quality.api.v2.pojo.response

import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("指标列表页面响应")
data class IndicatorListResponse(
    @ApiModelProperty("脚本指标")
    val scriptIndicators: List<IndicatorListItem>,
    @ApiModelProperty("系统指标")
    val systemIndicators: List<IndicatorListItem>,
    @ApiModelProperty("研发商店指标")
    val marketIndicators: List<IndicatorListItem>
) {
    data class IndicatorListItem(
        val hashId: String,
        val name: String,
        val cnName: String,
        val elementType: String,
        val elementName: String,
        val elementDetail: String, // 对应页面的工具
        val metadatas: List<QualityMetadata>,
        val availableOperation: List<QualityOperation>,
        val dataType: QualityDataType,
        val threshold: String,
        val desc: String
    )

    data class QualityMetadata(
        val enName: String,
        val cnName: String,
        val detail: String,
        val type: QualityDataType,
        val msg: String,
        val extra: String?
    )
}
