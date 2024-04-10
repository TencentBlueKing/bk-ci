package com.tencent.devops.process.service.pipelineExport.pojo

import com.fasterxml.jackson.annotation.JsonIgnore

data class RunAtomParam(
    val shell: String? = null,
    val script: String? = null,
    val charsetType: CharsetType? = null
) {
    enum class CharsetType {
        /*默认类型*/
        DEFAULT,

        /*UTF_8*/
        UTF_8,

        /*GBK*/
        GBK
    }

    @JsonIgnore
    fun getWith(): Map<String, Any> {
        val res = mutableMapOf<String, Any>()
        if (charsetType != CharsetType.DEFAULT) {
            res["charsetType"] = charsetType?.name ?: ""
        }
        return res
    }
}
