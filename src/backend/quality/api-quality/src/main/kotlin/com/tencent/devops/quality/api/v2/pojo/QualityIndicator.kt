package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation

data class QualityIndicator(
    val hashId: String,
    val elementType: String,
    val elementDetail: String,
    val enName: String,
    val cnName: String,
    val stage: String,
    val operation: QualityOperation,
    val operationList: List<QualityOperation>,
    val threshold: String,
    val thresholdType: QualityDataType,
    val readOnly: Boolean,
    val type: String,
    val tag: String?,
    val metadataList: List<Metadata>,
    val desc: String?,
    val logPrompt: String
) {
    data class Metadata(
        val hashId: String,
        val name: String, // 中文名
        val enName: String // 英文名
    )

    companion object {
        val SCRIPT_ELEMENT = setOf(LinuxScriptElement.classType, WindowsScriptElement.classType)
    }

    fun isScriptElementIndicator(): Boolean {
        return elementType in SCRIPT_ELEMENT
    }
}
