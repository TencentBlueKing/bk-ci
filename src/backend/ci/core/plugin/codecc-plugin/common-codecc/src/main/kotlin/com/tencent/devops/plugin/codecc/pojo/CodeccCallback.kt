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

package com.tencent.devops.plugin.codecc.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("codecc回调")
data class CodeccCallback(
    @ApiModelProperty(name = "bs_project_id")
    @JsonProperty("bs_project_id")
    val projectId: String = "",
    @ApiModelProperty(name = "bs_pipeline_id")
    @JsonProperty("bs_pipeline_id")
    val pipelineId: String = "",
    @ApiModelProperty(name = "task_id")
    @JsonProperty("task_id")
    val taskId: String = "",
    @ApiModelProperty(name = "bs_build_id")
    @JsonProperty("bs_build_id")
    val buildId: String = "",
    @ApiModelProperty(name = "tool_snapshot_list")
    @JsonProperty("tool_snapshot_list")
    val toolSnapshotList: List<Map<String, Any>> = listOf()
)

/*
* 成功返回的数据模板:{
  "bs_project_id": "ldtest004",
  "bs_pipeline_id": "a992ebf4d4464e59aaf575e31aff8ffe",
  "task_id": "13869",
  "bs_build_id": "0f43c3b43bf84747b4548420f36ce497",
  "tool_snapshot_list": [
    {
      "start_time": 1531901657,
      "end_time": 1531901798,
      "latest_new_add_count": 0,
      "latest_closed_count": 0,
      "latest_exist_count": 0,
      "total_new": 0,
      "total_close": 0,
      "total_ignore": 0,
      "total_excluded": 5,
      "total_new_serious": 0,
      "total_new_normal": 0,
      "total_new_prompt": 0,
      "author_list": [],
      "tool_name_cn": "COVERITY",
      "defect_detail_url": "",
      "defect_report_url": "",
      "result_status": "success",
      "result_message": ""
    }
  ]
}




失败模板：{
  "bs_project_id": "ldtest004",
  "bs_pipeline_id": "a992ebf4d4464e59aaf575e31aff8ffe",
  "task_id": "13869",
  "bs_build_id": "0f43c3b43bf84747b4548420f36ce497",
  "tool_snapshot_list": [
    {
      "tool_name_cn": "COVERITY",
      "defect_detail_url": "",
      "defect_report_url": "",
      "result_status": "failed",
      "result_message": "xxxxxxx"
    }
  ]
}


字段名 类型 描述
start_time Long 分析开始时间
end_time Long 分析结束时间
latest_new_add_count Int 最近一次（本次）分析新增的告警数
latest_closed_count Int 最近一次（本次）分析关闭的告警数
latest_exist_count Int 最近一次（本次）分析遗留的告警数
total_new Int 当前所有待修复告警个数
total_close Int 当前所有已修复告警个数
total_ignore Int 当前所有已忽略告警个数
total_excluded Int 当前所有已屏蔽告警个数
total_new_serious Int 待修复告警级别严重个数
total_new_normal Int 待修复告警级别一般个数
total_new_prompt Int 待修复告警级别提示个数
author_list JSONArray 待修复告警作者列表
tool_name_cn String 工具名称
defect_detail_url String 工具告警详情页面url
defect_report_url String 工具报表页面url
result_status String 结果状态（success/failed）
result_message String 结果描述（成功为空，失败则为具体失败原因）

*
* */
