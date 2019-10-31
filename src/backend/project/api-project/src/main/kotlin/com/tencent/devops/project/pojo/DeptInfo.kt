package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * {
"TypeId": "6",
"LeaderId": "73",
"Name": "IEG互动娱乐事业群",
"Level": "1",
"Enabled": "true",
"SecretaryId": "5095",
"TypeName": "20 系统",
"VicePresidentId": "73",
"ParentId": "0",
"ExProperties": "",
"ExchangeGroupName": " ",
"ID": "956"
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