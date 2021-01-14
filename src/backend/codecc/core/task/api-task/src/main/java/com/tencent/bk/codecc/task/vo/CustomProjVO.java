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
 
package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * 个性化项目视图
 * 
 * @date 2020/3/24
 * @version V1.0
 */
@Data
@ApiModel("个性化项目视图")
public class CustomProjVO extends CommonVO
{
    @ApiModelProperty("项目url")
    private String url;

    @ApiModelProperty("任务id")
    private Long taskId;

    @ApiModelProperty("流水线id")
    private String pipelineId;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("是否显示告警")
    private Boolean defectDisplay;

    @ApiModelProperty("个性化触发项目来源")
    private String customProjSource;

    @ApiModelProperty("参数映射")
    private Map<String, String> paramMap;
}
