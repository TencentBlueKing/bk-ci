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

package com.tencent.devops.project.resources

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.pojo.enums.GatewayType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OPProjectCallbackResource
import com.tencent.devops.project.dao.ProjectCallbackDao
import com.tencent.devops.project.enum.ProjectEventType
import com.tencent.devops.project.pojo.ProjectCallbackPojo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.secret.ISecretParam
import com.tencent.devops.project.service.UrlGenerator
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@Suppress("ALL")
@RestResource
class OPProjectCallbackResourceImpl @Autowired constructor(
    val projectCallbackDao: ProjectCallbackDao,
    val dslContext: DSLContext,
    val urlGenerator: UrlGenerator
) : OPProjectCallbackResource {

    @Value("\${project.callback.secretParam.aes-key}")
    private val aesKey = "C/R%3{?OS}IeGT21"

    override fun create(
        userId: String,
        event: ProjectEventType,
        gatewayType: GatewayType,
        secretParam: ISecretParam
    ): Result<Boolean> {
        projectCallbackDao.create(
            dslContext = dslContext,
            event = event.name,
            url = urlGenerator.generate(gatewayType, secretParam.url),
            secretParam = JsonUtil.toJson(secretParam.encode(aesKey), false),
            secretType = secretParam.getSecretType()
        )
        return Result(true)
    }

    override fun delete(
        userId: String,
        id: Int
    ): Result<Boolean> {
        logger.info("start delete callback: userId[$userId]|id[$id]")
        val changeCount = projectCallbackDao.delete(
            dslContext = dslContext,
            id = id
        )
        logger.info("delete callback changeCount[$changeCount]")
        return Result(true)
    }

    override fun list(userId: String, event: String, callbackUrl: String?): Result<List<ProjectCallbackPojo>> {
        val list = projectCallbackDao.get(
            dslContext = dslContext,
            event = event,
            url = callbackUrl
        ).map {
            ProjectCallbackPojo(
                event = it.event,
                callbackUrl = it.callbackUrl,
                secretType = it.secretType
            )
        }
        return Result(list)
    }

    companion object {
        val logger = LoggerFactory.getLogger(OPProjectCallbackResourceImpl::class.java)
    }
}
