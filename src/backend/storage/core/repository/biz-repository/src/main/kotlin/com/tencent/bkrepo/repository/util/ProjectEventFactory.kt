package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.artifact.event.project.ProjectCreatedEvent
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest

/**
 * 项目事件构造类
 */
object ProjectEventFactory {

    /**
     * 项目创建事件
     */
    fun buildCreatedEvent(request: ProjectCreateRequest): ProjectCreatedEvent {
        with(request) {
            return ProjectCreatedEvent(
                projectId = name,
                userId = operator
            )
        }
    }
}
