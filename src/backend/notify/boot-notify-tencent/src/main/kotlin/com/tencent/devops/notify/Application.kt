package com.tencent.devops.notify

import com.tencent.devops.common.service.MicroService
import com.tencent.devops.common.service.MicroServiceApplication
import org.springframework.context.annotation.ComponentScan

@MicroService
@ComponentScan("com.tencent.devops.notify")
class Application

fun main(args: Array<String>) {
    MicroServiceApplication.run(Application::class, args)
}
