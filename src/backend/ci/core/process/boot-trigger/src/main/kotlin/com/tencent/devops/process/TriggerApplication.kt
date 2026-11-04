package com.tencent.devops.process

import com.tencent.devops.common.service.MicroService
import com.tencent.devops.common.service.MicroServiceApplication
import org.springframework.context.annotation.ComponentScan

@MicroService
@ComponentScan("com.tencent.devops.plugin", "com.tencent.devops.process")
class TriggerApplication

fun main(args: Array<String>) {
    MicroServiceApplication.run(TriggerApplication::class, args)
}
