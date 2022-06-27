package com.tencent.devops.common.expression.expression

class EvaluationOptions() {

    constructor(copy: EvaluationOptions?) : this() {
        if (copy != null) {
            maxMemory = copy.maxMemory
        }
    }

    var maxMemory: Int = 0
}
