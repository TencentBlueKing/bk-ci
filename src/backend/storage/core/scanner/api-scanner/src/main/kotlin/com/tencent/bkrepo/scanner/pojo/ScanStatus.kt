package com.tencent.bkrepo.scanner.pojo

/**
 * 扫描状态
 */
@Deprecated(
    "仅用于兼容旧接口",
    replaceWith = ReplaceWith("ScanTaskStatus")
)
enum class ScanStatus {
    INIT,
    RUNNING,
    STOP,
    SUCCESS,
    FAILED
}
