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

package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserTcmResource
import com.tencent.devops.plugin.pojo.ParametersInfo
import com.tencent.devops.plugin.pojo.tcm.TcmApp
import com.tencent.devops.plugin.pojo.tcm.TcmTemplate
import com.tencent.devops.plugin.pojo.tcm.TcmTemplateParam
import com.tencent.devops.plugin.service.TcmService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTcmResourceImpl @Autowired constructor(private val tcmService: TcmService) : UserTcmResource {
    override fun getParamsList(userId: String, appId: String, tcmAppId: String, templateId: String): Result<List<ParametersInfo>> {
        return Result(tcmService.getParamsList(userId, appId, tcmAppId, templateId))
    }

    override fun getApps(userId: String): Result<List<TcmApp>> {
        return Result(tcmService.getApps(userId))
    }

    override fun getTemplates(userId: String, ccid: String, tcmAppId: String): Result<List<TcmTemplate>> {
        return Result(tcmService.getTemplates(userId, ccid, tcmAppId))
    }

    override fun getTemplateInfo(userId: String, ccid: String, tcmAppId: String, templateId: String): Result<List<TcmTemplateParam>> {
        return Result(tcmService.getTemplateInfo(userId, ccid, tcmAppId, templateId))
    }
}
