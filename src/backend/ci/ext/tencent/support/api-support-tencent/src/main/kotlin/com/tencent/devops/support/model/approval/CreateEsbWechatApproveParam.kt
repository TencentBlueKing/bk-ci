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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * ESB创建微信审批单参数
 * @author: carlyin
 * @since: 2019-09-03
 * @version: $Revision$ $Date$ $LastChangedBy$
 *.
 */
@ApiModel("ESB创建微信审批单参数")
open class CreateEsbWechatApproveParam(
    @ApiModelProperty("app标识", required = true, name = "app_code")
    @JsonProperty("app_code")
    val appCode: String = "",
    @ApiModelProperty("app私密key", required = true, name = "app_secret")
    @JsonProperty("app_secret")
    val appSecret: String = "",
    @ApiModelProperty("用户access_token", required = false, name = "access_token")
    @JsonProperty("access_token")
    var accessToken: String? = null,
    @ApiModelProperty("内部版用户登录态", required = false, name = "bk_ticket")
    @JsonProperty("bk_ticket")
    var bkTicket: String? = null,
    @ApiModelProperty("操作者RTX英文名", required = false, name = "operator")
    @JsonProperty("operator")
    var operator: String? = null,
    @ApiModelProperty("蓝鲸APP名称", required = true, name = "app_name")
    @JsonProperty("app_name")
    val appName: String,
    @ApiModelProperty("审批人，多个以逗号分隔", required = true, name = "verifier")
    @JsonProperty("verifier")
    val verifier: String,
    @ApiModelProperty("消息内容", required = true, name = "message")
    @JsonProperty("message")
    val message: String,
    @ApiModelProperty("任务ID", required = true, name = "taskid")
    @JsonProperty("taskid")
    val taskId: String,
    @ApiModelProperty("回调URL", required = false, name = "url")
    @JsonProperty("url")
    val url: String? = null
)
