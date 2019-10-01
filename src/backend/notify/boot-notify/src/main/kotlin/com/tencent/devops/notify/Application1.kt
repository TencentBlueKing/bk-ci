package com.tencent.devops.notify

import com.tencent.devops.common.service.MicroService
import com.tencent.devops.common.service.MicroServiceApplication

@MicroService
class Application1

fun main(args: Array<String>) {
    MicroServiceApplication.run(Application::class, args)
}