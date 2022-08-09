package com.tencent.devops.store.resources.common

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.ServicePublishersResource
import com.tencent.devops.store.pojo.common.PublishersRequest
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest
import com.tencent.devops.store.service.common.PublishersDataService
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePublishersResourceImpl @Autowired constructor(
    private val publishersDataService: PublishersDataService
): ServicePublishersResource {
    override fun synAddPublisherData(userId: String, publishers: List<PublishersRequest>): Result<Int> {
        return Result(publishersDataService.createPublisherData(userId, publishers))
    }

    override fun synDeletePublisherData(userId: String, publishers: List<PublishersRequest>): Result<Int> {
        return Result(publishersDataService.deletePublisherData(userId, publishers))
    }

    override fun synUpdatePublisherData(userId: String, publishers: List<PublishersRequest>): Result<Int> {
        return Result(publishersDataService.updatePublisherData(userId, publishers))
    }

    override fun synAddPlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(publishersDataService.createPlatformsData(userId, storeDockingPlatformRequests))
    }

    override fun synDeletePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(publishersDataService.deletePlatformsData(userId, storeDockingPlatformRequests))
    }

    override fun synUpdatePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(publishersDataService.updatePlatformsData(userId, storeDockingPlatformRequests))
    }

}