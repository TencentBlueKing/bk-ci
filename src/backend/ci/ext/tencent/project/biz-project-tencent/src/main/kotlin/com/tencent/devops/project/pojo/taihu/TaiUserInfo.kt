package com.tencent.devops.project.pojo.taihu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaiUserInfo(
    val username: String,
    @JsonProperty("account_name")
    val accountName: String,
    @JsonProperty("account_email")
    val accountEmail: String,
    @JsonProperty("company_tags")
    val companyTags: List<CompanyTags>,
    val departments: List<DepartmentsInfo>?
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CompanyTags(
        @JsonProperty("tag_id")
        val tagId: String,
        @JsonProperty("tag_name")
        val tagName: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DepartmentsInfo(
        val id: Long,
        val name: String
    )
}
