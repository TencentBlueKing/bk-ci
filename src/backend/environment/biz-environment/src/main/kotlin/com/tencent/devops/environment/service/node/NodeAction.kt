package com.tencent.devops.environment.service.node

import com.tencent.devops.model.environment.tables.records.TNodeRecord
import javax.annotation.PostConstruct

/**
 * Node节点操作接口
 */
interface NodeAction {

    fun type(): NodeActionFactory.Action

    @PostConstruct
    fun init() {
        NodeActionFactory.register(type = type(), nodeAction = this)
    }

    fun action(nodeRecords: List<TNodeRecord>)
}