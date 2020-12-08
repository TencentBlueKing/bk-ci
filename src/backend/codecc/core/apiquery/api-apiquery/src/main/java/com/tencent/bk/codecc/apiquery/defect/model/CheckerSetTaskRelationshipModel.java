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
import lombok.EqualsAndHashCode;

/**
 * 规则集任务关联实体类
 *
 * @version V2.0
 * @date 2020/5/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CheckerSetTaskRelationshipModel extends CommonModel
{

    /**
     * 规则集ID
     */
    @JsonProperty("checker_set_id")
    private String checkerSetId;

    /**
     * 任务ID
     */
    @JsonProperty("task_id")
    private Long taskId;

    /**
     * 项目ID
     */
    @JsonProperty("project_id")
    private String projectId;


}
