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

package com.tencent.devops.process.pojo.pipeline

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import com.fasterxml.jackson.annotation.JsonProperty

@ApiModel("MOA WorkItem 回调")
data class ExtServiceMoaWorkItemReq(
    @ApiModelProperty(
        "单据发生的节点。当前的审批单是报销流程中，申请人直接上级审批。" +
            "那么这个字段的值即可叫做“申请人直接上级审批”，或者对应的英文名称",
                required = true
    )
    @JsonProperty("activity")
    val activity: String, // Default
    @JsonProperty("business_key")
    @ApiModelProperty("业务系统不用关心此字段", required = true)
    val businessKey: String, // IT:Testflow:Testflow-20210903-165403
    @JsonProperty("category")
    @ApiModelProperty("所属业务领域，单据会被MyOA归类到指定的领域中。可用的领域请参考【业务领域分类表】", required = true)
    val category: String, // IT
    @JsonProperty("create_time")
    @ApiModelProperty("单据创建时间", required = true)
    val createTime: String, // 2021-09-03T16:54:06.571+08:00
    @JsonProperty("data")
    @ApiModelProperty("data字段是单据的变量，用于业务系统的数据传递", required = true)
    val data: List<ExtServiceMoaWorkItemReqData>,
    @JsonProperty("handler")
    @ApiModelProperty("当前审批单据的处理人的英文名，比如：zhangsan", required = true)
    val handler: String, // v_showpan
    @JsonProperty("id")
    @ApiModelProperty("单据id", required = true)
    val id: String, // 6131e2ae7b7c1a4ca6198da2
    @JsonProperty("process_inst_id")
    @ApiModelProperty("流程实例标识。通常是其对应的业务单据的流水号", required = true)
    val processInstId: String, // Testflow-20210903-165403
    @JsonProperty("process_name")
    @ApiModelProperty(
        "流程名称。标明单据所属的业务系统（流程）。比如：针对报销审批单据，它是费用系统的报销流程创建的。" +
            "因此其 process_name 为 Cost/ExpenseProcess",
                required = true
    )
    val processName: String, // Testflow
    @JsonProperty("submit_action")
    @ApiModelProperty("审批动作，比如：agree,reject等", required = true)
    val submitAction: String, // 1
    @JsonProperty("submit_action_name")
    @ApiModelProperty("审批动作的名称，比如：同意、驳回等", required = true)
    val submitActionName: String, // 同意
    @JsonProperty("submit_form")
    @ApiModelProperty("审批提交的自定义表单信息", required = true)
    val submitForm: Map<String, String>,
    @JsonProperty("submit_opinion")
    @ApiModelProperty("审批者的审批意见", required = true)
    val submitOpinion: String, // 同意 【通过PC快速审批】
    @JsonProperty("submit_source")
    @ApiModelProperty("审批者所使用的终端，例如PC、微信、RTX等", required = true)
    val submitSource: String, // PC
    @JsonProperty("submit_time")
    @ApiModelProperty("审批时间", required = true)
    val submitTime: String // 2021-09-03T16:54:22.005382898+08:00
)

data class ExtServiceMoaWorkItemReqData(
    @JsonProperty("key")
    val key: String, // data_key1
    @JsonProperty("value")
    val value: List<String>
)
