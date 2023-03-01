package com.tencent.devops.turbo.model.pojo

import org.springframework.data.mongodb.core.mapping.Field

data class EngineSceneEntity(
    /**
     * 场景名称
     */
    @Field("scene_name")
    val sceneName: String,

    /**
     * 加速场景 enum EnumEngineScene
     */
    @Field("scene_code")
    val sceneCode: String,

    /**
     * 加速次数
     */
    @Field("execute_count")
    var executeCount: Int = 0

) {
    fun incCount() {
        this.executeCount++
    }
}
