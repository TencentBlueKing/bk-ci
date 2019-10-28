package com.tencent.devops.environment.service.node

import java.util.concurrent.ConcurrentHashMap

object EnvCreatorFactory {

    private val cache = ConcurrentHashMap<String, EnvCreator>()

    fun register(creatorId: String, envCreator: EnvCreator) {
        cache[creatorId] = envCreator
    }

    fun load(creatorId: String) = cache[creatorId]
}