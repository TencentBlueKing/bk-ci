package com.tencent.devops.common.pipeline.type.esxi

import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchType

// value is empty
class ESXiDispatchType : DispatchType("") {
    override fun cleanDataBeforeSave() = Unit

    override fun replaceField(variables: Map<String, String>) = Unit

    override fun buildType() = BuildType.valueOf(BuildType.ESXi.name)
}
