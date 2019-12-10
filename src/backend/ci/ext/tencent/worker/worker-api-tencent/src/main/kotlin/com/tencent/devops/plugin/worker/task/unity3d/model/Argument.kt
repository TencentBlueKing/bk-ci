package com.tencent.devops.plugin.worker.task.unity3d.model

import java.io.File
import com.tencent.devops.common.pipeline.enums.Platform

data class Argument(
    val platform: Platform,
    val executeMethod: String?,
    val debug: Boolean,
    val rootDir: File,
    val androidKey: AndroidKey,
    val androidAPKPath: String,
    val androidAPKName: String,
    val xcodeProjectName: String,
    val enableBitCode: Boolean? = null,
    var version: String = ""
)