package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 单向网络状态枚举
 */
@Schema(description = "单向网络状态")
enum class CdsMeshStatus(
    val value: Int,
    @get:Schema(title = "状态描述")
    val description: String
) {
    /** 未启用单向网络或在黑名单中 */
    DISABLED(0, "未启用单向网络"),
    
    /** Mesh单向网络模式 */
    MESH(1, "Mesh单向网络"),
    
    /** SSL单向网络模式 */
    SSL(2, "SSL单向网络");

    companion object {
        /**
         * 根据数值获取对应的状态
         */
        fun fromValue(value: Int): CdsMeshStatus {
            return values().find { it.value == value } ?: DISABLED
        }
    }
    
    /**
     * 是否为单向网络（Mesh 或 SSL）
     */
    fun isSingleNetwork(): Boolean = this == MESH || this == SSL
}
