package com.tencent.devops.environment.service.node

import java.util.concurrent.ConcurrentHashMap

object NodeActionFactory {

    enum class Action {
        CREATE, DELETE, EDIT
    }

    private val cache = ConcurrentHashMap<Action, NodeAction>()

    fun register(type: Action, nodeAction: NodeAction) {
        cache[type] = nodeAction
    }

    fun load(type: Action) = cache[type]
}