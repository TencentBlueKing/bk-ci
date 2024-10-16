package com.tencent.devops.gpt.pojo

data class AIScoreRes(
    val goodUsers: Set<String>,
    val badUsers: Set<String>
)
