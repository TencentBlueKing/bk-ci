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
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 新增开源扫描项目retry实体类
 * 
 * @date 2020/7/15
 * @version V1.0
 */
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_new_task_retry")
@Data
@CompoundIndexes({
        @CompoundIndex(name = "upload_time_1_retry_flag_1", def = "{'upload_time': 1, 'retry_flag': 1}", background = true)
})
public class NewTaskRetryRecordEntity extends CommonEntity
{

    @Field("task_id")
    @Indexed(background = true)
    private Long taskId;

    @Field("pipeline_id")
    private String pipelineId;

    @Field("project_id")
    private String projectId;

    @Field("task_owner")
    private String taskOwner;

    @Field("retry_flag")
    private Boolean retryFlag;

    @Field("upload_time")
    private Long uploadTime;

}
