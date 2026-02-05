package com.tencent.devops.process.pojo.`var`

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 公共变量组键
 * 用于唯一标识一个变量组的名称和版本组合
 * 提供字符串格式转换功能，格式："groupName:version"
 */
@Schema(title = "公共变量组键")
data class PublicGroupKey(
    @get:Schema(title = "变量组名称", required = true)
    val groupName: String,
    @get:Schema(title = "变量组版本", description = "版本号，null表示使用最新版本，-1表示动态版本")
    val version: Int?
) {

    /**
     * 获取用于数据库存储的版本号
     * version为null时返回-1
     */
    fun getVersionForDb(): Int = version ?: -1
}
