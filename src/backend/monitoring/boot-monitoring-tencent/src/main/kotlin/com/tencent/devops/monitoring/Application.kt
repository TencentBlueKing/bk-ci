package com.tencent.devops.monitoring

import com.tencent.devops.common.service.MicroService
import com.tencent.devops.common.service.MicroServiceApplication

@MicroService
class Application

fun main(args: Array<String>) {
    MicroServiceApplication.run(Application::class, args)
}