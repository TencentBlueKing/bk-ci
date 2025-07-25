/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.common.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.BuildPublishersResource
import com.tencent.devops.store.pojo.common.publication.PublishersRequest
import com.tencent.devops.store.pojo.common.platform.StoreDockingPlatformRequest
import com.tencent.devops.store.common.service.PublishersDataService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildPublishersResourceImpl @Autowired constructor(
    private val publishersDataService: PublishersDataService
) : BuildPublishersResource {

    @SensitiveApiPermission("syn_publisher_data")
    override fun synAddPublisherData(userId: String, publishers: List<PublishersRequest>): Result<Int> {
        return Result(data = publishersDataService.createPublisherData(userId, publishers))
    }

    @SensitiveApiPermission("syn_publisher_data")
    override fun synDeletePublisherData(userId: String, publishers: List<PublishersRequest>): Result<Int> {
        return Result(data = publishersDataService.deletePublisherData(userId, publishers))
    }

    @SensitiveApiPermission("syn_publisher_data")
    override fun synUpdatePublisherData(userId: String, publishers: List<PublishersRequest>): Result<Int> {
        return Result(data = publishersDataService.updatePublisherData(userId, publishers))
    }

    @SensitiveApiPermission("syn_platforms_data")
    override fun synAddPlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(data = publishersDataService.savePlatformsData(userId, storeDockingPlatformRequests))
    }

    @SensitiveApiPermission("syn_platforms_data")
    override fun synDeletePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(data = publishersDataService.deletePlatformsData(userId, storeDockingPlatformRequests))
    }

    @SensitiveApiPermission("syn_platforms_data")
    override fun synUpdatePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(data = publishersDataService.savePlatformsData(userId, storeDockingPlatformRequests))
    }

    @SensitiveApiPermission("syn_platforms_data")
    override fun synUpdatePlatformsLogoInfo(userId: String, platformCode: String, logoUrl: String): Result<Boolean> {
        return Result(data = publishersDataService.updatePlatformsLogoInfo(userId, platformCode, logoUrl))
    }
}
