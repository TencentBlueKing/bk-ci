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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.pojo.service.ServiceVO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ServiceProjectService @Autowired constructor(
    private val projectServiceDao: ServiceDao,
    private val dslContext: DSLContext
) {
    fun getServiceList(): Result<List<ServiceVO>> {
        val serviceList = mutableListOf<ServiceVO>()
        val serviceRecodes = projectServiceDao.getServiceList(dslContext)
        if (serviceRecodes != null) {
            for (serviceRecode in serviceRecodes) {
                serviceList.add(
                    ServiceVO(
                        id = serviceRecode.id ?: 0,
                        name = MessageCodeUtil.getMessageByLocale(serviceRecode.name, serviceRecode.englishName),
                        link = serviceRecode.link ?: "",
                        linkNew = serviceRecode.linkNew ?: "",
                        status = serviceRecode.status,
                        injectType = serviceRecode.injectType ?: "",
                        iframeUrl = serviceRecode.iframeUrl ?: "",
                        grayIframeUrl = serviceRecode.grayIframeUrl ?: "",
                        cssUrl = serviceRecode.cssUrl ?: "",
                        jsUrl = serviceRecode.jsUrl ?: "",
                        grayCssUrl = serviceRecode.grayCssUrl ?: "",
                        grayJsUrl = serviceRecode.grayJsUrl ?: "",
                        showProjectList = serviceRecode.showProjectList ?: true,
                        showNav = serviceRecode.showNav ?: true,
                        projectIdType = serviceRecode.projectIdType ?: "",
                        collected = true,
                        weigHt = serviceRecode.weight ?: 0,
                        logoUrl = serviceRecode.logoUrl ?: "",
                        webSocket = serviceRecode.webSocket ?: ""
                    )
                )
            }
        }
        return Result(serviceList)
    }
}
