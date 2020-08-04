package com.tencent.bk.codecc.apiquery

import com.tencent.devops.common.service.MicroServiceApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration
import org.springframework.cloud.netflix.rx.RxJavaAutoConfiguration

@SpringBootApplication(exclude = [(ArchaiusAutoConfiguration::class), (RxJavaAutoConfiguration::class), (MongoAutoConfiguration::class), (MongoDataAutoConfiguration::class)])
open class Application

fun main(args: Array<String>) {
    MicroServiceApplication.run(Application::class, args)
}