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

@ApiModel("ESB 创建审批单据（V3）")
data class CreateEsbMoaWorkItem(
    @ApiModelProperty("app标识", required = true, name = "app_code")
    @JsonProperty("app_code")
    val appCode: String,
    @ApiModelProperty("app私密key", required = true, name = "app_secret")
    @JsonProperty("app_secret")
    val appSecret: String,
    @ApiModelProperty("用户access_token", required = false, name = "access_token")
    @JsonProperty("access_token")
    var accessToken: String? = null,
    @ApiModelProperty("内部版用户登录态", required = false, name = "bk_ticket")
    @JsonProperty("bk_ticket")
    var bkTicket: String? = null,
    @ApiModelProperty("操作者RTX英文名", required = false, name = "operator")
    @JsonProperty("operator")
    var operator: String? = null,
    @JsonProperty("work_items")
    @ApiModelProperty("单据列表", required = false)
    val workItems: List<MoaWorkItemElement>
)

data class MoaWorkItemElement(
    @JsonProperty("actions")
    @ApiModelProperty("审批动作: 定义了审批人对单据可执行的操作", required = false)
    val actions: List<MoaWorkItemCreateAction>? = null,
    @JsonProperty("activity")
    @ApiModelProperty("审批节点: 审批流程对应的审批节点", required = true)
    val activity: String, // 一级审批
    @JsonProperty("applicant")
    @ApiModelProperty("申请人: 该参数仅支持传入一个申请人英文名", required = false)
    val applicant: String? = null, // {{applicant}}
    @JsonProperty("approval_history")
    @ApiModelProperty("审批历史: 单据详情页可展示该单据过往的全部审批历史", required = false)
    val approvalHistory: List<MoaWorkItemCreateApprovalHistory>? = null,
    @JsonProperty("attachments")
    @ApiModelProperty("附件", required = false)
    val attachments: List<MoaWorkItemCreateAttachment>? = null,
    @JsonProperty("callback_url")
    @ApiModelProperty("回调地址: 当审批人在MyOA审批单据后，MyOA会调用此地址将审批结果告知业务系统", required = false)
    val callbackUrl: String?,
    @JsonProperty("category")
    @ApiModelProperty("单据类别: MyOA定义了13种类别，单据会被归类到指定类别中", required = false)
    val category: String? = MoaWorkitemCreateCategoryType.IT.id, // C23D7091B98844659D128773209BBF85
    @JsonProperty("detail_view")
    @ApiModelProperty("详情视图: 在这里的定义的字段会在待办详情页中被展示出来。", required = false)
    val detailView: List<MoaWorkItemCreateKeyAndValue>? = null,
    @JsonProperty("enable_batch_approval")
    @ApiModelProperty("是否允许批量审批: 默认为true，若为false则单据不能被批量审批，只能逐条审批。", required = false)
    val enableBatchApproval: Boolean? = true, // true
    @JsonProperty("enable_mobile_approval")
    @ApiModelProperty("是否允许手机审批", required = false)
    val enableMobileApproval: Boolean? = true, // false
    @JsonProperty("enable_quick_approval")
    @ApiModelProperty("是否允许单据在列表快速审批: 默认为true，若为false则只能在单据详情页审批，", required = false)
    val enableQuickApproval: Boolean? = true, // true
    @JsonProperty("form")
    @ApiModelProperty("自定义表单: 可以在审批单上展示一个自定义控件的表单。回调时，把用户的输入结果一并返回给业务系统", required = false)
    val form: List<MoaWorkItemCreateForm>?,
    @JsonProperty("form_url")
    @ApiModelProperty("业务单据的PC端访问地址: 审批人点击此地址可以跳转到业务系统的当前单据进行进一步查看和操作。", required = false)
    val formUrl: String?,
    @JsonProperty("handler")
    @ApiModelProperty("单据审批人: 该参数仅支持传入一个审批人英文名", required = true)
    val handler: String, // {{handlerA}}
    @JsonProperty("list_view")
    @ApiModelProperty(
        "列表视图: 在这里定义的字段会在待办列表中被简要展示出来（最多支持显示5个字段）。" +
            "如果此字段没有定义，则会使用详情的数据来进行填充。",
                required = false
    )
    val listView: List<MoaWorkItemCreateKeyAndValue>? = null,
    @JsonProperty("mobile_form_url")
    @ApiModelProperty("业务单据的移动端访问地址: 审批人可以在移动端（微信、企业微信等）点击对应的链接打开此单据。", required = false)
    val mobileFormUrl: String? = null,
    @JsonProperty("process_inst_id")
    @ApiModelProperty("流程实例标识: 该审批流程的唯一标识 如流程单号", required = true)
    val processInstId: String, // {{process_inst_id}}
    @JsonProperty("process_name")
    @ApiModelProperty("流程名称: 单据所属的业务流程名称，由业务传入", required = true)
    val processName: String, // Cost/ExpenseProcess
    @JsonProperty("title")
    @ApiModelProperty("单据标题", required = true)
    val title: String, // 报销系统：{{applicant}}提交的车旅费报销,请您审批！单号:【{{process_inst_id}}】
    @JsonProperty("data")
    @ApiModelProperty("单据变量。可以记录一些业务系统传递过来的变量，并在触发回调的时候返回给业务系统", required = false)
    val data: List<MoaWorkItemCreateData>?
)

data class MoaWorkItemCreateAction(
    @JsonProperty("display_name")
    @ApiModelProperty("动作的展示名称。比如：同意、驳回等", required = false)
    val displayName: String, // 同意
    @JsonProperty("value")
    @ApiModelProperty("动作的标识，与display_name一一对应。", required = false)
    val value: String, // agree
    @JsonProperty("opinion_required")
    @ApiModelProperty(
        "动作的审批意见是否必填。默认为false，若为true，执行该动作时会提示必填审批意见。" +
            "仅针对详情页的审批动作，对于列表页的审批动作无效。",
                required = false
    )
    val opinionRequired: Boolean? = false
)

data class MoaWorkItemCreateApprovalHistory(
    @JsonProperty("action")
    @ApiModelProperty("审批者的审批动作", required = false)
    val action: String?, // 同意
    @JsonProperty("approval_time")
    @ApiModelProperty("审批的时间,格式为:yyyy-MM-ddTHH:mm:ss", required = false)
    val approvalTime: String?, // 2021-08-20T08:55:05.893
    @JsonProperty("approver")
    @ApiModelProperty("审批者的英文名", required = false)
    val approver: String?, // rustzou
    @JsonProperty("approver_role")
    @ApiModelProperty("审批者的角色", required = false)
    val approverRole: String?, // 直接leader
    @JsonProperty("opinion")
    @ApiModelProperty("审批者的意见", required = false)
    val opinion: String?, // 同意
    @JsonProperty("remark")
    @ApiModelProperty("备注", required = false)
    val remark: String?, // 同意报销
    @JsonProperty("step")
    @ApiModelProperty("审批流的审批节点", required = false)
    val step: String? // 直接leader审批
)

data class MoaWorkItemCreateAttachment(
    @JsonProperty("name")
    @ApiModelProperty("附件的名称", required = false)
    val name: String?, // 南航发票
    @JsonProperty("type")
    @ApiModelProperty("附件类型，小写，比如pdf，txt，jpg等，指定类型后，MyOA会在前端渲染出类型图标。", required = false)
    val type: String?, // pdf
    @JsonProperty("url")
    @ApiModelProperty("附件的url地址", required = false)
    val url: String?
)

data class MoaWorkItemCreateForm(
    @JsonProperty("default_value")
    @ApiModelProperty("控件的默认值", required = false)
    val defaultValue: String?, // TextBox1
    @JsonProperty("description")
    @ApiModelProperty("控件的描述", required = false)
    var description: String?, // TextBox description
    @JsonProperty("is_required")
    @ApiModelProperty("提交表单用，决定此字段是否必填，布尔类型", required = false)
    val isRequired: Boolean?, // true
    @JsonProperty("name")
    @ApiModelProperty("控件名称，不能为null、undefined和空串", required = false)
    val name: String?, // TextBox
    @JsonProperty("ui_type")
    @ApiModelProperty("控件的类型，目前仅支持五种类型", required = false)
    val uiType: String? = MoaWorkItemCreateUiType.TEXT_BOX.value, // TextBox
    @JsonProperty("values")
    @ApiModelProperty("控件值的集合，数组类型", required = false)
    val values: List<String>?
)

enum class MoaWorkItemCreateUiType(val value: String) {
    // 文本输入框
    TEXT_BOX("TextBox"),

    // 下拉选择框
    DROP_DOWN_LIST("DropDownList"),

    // 单选
    RADIO_BOX("RadioBox"),

    // 多选
    CHECK_BOX("CheckBox"),

    // 日期选择器
    DATE_PICKER("DatePicker");
}

data class MoaWorkItemCreateKeyAndValue(
    @JsonProperty("key")
    val key: String, // 申请人
    @JsonProperty("value")
    val value: String // {{applicant}}
)

data class MoaWorkItemCreateData(
    @JsonProperty("key")
    val key: String,
    @JsonProperty("value")
    val value: List<String>
)

enum class MoaWorkitemCreateCategoryType(val id: String) {
    // IT服务
    IT("6E407729E7F44863A109CFC009731C79"),

    // 安全中心
    SECURITY("A40524B5A3C3469DA3D0E876412EF30D"),

    // 运营业务
    BUSINESS("99511B1EDD8E4EB9B70E7AC1184EEBC2");
    // 其余10种按需增加
}
