package com.tencent.devops.store.common.service.impl

import com.tencent.devops.store.common.service.AbstractComponentVersionService
import org.springframework.stereotype.Service

@Service("TRIGGER_EVENT_VERSION_SERVICE")
class TriggerEventVersionService : AbstractComponentVersionService() {

    override fun convertVersion(version: String): String {
        val index = version.indexOf(".")
        val versionPrefix = version.substring(0, index + 1)
        return "V$versionPrefix"
    }
}