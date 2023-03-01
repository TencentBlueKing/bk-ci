package com.tencent.devops.turbo.enums

/**
 * 三种引擎模式，其中 disttask-ue4 细分为：代码编译、shader加速
 */
enum class EnumEngineScene(
    private val sceneName: String,
    private val sceneCode: String,
    private val sceneRegex: String
) {
    DISTTASKCC("Linux-C/C++加速", "disttask-cc", "cc"),
    UE4COMPILE("UE4加速 - 代码编译", "disttask-ue4", "cl.exe"),
    UE4SHADER("UE4加速 - shader加速", "disttask-ue4", "ShaderCompileWorker.exe"),
    DISTCC("distcc", "distcc", "distcc");


    fun getName(): String {
        return this.sceneName
    }

    fun getCode(): String {
        return this.sceneCode
    }

    fun regexStr(): String {
        return this.sceneRegex
    }
}
