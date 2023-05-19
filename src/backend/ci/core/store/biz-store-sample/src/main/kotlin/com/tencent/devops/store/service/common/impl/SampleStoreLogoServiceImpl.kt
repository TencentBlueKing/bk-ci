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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.artifactory.api.service.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.utils.StoreUtils
import java.io.File
import org.springframework.stereotype.Service

@Service
class SampleStoreLogoServiceImpl : StoreLogoServiceImpl() {

    override fun uploadStoreLogo(userId: String, file: File): Result<String?> {
        val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
        val logoUrl = CommonUtils.serviceUploadFile(
            userId = userId,
            serviceUrlPrefix = serviceUrlPrefix,
            file = file,
            fileChannelType = FileChannelTypeEnum.WEB_SHOW.name,
            logo = true,
            language = I18nUtil.getLanguage(userId)
        ).data
        // 开源版如果logoUrl的域名和ci域名一样，则logoUrl无需带上域名，防止域名变更影响图片显示（logoUrl会存db）
        return Result(if (logoUrl != null) StoreUtils.removeUrlHost(logoUrl) else logoUrl)
    }
}
