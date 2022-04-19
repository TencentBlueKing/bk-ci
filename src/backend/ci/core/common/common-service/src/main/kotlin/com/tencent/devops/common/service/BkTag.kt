package com.tencent.devops.common.service

import com.tencent.devops.common.service.utils.KubernetesUtils

class BkTag constructor(
    private val consulTag: String
) {
    fun getTag(): String {
        if (KubernetesUtils.inContainer()) {
            return "kubernetes-" + KubernetesUtils.getNamespace()
        }
        return consulTag
    }
}
