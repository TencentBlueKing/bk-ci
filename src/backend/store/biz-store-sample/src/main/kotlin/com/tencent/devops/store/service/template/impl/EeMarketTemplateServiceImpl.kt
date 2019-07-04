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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.pojo.atom.MarketMainItemLabel
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.enums.TemplateStatusEnum
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum
import com.tencent.devops.store.service.template.EeMarketTemplateService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Future

@Service
class EeMarketTemplateServiceImpl : EeMarketTemplateService, MarketTemplateServiceImpl() {

    private val logger = LoggerFactory.getLogger(EeMarketTemplateServiceImpl::class.java)

    /**
     * 模版市场，查询模版列表
     */
    override fun list(
        userId: String,
        name: String?,
        classifyCode: String?,
        category: String?,
        labelCode: String?,
        score: Int?,
        rdType: TemplateRdTypeEnum?,
        sortType: MarketTemplateSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): MarketTemplateResp {
        logger.info("[list]enter")

        return doList(
            name = name,
            classifyCode = classifyCode,
            category = category,
            labelCode = labelCode,
            score = score,
            rdType = rdType,
            sortType = sortType,
            desc = true,
            page = page,
            pageSize = pageSize
        ).get()
    }

    /**
     * 模版市场，首页
     */
    override fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<MarketTemplateMain>> {
        val result = mutableListOf<MarketTemplateMain>()
        val futureList = mutableListOf<Future<MarketTemplateResp>>()
        val labelInfoList = mutableListOf<MarketMainItemLabel>()
        labelInfoList.add(MarketMainItemLabel(LATEST, MessageCodeUtil.getCodeLanMessage(LATEST)))
        futureList.add(
            doList(
                name = null, classifyCode = null, category = null,
                labelCode = null, score = null, rdType = null, sortType = MarketTemplateSortTypeEnum.UPDATE_TIME,
                desc = true, page = page, pageSize = pageSize
            )
        )
        labelInfoList.add(MarketMainItemLabel(HOTTEST, MessageCodeUtil.getCodeLanMessage(HOTTEST)))
        futureList.add(
            doList(
                name = null, classifyCode = null, category = null,
                labelCode = null, score = null, rdType = null, sortType = MarketTemplateSortTypeEnum.DOWNLOAD_COUNT,
                desc = true, page = page, pageSize = pageSize
            )
        )
        val classifyList = classifyDao.getAllClassify(dslContext, 1)
        classifyList.forEach {
            val classifyCode = it["CLASSIFY_CODE"] as String
            labelInfoList.add(MarketMainItemLabel(classifyCode, it["CLASSIFY_NAME"] as String))
            futureList.add(
                doList(
                    name = null,
                    classifyCode = classifyCode,
                    category = null,
                    labelCode = null,
                    score = null,
                    rdType = null,
                    sortType = MarketTemplateSortTypeEnum.NAME,
                    desc = false,
                    page = page,
                    pageSize = pageSize
                )
            )
        }
        for (index in futureList.indices) {
            val labelInfo = labelInfoList[index]
            result.add(
                MarketTemplateMain(
                    key = labelInfo.key,
                    label = labelInfo.label,
                    records = futureList[index].get().records
                )
            )
        }
        return Result(result)
    }

    override fun handleProcessInfo(status: Int): List<ReleaseProcessItem> {
        val processInfo = initProcessInfo()
        when (status) {
            TemplateStatusEnum.INIT.status -> {
                setProcessInfo(processInfo, NUM_THREE, NUM_TWO, DOING)
            }
            TemplateStatusEnum.RELEASED.status -> {
                setProcessInfo(processInfo, NUM_THREE, NUM_THREE, SUCCESS)
            }
        }
        return processInfo
    }

    /**
     * 初始化插件版本进度
     */
    private fun initProcessInfo(): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BEGIN), NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(COMMIT), NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), NUM_THREE, UNDO))
        return processInfo
    }
}