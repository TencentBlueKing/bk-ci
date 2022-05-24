package com.tencent.bk.codecc.apiquery

import com.tencent.devops.common.service.MicroServiceApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling


@EnableScheduling
@SpringBootApplication(exclude = [
    MongoAutoConfiguration::class,
    MongoDataAutoConfiguration::class
])
open class Application

fun main(args: Array<String>) {
    MicroServiceApplication.run(Application::class, args)
}