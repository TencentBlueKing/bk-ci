package com.tencent.devops.common.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class ActionPolicyRes(
    @JsonProperty("policy_id")
    val policyId: Int,
    val action: ActionRes
)