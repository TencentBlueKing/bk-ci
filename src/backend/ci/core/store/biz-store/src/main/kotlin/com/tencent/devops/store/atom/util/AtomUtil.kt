package com.tencent.devops.store.atom.util

import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum

object AtomUtil {

    /**
     * 当 serviceScope 为 CREATIVE_STREAM 时，将 docsLink 中的 /atom/ 替换为 /creative/
     */
    fun transformDocsLink(docsLink: String?, serviceScope: ServiceScopeEnum?): String? {
        if (docsLink.isNullOrBlank() || serviceScope != ServiceScopeEnum.CREATIVE_STREAM) {
            return docsLink
        }
        return docsLink.replace("/atom/", "/creative/")
    }
}
