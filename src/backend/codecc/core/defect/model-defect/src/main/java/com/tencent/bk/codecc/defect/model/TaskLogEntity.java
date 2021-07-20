/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 任务分析记录持久化对象
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_task_log")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1_build_id_1", def = "{'task_id': 1, 'tool_name': 1, 'build_id': 1}")
})
public class TaskLogEntity extends CommonEntity
{

    @Indexed
    @Field("stream_name")
    private String streamName;

    @Field("task_id")
    private long taskId;

    @Field("tool_name")
    private String toolName;

    @Field("curr_step")
    private int currStep;

    /**
     * 状态
     */
    @Field("flag")
    private int flag;

    @Field("start_time")
    private long startTime;

    @Field("end_time")
    private long endTime;

    @Field("elapse_time")
    private long elapseTime;

    @Field("pipeline_id")
    private String pipelineId;

    @Field("build_id")
    private String buildId;

    @Field("build_num")
    private String buildNum;

    @Field("trigger_from")
    private String triggerFrom;

    /**
     * 此次构建的代码中最晚提交时间
     */
    @Field("version_time")
    private long versionTime;

    @Field("step_array")
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
