package com.tencent.devops.process.engine.atom.vm.parser

import com.tencent.devops.common.pipeline.type.DispatchType

/**
 * @Description
 * @Date 2019/11/17
 * @Version 1.0
 */
interface DispatchTypeParser {
    fun parse(userId: String, projectId: String, dispatchType: DispatchType)
}