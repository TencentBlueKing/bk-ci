package com.tencent.bkrepo.scanner.pojo

/**
 * 漏洞类型
 */
@Deprecated(message = "用于兼容旧接口")
enum class LeakType(val value: String) {
    CRITICAL("危急"),
    HIGH("高危"),
    LOW("低危"),
    MEDIUM("中危");
}
