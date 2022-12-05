package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.artifact.event.packages.VersionCreatedEvent
import com.tencent.bkrepo.common.artifact.event.packages.VersionDeletedEvent
import com.tencent.bkrepo.common.artifact.event.packages.VersionDownloadEvent
import com.tencent.bkrepo.common.artifact.event.packages.VersionUpdatedEvent
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionUpdateRequest

/**
 * 包版本事件构造类
 */
object PackageEventFactory {

    /**
     * 包版本创建事件
     * [realIpAddress]: 由调用方传递真实的请求来源IP, 否则记录的是微服务调用机器的IP
     */
    fun buildCreatedEvent(request: PackageVersionCreateRequest, realIpAddress: String?): VersionCreatedEvent {
        with(request) {
            return VersionCreatedEvent(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                packageVersion = versionName,
                userId = createdBy,
                packageType = packageType.name,
                packageName = packageName,
                realIpAddress = realIpAddress
            )
        }
    }

    /**
     * 包版本更新事件-upload 覆盖上传
     */
    fun buildUpdatedEvent(request: PackageVersionCreateRequest, realIpAddress: String?): VersionUpdatedEvent {
        with(request) {
            return VersionUpdatedEvent(
                projectId = projectId,
                repoName = repoName,
                packageType = packageType.name,
                packageKey = packageKey,
                packageName = packageName,
                packageVersion = versionName,
                userId = createdBy,
                realIpAddress = realIpAddress
            )
        }
    }

    /**
     * 包版本更新事件-update 请求
     */
    fun buildUpdatedEvent(
        request: PackageVersionUpdateRequest,
        packageType: String,
        packageName: String,
        createdBy: String,
        realIpAddress: String?
    ): VersionUpdatedEvent {
        with(request) {
            return VersionUpdatedEvent(
                projectId = projectId,
                repoName = repoName,
                packageType = packageType,
                packageKey = packageKey,
                packageName = packageName,
                packageVersion = versionName,
                userId = createdBy,
                realIpAddress = realIpAddress
            )
        }
    }

    /**
     * 包版本下载事件
     */
    fun buildDownloadEvent(
        projectId: String,
        repoName: String,
        packageType: PackageType,
        packageKey: String,
        packageName: String,
        versionName: String,
        createdBy: String,
        realIpAddress: String?
    ): VersionDownloadEvent {
        return VersionDownloadEvent(
            projectId = projectId,
            repoName = repoName,
            packageType = packageType.name,
            packageKey = packageKey,
            packageName = packageName,
            packageVersion = versionName,
            userId = createdBy,
            realIpAddress = realIpAddress
        )
    }

    /**
     * 包版本删除事件
     */
    fun buildDeletedEvent(
        projectId: String,
        repoName: String,
        packageType: PackageType,
        packageKey: String,
        packageName: String,
        versionName: String?,
        createdBy: String,
        realIpAddress: String?
    ): VersionDeletedEvent {
        return VersionDeletedEvent(
            projectId = projectId,
            repoName = repoName,
            packageType = packageType.name,
            packageKey = packageKey,
            packageName = packageName,
            packageVersion = versionName,
            userId = createdBy,
            realIpAddress = realIpAddress
        )
    }
}
