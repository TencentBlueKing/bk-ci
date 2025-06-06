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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAtomResourceV4
import com.tencent.devops.openapi.utils.ApigwParamUtil
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.api.common.ServiceStoreStatisticResource
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.statistic.StoreStatistic
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAtomResourceV4Impl @Autowired constructor(private val client: Client) :
    ApigwAtomResourceV4 {
    override fun getAtomByCode(
        appCode: String?,
        apigwType: String?,
        atomCode: String,
        userId: String
    ): Result<AtomVersion?> {
        logger.info("OPENAPI_ATOM_V4|$appCode|$userId|$atomCode|get Atom By Code")
        return client.get(ServiceMarketAtomResource::class).getAtomByCode(atomCode, userId)
    }

    override fun getAtomStatisticByCode(
        appCode: String?,
        apigwType: String?,
        atomCode: String,
        userId: String
    ): Result<StoreStatistic> {
        logger.info("OPENAPI_ATOM_V4|$appCode|$userId|$atomCode|get Atom Statistic By Code")
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
        logger.info("OPENAPI_ATOM_V4|$appCode|$userId|$atomCode|get Atom Pipelines By Code,$page,$pageSize")
        return client.get(ServiceMarketAtomResource::class).getAtomPipelinesByCode(
            atomCode = atomCode,
            username = userId,
            page = page ?: 1,
            pageSize = ApigwParamUtil.standardSize(pageSize) ?: 20
        )
    }

    override fun installAtom(
        appCode: String?,
        apigwType: String?,
        userId: String,
        channelCode: ChannelCode?,
        installAtomReq: InstallAtomReq
    ): Result<Boolean> {
        logger.info("OPENAPI_ATOM_V4|$appCode|$userId|install Atom: $channelCode, $installAtomReq")
        return client.get(ServiceMarketAtomResource::class).installAtom(userId, channelCode, installAtomReq)
    }

    override fun getAtomDetail(
        appCode: String?,
        apigwType: String?,
        atomCode: String,
        version: String,
        userId: String
    ): Result<PipelineAtom?> {
        logger.info("OPENAPI_ATOM_V4|$appCode|$userId|getAtomDetail: $atomCode, $version")
        return client.get(ServiceAtomResource::class).getAtomVersionInfo(atomCode, version)
    }

    override fun list(
        appCode: String?,
        apigwType: String?,
        userId: String,
        keyword: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: AtomTypeEnum?,
        yamlFlag: Boolean?,
        recommendFlag: Boolean?,
        qualityFlag: Boolean?,
        sortType: MarketAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<MarketAtomResp> {
        logger.info(
            "OPENAPI_ATOM_V4|$appCode|$userId|atom list: $keyword, $classifyCode," +
                " $labelCode, $score, $rdType, $yamlFlag, $recommendFlag, $qualityFlag, " +
                "$sortType, $page, $pageSize"
        )
        return client.get(ServiceAtomResource::class).list(
            userId = userId.trim(),
            keyword = keyword?.trim(),
            classifyCode = classifyCode?.trim(),
            labelCode = labelCode?.trim(),
            score = score,
            rdType = rdType,
            yamlFlag = yamlFlag,
            recommendFlag = recommendFlag,
            qualityFlag = qualityFlag,
            sortType = sortType,
            page = page,
            pageSize = pageSize
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwAtomResourceV4Impl::class.java)
    }
}
