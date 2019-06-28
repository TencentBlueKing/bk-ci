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

package com.tencent.devops.store.service.template

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.pojo.template.MyTemplateItem
import com.tencent.devops.store.pojo.template.TemplateDetail
import com.tencent.devops.store.pojo.template.TemplateProcessInfo
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum

interface MarketTemplateService {

    /**
     * 模版市场，首页
     */
    fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<MarketTemplateMain>>

    /**
     * 模版市场，查询模版列表
     */
    fun list(
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
    ): MarketTemplateResp

    /**
     * 根据模版标识获取模版
     */
    fun getTemplateDetailByCode(
        userId: String,
        templateCode: String
    ): Result<TemplateDetail?>

    /**
     * 根据模版ID获取模版
     */
    fun getTemplateDetailById(
        userId: String,
        templateId: String
    ): Result<TemplateDetail?>

    /**
     * 关联模版到商店
     */
    fun addMarketTemplate(
        userId: String,
        templateCode: String,
        marketTemplateRelRequest: MarketTemplateRelRequest
    ): Result<Boolean>

    /**
     * 更新商店模版信息
     */
    fun updateMarketTemplate(
        userId: String,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ): Result<String?>

    /**
     * 删除模版关联关系
     */
    fun delete(
        userId: String,
        templateCode: String
    ): Result<Boolean>

    /**
     * 获取发布进度
     */
    fun getProcessInfo(
        TemplateId: String
    ): Result<TemplateProcessInfo>

    /**
     * 安装模板到项目
     */
    fun installTemplate(
        accessToken: String,
        userId: String,
        projectCodeList: ArrayList<String>,
        templateCode: String
    ): Result<Boolean>

    /**
     * 获取工作台模版列表
     */
    fun getMyTemplates(
        userId: String,
        templateName: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<MyTemplateItem>?>

    /**
     * 取消发布
     */
    fun cancelRelease(
        userId: String,
        templateId: String
    ): Result<Boolean>

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
     * 根据模板ID和模板代码判断模板是否存在
     */
    fun judgeTemplateExistByIdAndCode(
        templateId: String,
        templateCode: String
    ): Result<Boolean>
}