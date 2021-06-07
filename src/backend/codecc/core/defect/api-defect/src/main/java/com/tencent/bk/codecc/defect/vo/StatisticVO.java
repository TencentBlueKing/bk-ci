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
 
package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 统计视图
 * 
 * @date 2019/12/3
 * @version V1.0
 */
@Data
@ApiModel("统计视图")
public class StatisticVO extends CommonVO
{
    /**
     * 任务ID
     */
    @ApiModelProperty("任务id")
    private Long taskId;

    /**
     * 工具名称
     */
    @ApiModelProperty("工具名称")
    private String toolName;

    /**
     * 分析版本号
     */
    @ApiModelProperty("分析版本号")
    private String analysisVersion;

    /**
     * 统计的时间
     */
    @ApiModelProperty("统计的时间")
    private long time;

    /**
     * 告警数量
     */
    @ApiModelProperty("告警数量")
    private int defectCount;

    /**
     * 告警变化量
     */
    @ApiModelProperty("告警变化量")
    private int defectChange;

}
