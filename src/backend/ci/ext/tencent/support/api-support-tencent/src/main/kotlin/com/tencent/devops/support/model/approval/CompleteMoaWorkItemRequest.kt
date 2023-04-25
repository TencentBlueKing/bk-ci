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

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import com.fasterxml.jackson.annotation.JsonProperty

@ApiModel("审批单关闭请求报文体")
data class CompleteMoaWorkItemRequest(
    @ApiModelProperty("app标识", required = false, name = "app_code")
    @JsonProperty("app_code")
    var appCode: String? = null,
    @ApiModelProperty("app私密key", required = false, name = "app_secret")
    @JsonProperty("app_secret")
    var appSecret: String? = null,
    @ApiModelProperty("用户access_token", required = false, name = "access_token")
    @JsonProperty("access_token")
    var accessToken: String? = null,
    @ApiModelProperty("内部版用户登录态", required = false, name = "bk_ticket")
    @JsonProperty("bk_ticket")
    var bkTicket: String? = null,
    @ApiModelProperty("操作者RTX英文名", required = false, name = "operator")
    @JsonProperty("operator")
    var operator: String? = null,
    @JsonProperty("activity")
    @ApiModelProperty("审批流程对应的审批节点", required = false)
    val activity: String?, // GM审批
    @JsonProperty("category")
    @ApiModelProperty("单据类别", required = true)
    val category: String = MoaWorkitemCreateCategoryType.IT.id, // C23D7091B98844659D128773209BBF85
    @JsonProperty("handler")
    @ApiModelProperty("单据审批人", required = false)
    val handler: String?, // erkehe
    @JsonProperty("process_inst_id")
    @ApiModelProperty("流程实例标识", required = true)
    val processInstId: String, // Cost/ExpenseProcess/p20210080912
    @JsonProperty("process_name")
    @ApiModelProperty("流程名称", required = true)
    val processName: String // Cost/ExpenseProcess
)
