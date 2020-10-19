/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.apiquery.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.devops.common.api.pojo.ToolRunResult;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.Map;

/**
 * 失败任务记录实体类
 * 
 * @date 2020/8/19
 * @version V1.0
 */
@Data
@ApiModel("失败任务记录实体类")
public class TaskFailRecordModel 
{
    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("pipeline_id")
    private String pipelineId;

    @JsonProperty("build_id")
    private String buildId;

    @JsonProperty("vm_seq_id")
    private String vmSeqId;

    @JsonProperty("create_from")
    private String createFrom;

    @JsonProperty("machine_ip")
    private String machineIp;

    @JsonProperty("retry_flag")
    private Boolean retryFlag;

    @JsonProperty("upload_time")
    private Long uploadTime;

    @JsonProperty("time_cost")
    private Long timeCost;

    @JsonProperty("atom_code")
    private String atomCode;

    @JsonProperty("atom_version")
    private String atomVersion;

    @JsonProperty("err_code")
    private Integer errCode;

    @JsonProperty("err_msg")
    private String errMsg;

    @JsonProperty("err_type")
    private String errType;

    @JsonProperty("tool_result")
    private Map<String, ToolRunResult> toolResult;

}
