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
package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwAtomResourceV3
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.api.common.ServiceStoreStatisticResource
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.common.StoreStatistic
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAtomResourceV3Impl @Autowired constructor(private val client: Client) :
    ApigwAtomResourceV3 {
    override fun getAtomByCode(
        appCode: String?,
        apigwType: String?,
        atomCode: String,
        userId: String
    ): Result<AtomVersion?> {
        logger.info("OPENAPI_ATOM_V3|$appCode|$userId|$atomCode|get Atom By Code")
        return client.get(ServiceMarketAtomResource::class).getAtomByCode(atomCode, userId)
    }

    override fun getAtomStatisticByCode(
        appCode: String?,
        apigwType: String?,
        atomCode: String,
        userId: String
    ): Result<StoreStatistic> {
        logger.info("OPENAPI_ATOM_V3|$appCode|$userId|$atomCode|get Atom Statistic By Code")
        return client.get(ServiceStoreStatisticResource::class).getStatisticByCode(
            userId = userId,
            storeType = StoreTypeEnum.ATOM,
            storeCode = atomCode
        )
    }

    override fun getAtomPipelinesByCode(
        appCode: String?,
        apigwType: String?,
        atomCode: String,
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AtomPipeline>> {
        logger.info("OPENAPI_ATOM_V3|$appCode|$userId|$atomCode|get Atom Pipelines By Code,$page,$pageSize")
        return client.get(ServiceMarketAtomResource::class).getAtomPipelinesByCode(
            atomCode = atomCode,
            username = userId,
            page = page ?: 1,
            pageSize = pageSize ?: 20
        )
    }

    override fun installAtom(
        appCode: String?,
        apigwType: String?,
        userId: String,
        channelCode: ChannelCode?,
        installAtomReq: InstallAtomReq
    ): Result<Boolean> {
        logger.info("OPENAPI_ATOM_V3|$appCode|$userId|install Atom: $channelCode, $installAtomReq")
        return client.get(ServiceMarketAtomResource::class).installAtom(userId, channelCode, installAtomReq)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwAtomResourceV3Impl::class.java)
    }
}
