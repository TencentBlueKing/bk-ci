package com.devops.process.yaml.v2.models.on

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class ReviewRule {
    val types: List<String>? = null
    val states: List<String>? = null
}
