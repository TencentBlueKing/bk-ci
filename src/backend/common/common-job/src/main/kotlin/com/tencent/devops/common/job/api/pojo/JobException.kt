package com.tencent.devops.common.job.api.pojo

import org.springframework.stereotype.Component

@Component
class JobException : Exception {
    constructor(message: String) : super(message)

    constructor(message: String, t: Throwable) : super(message, t)

    companion object {
        private const val serialVersionUID = 1L
    }
}