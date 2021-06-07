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

package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import com.tencent.devops.common.api.pojo.ToolRunResult;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

/**
 * 任务失败记录视图
 *
 * @date 2020/7/13
 * @version V1.0
 */
@Data
@Document(collection = "t_task_fail_record")
@CompoundIndexes({
        @CompoundIndex(name = "upload_time_1_retry_flag_1_time_cost_1", def = "{'upload_time': 1, 'retry_flag': 1, 'time_cost' : 1}", background = true),
})
public class TaskFailRecordEntity extends CommonEntity
{
    @Field("task_id")
    private Long taskId;

    @Field("project_id")
    private String projectId;

    @Field("pipeline_id")
    private String pipelineId;

    @Field("build_id")
    private String buildId;

    @Field("vm_seq_id")
    private String vmSeqId;

    @Field("create_from")
    private String createFrom;

    @Field("machine_ip")
    private String machineIp;

    @Field("retry_flag")
    private Boolean retryFlag;

    @Field("upload_time")
    private Long uploadTime;

    @Field("time_cost")
    private Long timeCost;

    @Field("atom_code")
    private String atomCode;

    @Field("atom_version")
    private String atomVersion;

    @Field("err_code")
    private Integer errCode;

    @Field("err_msg")
    private String errMsg;

    @Field("err_type")
    private String errType;

    @Field("tool_result")
    private Map<String, ToolRunResult> toolResult;

}
