package com.tencent.devops.turbo

import com.tencent.devops.boot.runApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@EnableRetry
@EnableFeignClients(basePackages = ["com.tencent.devops.turbo"])
class TurboApplication

fun main(args: Array<String>) {
    runApplication<TurboApplication>(args)
}
