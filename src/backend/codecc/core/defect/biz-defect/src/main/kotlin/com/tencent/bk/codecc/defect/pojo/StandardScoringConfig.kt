package com.tencent.bk.codecc.defect.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class StandardScoringConfig(
        @JsonProperty("tool_name")
        val toolNameList: MutableList<String> = mutableListOf(),

        val coefficient: Double = 1.toDouble(),

        @JsonProperty("cloc_language")
        val clocLanguage: MutableList<String> = mutableListOf(),

        var lineCount: Long = 0L
)
