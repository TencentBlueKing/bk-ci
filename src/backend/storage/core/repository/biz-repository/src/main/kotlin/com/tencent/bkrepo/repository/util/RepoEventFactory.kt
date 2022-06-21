package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.artifact.event.repo.RepoCreatedEvent
import com.tencent.bkrepo.common.artifact.event.repo.RepoDeletedEvent
import com.tencent.bkrepo.common.artifact.event.repo.RepoUpdatedEvent
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest

/**
 * 仓库事件构造类
 */
object RepoEventFactory {

    /**
     * 仓库创建事件
     */
    fun buildCreatedEvent(request: RepoCreateRequest): RepoCreatedEvent {
        with(request) {
            return RepoCreatedEvent(
                projectId = projectId,
                repoName = name,
                userId = operator
            )
        }
    }

    /**
     * 仓库更新事件
     */
    fun buildUpdatedEvent(request: RepoUpdateRequest): RepoUpdatedEvent {
        with(request) {
            return RepoUpdatedEvent(
                projectId = projectId,
                repoName = name,
                userId = operator
            )
        }
    }

    /**
     * 项目删除事件
     */
    fun buildDeletedEvent(request: RepoDeleteRequest): RepoDeletedEvent {
        with(request) {
            return RepoDeletedEvent(
                projectId = projectId,
                repoName = name,
                userId = operator
            )
        }
    }
}
