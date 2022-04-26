package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("V2-体验列表")
data class ExperienceList(
    @ApiModelProperty("内部体验列表")
    val privateExperiences: List<AppExperience>,
    @ApiModelProperty("公开体验列表")
    val publicExperiences: List<AppExperience>,
    @ApiModelProperty("红点个数")
    val redPointCount: Int
)
