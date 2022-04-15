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

package com.tencent.devops.stream.trigger.parsers.modelCreate

import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput
import com.tencent.devops.process.yaml.modelCreate.inner.TXInnerModelCreator
import com.tencent.devops.stream.dao.GitCIServicesConfDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class TXInnerModelCreatorImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamTriggerCache: StreamTriggerCache,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val gitServicesConfDao: GitCIServicesConfDao
) : TXInnerModelCreator, InnerModelCreatorImpl(
    dslContext, streamTriggerCache, streamBasicSettingDao
) {

    override fun getServiceJobDevCloudInput(
        image: String,
        imageName: String,
        imageTag: String,
        params: String
    ): ServiceJobDevCloudInput {
        val record = gitServicesConfDao.get(dslContext, imageName, imageTag)
            ?: throw RuntimeException("Git CI没有此镜像版本记录. $image")
        if (!record.enable) {
            throw RuntimeException("镜像版本不可用")
        }

        return ServiceJobDevCloudInput(
            image = image,
            registryHost = record.repoUrl,
            registryUsername = record.repoUsername,
            registryPassword = record.repoPwd,
            params = params,
            serviceEnv = record.env
        )
    }
}
