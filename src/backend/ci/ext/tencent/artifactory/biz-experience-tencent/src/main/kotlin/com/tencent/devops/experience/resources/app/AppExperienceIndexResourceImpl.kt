/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceIndexResource
import com.tencent.devops.experience.pojo.index.HotCategoryParam
import com.tencent.devops.experience.pojo.index.IndexAppInfoVO
import com.tencent.devops.experience.pojo.index.IndexBannerVO
import com.tencent.devops.experience.pojo.index.NewCategoryParam
import com.tencent.devops.experience.service.ExperienceIndexService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppExperienceIndexResourceImpl @Autowired constructor(
    val experienceIndexService: ExperienceIndexService
) : AppExperienceIndexResource {
    override fun banners(
        userId: String,
        platform: Int,
        page: Int,
        pageSize: Int
    ): Result<Pagination<IndexBannerVO>> {
        return experienceIndexService.banners(userId, page, pageSize, platform)
    }

    override fun hots(
        userId: String,
        platform: Int,
        page: Int,
        pageSize: Int,
        includeExternalUrl: Boolean?
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.hots(userId, page, pageSize, platform, includeExternalUrl)
    }

    override fun necessary(
        userId: String,
        platform: Int,
        page: Int,
        pageSize: Int,
        includeExternalUrl: Boolean?
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.necessary(userId, page, pageSize, platform, includeExternalUrl)
    }

    override fun newest(
        userId: String,
        platform: Int,
        page: Int,
        pageSize: Int,
        includeExternalUrl: Boolean?
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.newest(userId, page, pageSize, platform, includeExternalUrl)
    }

    override fun hotCategory(
        userId: String,
        platform: Int,
        hotCategoryParam: HotCategoryParam
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.hotCategory(userId, platform, hotCategoryParam)
    }

    override fun newCategory(
        userId: String,
        platform: Int,
        newCategoryParam: NewCategoryParam
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.newCategory(userId, platform, newCategoryParam)
    }

    override fun miniGameExperience(
        userId: String,
        platform: Int
    ): Result<List<IndexAppInfoVO>> {
        return experienceIndexService.miniGameExperience(userId, platform)
    }

    override fun miniGamePicture(userId: String): Result<String> {
        return experienceIndexService.showMiniGamePicture()
    }
}
