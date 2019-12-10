package com.tencent.devops.plugin.worker.task.xcode

/**
 * Created by schellingma on 2017/03/13.
 * Powered By Tencent
 */
data class Argument(
    val project: String,
    val sdk: String,
    val scheme: String,
    val certId: String,
    val configuration: String,
    val iosOutPath: String,
    val rootDir: String,
    val enableBitCode: Boolean,
    val extra: String
)