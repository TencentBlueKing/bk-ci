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
package com.tencent.devops.monitoring.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "noc语音告警通知请求体")
data class NocNoticeRequest(
    @get:Schema(title = "app标识", required = true, description = "app_code")
    @JsonProperty("app_code")
    val appCode: String,
    @get:Schema(title = "app私密key", required = true, description = "app_secret")
    @JsonProperty("app_secret")
    val appSecret: String,
    @get:Schema(title = "操作者RTX英文名", required = true, description = "operator")
    @JsonProperty("operator")
    var operator: String,
    @get:Schema(title = "父流程的ProcessName", required = false, description = "parent_process_name")
    @JsonProperty("parent_process_name")
    val parentProcessName: String? = null,
    @get:Schema(title = "父流程的InstanceId", required = false, description = "parent_instance_id")
    @JsonProperty("parent_instance_id")
    val parentInstanceId: String? = null,
    @get:Schema(title = "自动语音读字信息", required = false, description = "auto_read_message")
    @JsonProperty("auto_read_message")
    val autoReadMessage: String? = null,
    @get:Schema(title = "自动语音电话自定义回复选项", required = false, description = "key_options")
    @JsonProperty("key_options")
    val keyOptions: Map<String, String>? = null,
    @get:Schema(title = "任务头描述", required = false, description = "head_desc")
    @JsonProperty("head_desc")
    val headDesc: String? = null,
    @get:Schema(title = "需要展示的业务故障数据", required = true, description = "busi_data_list")
    @JsonProperty("busi_data_list")
    val busiDataList: List<NocNoticeBusData>,
    @get:Schema(title = "待通知的用户列表", required = true, description = "user_list_information")
    @JsonProperty("user_list_information")
    val userInfoList: List<NocNoticeUserInfo>,
    @get:Schema(title = "具体通知给用户的信息", required = false, description = "notice_information")
    @JsonProperty("notice_information")
    val noticeInformation: String? = null,
    @get:Schema(title = "任务最下方提供的附加注解信息", required = false, description = "append_comment")
    @JsonProperty("append_comment")
    val appendComment: String? = null
)
