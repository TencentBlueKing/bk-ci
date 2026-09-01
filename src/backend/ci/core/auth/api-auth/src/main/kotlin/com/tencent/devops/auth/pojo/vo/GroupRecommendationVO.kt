package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组推荐结果")
data class GroupRecommendationVO(
    @get:Schema(title = "推荐建议", required = true)
    val recommendation: String,
    @get:Schema(title = "候选用户组列表", required = true)
    val candidateGroups: List<CandidateGroupVO>
)
