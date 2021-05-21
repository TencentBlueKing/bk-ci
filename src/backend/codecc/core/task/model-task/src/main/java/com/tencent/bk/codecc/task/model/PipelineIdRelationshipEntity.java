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
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

/**
 * 流水线维度流水表
 * 
 * @date 2020/10/27
 * @version V1.0
 */
@Data
@Document(collection = "t_pipeline_id_relationship")
@CompoundIndexes({
        @CompoundIndex(name = "triggerdate_1_pipeline_1", def = "{'trigger_date': 1, 'pipeline_id' : 1}", background = true)
})
@AllArgsConstructor
public class PipelineIdRelationshipEntity extends CommonEntity
{
    @Field("task_id")
    private Long taskId;

    @Field("project_id")
    private String projectId;

    @Field("pipeline_id")
    private String pipelineId;

    @Field("status")
    private Integer status;

    @Field("trigger_date")
    private LocalDate triggerDate;
}
