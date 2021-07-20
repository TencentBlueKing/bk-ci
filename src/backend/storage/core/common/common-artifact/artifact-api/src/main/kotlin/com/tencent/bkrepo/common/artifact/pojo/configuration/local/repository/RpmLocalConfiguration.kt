package com.tencent.bkrepo.common.artifact.pojo.configuration.local.repository

import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration

/**
 * RPM仓库个性化属性: repodataDepth  索引目录深度
 *                  enabledFileLists  是否启用filelsit
 *                  groupXmlSet     分组设置
 */
class RpmLocalConfiguration(
    val repodataDepth: Int? = 0,
    val enabledFileLists: Boolean? = false,
    val groupXmlSet: MutableSet<String>? = mutableSetOf()
) : LocalConfiguration() {
    companion object {
        const val type = "rpm-local"
    }
}
