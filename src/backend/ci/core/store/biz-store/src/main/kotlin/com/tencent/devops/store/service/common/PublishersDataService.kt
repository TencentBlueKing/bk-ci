package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.PublisherInfo
import com.tencent.devops.store.pojo.common.PublishersRequest
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

interface PublishersDataService {

    fun createPublisherData(userId: String, publishers: List<PublishersRequest>): Int

    fun deletePublisherData(userId: String, publishers: List<PublishersRequest>): Int

    fun updatePublisherData(userId: String, publishers: List<PublishersRequest>): Int

    fun createPlatformsData(userId: String, storeDockingPlatformRequests: List<StoreDockingPlatformRequest>): Int

    fun deletePlatformsData(userId: String, storeDockingPlatformRequests: List<StoreDockingPlatformRequest>): Int

    fun updatePlatformsData(userId: String, storeDockingPlatformRequests: List<StoreDockingPlatformRequest>): Int

    fun getPublishers(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<List<PublisherInfo>>
}