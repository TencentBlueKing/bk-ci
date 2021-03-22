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

package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceResource
import com.tencent.devops.store.pojo.EditInfoDTO
import com.tencent.devops.store.pojo.ServiceBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.UpdateExtBaseInfo
import com.tencent.devops.store.pojo.dto.ExtSubmitDTO
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import com.tencent.devops.store.pojo.vo.ExtServiceMainItemVo
import com.tencent.devops.store.pojo.vo.SearchExtServiceVO
import com.tencent.devops.store.pojo.vo.ServiceVersionListItem
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import com.tencent.devops.store.service.ExtServiceBaseService
import com.tencent.devops.store.service.ExtServiceSearchService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceResourceImpl @Autowired constructor(
    val extServiceSearchService: ExtServiceSearchService,
    val extServiceBaseService: ExtServiceBaseService
) : UserExtServiceResource {
    override fun mainPageList(userId: String, page: Int?, pageSize: Int?): Result<List<ExtServiceMainItemVo>> {
        return extServiceSearchService.mainPageList(userId, page, pageSize)
    }

    override fun list(
        userId: String,
        keyword: String?,
        classifyCode: String?,
        labelCode: String?,
        bkServiceId: Long?,
        score: Int?,
        rdType: ServiceTypeEnum?,
        sortType: ExtServiceSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<SearchExtServiceVO> {
        return Result(extServiceSearchService.list(
            userId = userId,
            keyword = keyword,
            classifyCode = classifyCode,
            labelCode = labelCode,
            bkServiceId = bkServiceId,
            rdType = rdType,
            score = score,
            sortType = sortType,
            pageSize = pageSize,
            page = page
        ))
    }

    override fun getServiceByCode(userId: String, bk_ticket: String, serviceCode: String): Result<ServiceVersionVO?> {
        return extServiceBaseService.getServiceByCode(
            userId = userId,
            serviceCode = serviceCode
        )
    }

    override fun getServiceVersionsByCode(
        userId: String,
        serviceCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<ServiceVersionListItem>> {
        return extServiceBaseService.getServiceVersionListByCode(userId, serviceCode, page, pageSize)
    }

    override fun createMediaAndVisible(userId: String, serviceId: String, submitInfo: ExtSubmitDTO): Result<Boolean> {
        return extServiceBaseService.createMediaAndVisible(userId, serviceId, submitInfo)
    }

    override fun createMediaAndVisible(userId: String, serviceId: String): Result<Boolean> {
        return extServiceBaseService.backToTest(userId, serviceId)
    }

    override fun updateServiceBaseInfo(
        userId: String,
        serviceCode: String,
        serviceId: String,
        serviceBaseInfoUpdateRequest: ServiceBaseInfoUpdateRequest
    ): Result<Boolean> {
        val baseInfo = UpdateExtBaseInfo(
            serviceName = serviceBaseInfoUpdateRequest.serviceName,
            labels = serviceBaseInfoUpdateRequest.labelIdList,
            itemIds = serviceBaseInfoUpdateRequest.extensionItemIdList,
            summary = serviceBaseInfoUpdateRequest.summary,
            logoUrl = serviceBaseInfoUpdateRequest.logoUrl,
            iconData = serviceBaseInfoUpdateRequest.iconData,
            description = serviceBaseInfoUpdateRequest.description,
            descInputType = serviceBaseInfoUpdateRequest.descInputType
        )

        val editInfo = EditInfoDTO(
            baseInfo = baseInfo,
            mediaInfo = serviceBaseInfoUpdateRequest.mediaList,
            settingInfo = null
        )
        return extServiceBaseService.updateExtInfo(userId, serviceId, serviceCode, editInfo)
    }
}
