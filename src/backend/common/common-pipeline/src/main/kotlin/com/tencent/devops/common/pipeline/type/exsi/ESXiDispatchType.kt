package com.tencent.devops.common.pipeline.type.exsi

import com.tencent.devops.common.pipeline.type.DispatchType
// value is empty
class ESXiDispatchType : DispatchType("") {
    override fun replaceField(variables: Map<String, String>) {
    }
}