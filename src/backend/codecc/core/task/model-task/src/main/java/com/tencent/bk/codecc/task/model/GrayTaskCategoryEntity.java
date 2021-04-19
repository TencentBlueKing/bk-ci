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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 灰度项目分类表
 *
 * @version V1.0
 * @date 2021/1/4
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "t_gray_task_category")
@CompoundIndexes({
        @CompoundIndex(name = "project_id_1_status_1", def = "{'project_id': 1, 'status': 1}",
                background = true),
        @CompoundIndex(name = "project_id_1_pipeline_id_1", def = "{'project_id': 1, 'pipeline_id': 1}",
                background = true),
        @CompoundIndex(name = "project_id_1_gongfeng_project_id_1", def = "{'project_id': 1, 'gongfeng_project_id': 1}",
                background = true)
})
public class GrayTaskCategoryEntity extends CommonEntity {
    //蓝盾项目id
    @Field("project_id")
    private String projectId;
    //流水线id
    @Field("pipeline_id")
    private String pipelineId;
    //任务id
    @Field("task_id")
    private Long taskId;
    //工蜂项目id
    @Field("gongfeng_project_id")
    private Integer gongfengProjectId;
    //任务类别
    @Field("category")
    private String category;
    //任务状态
    @Field("status")
    private Integer status;
    //上次构建id
    @Field("last_build_id")
    private String lastBuildId;

}
