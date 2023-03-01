package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModelProperty

data class EngineSceneVO(

    @ApiModelProperty("场景名称")
    val sceneName: String,

    @ApiModelProperty("加速场景 enum EnumEngineScene")
    val sceneCode: String,

    @ApiModelProperty("加速次数")
    val executeCount: Int
)
