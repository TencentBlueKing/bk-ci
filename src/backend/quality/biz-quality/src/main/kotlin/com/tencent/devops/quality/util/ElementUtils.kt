package com.tencent.devops.quality.util

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource

object ElementUtils {

    fun getElementCnName(classType: String, projectId: String): String {
        val map = getProjectElement(projectId)
        return map[classType] ?: ""
    }

    fun getProjectElement(projectId: String): Map<String/* atomCode */, String/* cnName */> {
        val client = SpringContextUtil.getBean(Client::class.java)
        return client.get(ServiceMarketAtomResource::class).getProjectElements(projectId).data!!
    }
}