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
import com.tencent.bk.codecc.apiquery.defect.model.CommonModel;
import lombok.Data;

import java.util.List;

/**
 * 个性化工蜂扫描实体类
 *
 * @date 2020/3/23
 * @version V1.0
 */
@Data
public class CustomProjModel
{
    /**
     * 传入项目的url
     */
    @JsonProperty("url")
    private String url;

    @JsonProperty("branch")
    private String branch;

    /**
     * 代码仓库类型
     */
    @JsonProperty("repository_type")
    private String repositoryType;

    /**
     * 任务id
     */
    @JsonProperty("task_id")
    private Long taskId;

    /**
     * 流水线id
     */
    @JsonProperty("pipeline_id")
    private String pipelineId;

    /**
     * 项目id
     */
    @JsonProperty("project_id")
    private String projectId;

    /**
     * 是否显示告警
     */
    @JsonProperty("defect_display")
    private Boolean defectDisplay;

    /**
     * 个性化项目来源
     */
    @JsonProperty("custom_proj_source")
    private String customProjSource;

    /**
     * 工蜂统计项目信息
     */
    @JsonProperty("gongfeng_stat_proj_info")
    private List<GongfengStatProjModel> gongfengStatProjInfo;

}
