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

package com.tencent.devops.support.model.approval

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * ESB创建MOA审批单参数
 * @author: carlyin
 * @since: 2019-09-03
 * @version: $Revision$ $Date$ $LastChangedBy$
 *.
 */
@Schema(title = "ESB创建MOA审批单参数")
open class CreateEsbMoaApproveParam(
    @get:Schema(title = "app标识", required = true, description = "app_code")
    @JsonProperty("app_code")
    val appCode: String = "",
    @get:Schema(title = "app私密key", required = true, description = "app_secret")
    @JsonProperty("app_secret")
    val appSecret: String = "",
    @get:Schema(title = "用户access_token", required = false, description = "access_token")
    @JsonProperty("access_token")
    var accessToken: String? = null,
    @get:Schema(title = "内部版用户登录态", required = false, description = "bk_ticket")
    @JsonProperty("bk_ticket")
    var bkTicket: String? = null,
    @get:Schema(title = "操作者RTX英文名", required = false, description = "operator")
    @JsonProperty("operator")
    var operator: String? = null,
    @get:Schema(title = "审批人，多个以逗号分隔", required = true, description = "verifier")
    @JsonProperty("verifier")
    val verifier: String,
    @get:Schema(title = "消息内容", required = true, description = "title")
    @JsonProperty("title")
    val title: String,
    @get:Schema(title = "任务ID", required = true, description = "taskid")
    @JsonProperty("taskid")
    val taskId: String,
    @get:Schema(title = "申请时间", required = true, description = "start_date")
    @JsonProperty("start_date")
    val startDate: String,
    @get:Schema(title = "回调URL", required = true, description = "back_url")
    @JsonProperty("back_url")
    val backUrl: String,
    @get:Schema(title = "系统URL，用于用户审核时跳转系统查看", required = false, description = "sys_url")
    @JsonProperty("sys_url")
    val sysUrl: String? = null
)
