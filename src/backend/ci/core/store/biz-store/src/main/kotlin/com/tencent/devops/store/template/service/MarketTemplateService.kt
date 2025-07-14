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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import com.tencent.devops.store.pojo.template.InstallTemplateResp
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.MyTemplateItem
import com.tencent.devops.store.pojo.template.TemplateDetail
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum

@Suppress("ALL")
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
        keyword: String?,
        classifyCode: String?,
        category: String?,
        labelCode: String?,
        score: Int?,
        rdType: TemplateRdTypeEnum?,
        sortType: MarketTemplateSortTypeEnum?,
        projectCode: String?,
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
     * 删除模版关联关系
     */
    fun delete(
        userId: String,
        templateCode: String
    ): Result<Boolean>

    /**
     * 安装模板到项目
     */
    fun installTemplate(
        userId: String,
        channelCode: ChannelCode,
        installTemplateReq: InstallTemplateReq
    ): Result<InstallTemplateResp>

    /**
     * 校验用户、模板和插件的可见范围
     */
    fun validateUserTemplateComponentVisibleDept(
        userId: String,
        templateCode: String,
        projectCodeList: ArrayList<String>
    ): Result<Boolean>

    /**
     * 校验流水线模型组件的可见范围
     */
    fun verificationModelComponentVisibleDept(
        userId: String,
        model: Model,
        projectCodeList: ArrayList<String>,
        templateCode: String? = null
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
     * 根据模板ID和模板代码判断模板是否存在
     */
    fun judgeTemplateExistByIdAndCode(
        templateId: String,
        templateCode: String
    ): Result<Boolean>
}
