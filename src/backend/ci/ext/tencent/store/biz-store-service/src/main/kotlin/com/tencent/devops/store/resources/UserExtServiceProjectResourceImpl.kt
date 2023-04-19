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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceProjectResource
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.InstallExtServiceReq
import com.tencent.devops.store.pojo.vo.ExtServiceRespItem
import com.tencent.devops.store.service.ExtServiceProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceProjectResourceImpl @Autowired constructor(
    val extServiceProjectService: ExtServiceProjectService
) : UserExtServiceProjectResource {
    override fun installImage(
        accessToken: String,
        userId: String,
        installExtServiceReq: InstallExtServiceReq
    ): Result<Boolean> {
        return extServiceProjectService.installService(
            userId = userId,
            projectCodeList = installExtServiceReq.projectCodeList,
            serviceCode = installExtServiceReq.serviceCode,
            channelCode = ChannelCode.BS
        )
    }

    override fun getInstalledProjects(
        accessToken: String,
        userId: String,
        serviceCode: String
    ): Result<List<InstalledProjRespItem>> {
        return extServiceProjectService.getInstalledProjects(
            userId = userId,
            accessToken = accessToken,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE
        )
    }

    override fun getServiceByInstalledProject(
        accessToken: String,
        userId: String,
        projectCode: String
    ): Result<List<ExtServiceRespItem>> {
        return extServiceProjectService.getServiceByProjectCode(projectCode)
    }

    override fun unInstallService(
        accessToken: String,
        userId: String,
        serviceCode: String,
        projectCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {
        return extServiceProjectService.uninstallService(userId, projectCode, serviceCode, unInstallReq)
    }
}
