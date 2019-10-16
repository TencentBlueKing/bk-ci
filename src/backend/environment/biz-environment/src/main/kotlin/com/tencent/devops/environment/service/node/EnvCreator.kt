package com.tencent.devops.environment.service.node

import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvironmentId
import javax.annotation.PostConstruct

/**
 * Node节点创建操作接口
 */
interface EnvCreator {

    fun id(): String

    @PostConstruct
    fun init() {
        EnvCreatorFactory.register(creatorId = id(), envCreator = this)
    }

    fun createEnv(projectId: String, userId: String, envCreateInfo: EnvCreateInfo): EnvironmentId
}