package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * {
"TypeId": "0",
"LeaderId": "0",
"Name": "xxxx",
"Level": "1",
"Enabled": "true",
"SecretaryId": "0",
"TypeName": "20 系统",
"VicePresidentId": "0",
"ParentId": "0",
"ExProperties": "",
"ExchangeGroupName": " ",
"ID": "0"
}
 */
data class DeptInfo(
    @JsonProperty("TypeId")
    val typeId: String,
    @JsonProperty("LeaderId")
    val leaderId: String,
    @JsonProperty("Name")
    val name: String,
    @JsonProperty("Level")
    val level: String,
    @JsonProperty("Enabled")
    val enabled: String,
    @JsonProperty("ParentId")
    val parentId: String,
    @JsonProperty("ID")
    val id: String

)