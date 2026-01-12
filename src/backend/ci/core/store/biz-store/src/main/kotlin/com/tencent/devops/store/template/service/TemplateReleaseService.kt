/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.template.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.model.store.tables.records.TTemplateRecord
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateReleaseReq
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateV2Request
import org.jooq.DSLContext

@Suppress("ALL")
interface TemplateReleaseService {
    /**
     * 关联研发商店模板
     */
    fun addMarketTemplate(
        userId: String,
        templateCode: String,
        marketTemplateRelRequest: MarketTemplateRelRequest
    ): Result<Boolean>

    /**
     * 更新研发商店模板-v1
     */
    fun updateMarketTemplate(
        userId: String,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ): Result<String?>

    /**
     * 上架研发商店-v2
     */
    fun releaseMarketTemplate(
        userId: String,
        request: MarketTemplateUpdateV2Request
    ): Result<String>

    fun releaseMarketTemplateVersions(
        userId: String,
        request: MarketTemplateReleaseReq
    ): Boolean

    fun handleTemplateRelease(
        context: DSLContext,
        userId: String,
        approveResult: String,
        template: TTemplateRecord,
        templateStatus: Byte,
        templateStatusMsg: String
    )

    /**
     * 获取发布进度
     */
    fun getProcessInfo(userId: String, templateId: String): Result<StoreProcessInfo>

    /**
     * 获取发布进度-根据模板CODE
     */
    fun getProcessInfoByCode(userId: String, templateCode: String): Result<StoreProcessInfo>

    /**
     * 取消发布
     */
    fun cancelRelease(userId: String, templateId: String): Result<Boolean>

    /**
     * 取消发布-根据CODE
     */
    fun cancelReleaseByCode(userId: String, templateCode: String): Result<Boolean>

    /**
     * 下架模板
     */
    fun offlineTemplate(
        userId: String,
        templateCode: String,
        version: String?,
        reason: String?
    ): Result<Boolean>

    /**
     * 下架模板-v2
     */
    fun offlineTemplateV2(
        userId: String,
        templateCode: String,
        templateVersion: Long?,
        reason: String?
    ): Result<Boolean>
}
