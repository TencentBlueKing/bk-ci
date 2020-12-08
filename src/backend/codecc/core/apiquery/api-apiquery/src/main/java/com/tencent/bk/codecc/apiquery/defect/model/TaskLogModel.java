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
package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 分析记录实体类
 *
 * @version V2.0
 * @date 2020/5/15
 */
@Data
public class TaskLogModel
{
    /**
     * 流名称(任务英文名)
     */
    @JsonProperty("stream_name")
    private String streamName;

    /**
     * 任务ID
     */
    @JsonProperty("task_id")
    private long taskId;

    /**
     * 工具名
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 当前分析阶段
     */
    @JsonProperty("curr_step")
    private int currStep;

    /**
     * 阶段的状态
     */
    @JsonProperty("flag")
    private int flag;

    /**
     * 当前阶段开始时间
     */
    @JsonProperty("start_time")
    private long startTime;

    /**
     * 当前阶段结束时间
     */
    @JsonProperty("end_time")
    private long endTime;

    /**
     * 当前阶段耗时
     */
    @JsonProperty("elapse_time")
    private long elapseTime;

    /**
     * 流水线ID
     */
    @JsonProperty("pipeline_id")
    private String pipelineId;

    /**
     * 构建ID
     */
    @JsonProperty("build_id")
    private String buildId;

    /**
     * 构建号
     */
    @JsonProperty("build_num")
    private String buildNum;

    /**
     * 触发来源
     */
    @JsonProperty("trigger_from")
    private String triggerFrom;

    /**
     * 此次构建的代码中最晚提交时间
     */
    @JsonProperty("version_time")
    private long versionTime;

    /**
     * 步骤列表
     */
    @JsonProperty("step_array")
    private List<TaskUnit> stepArray;

    @Data
    public static class TaskUnit
    {
        private int stepNum;
        private long startTime;
        private long endTime;
        private String msg;
        private int flag;
        private long elapseTime;

        /**
         * 建议值,true/false
         */
        private String dirStructSuggestParam;

        /**
         * 编译是否成功，true（成功）/false（失败）
         */
        private String compileResult;

    }
}
