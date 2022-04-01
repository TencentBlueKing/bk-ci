package com.tencent.devops.common.ci.v2

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class NoteRule {
    val types: List<String>? = null
    val comment: List<String>? = null
}
