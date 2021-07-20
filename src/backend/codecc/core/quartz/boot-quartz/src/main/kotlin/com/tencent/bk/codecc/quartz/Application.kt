package com.tencent.bk.codecc.quartz

import com.tencent.devops.common.service.MicroService
import com.tencent.devops.common.service.MicroServiceApplication
import org.springframework.scheduling.annotation.EnableScheduling

@MicroService
@EnableScheduling
open class Application

fun main(args: Array<String>) {
    MicroServiceApplication.run(Application::class, args)
}