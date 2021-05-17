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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 灰度报告视图
 *
 * @version V1.0
 * @date 2021/1/8
 */
@ApiModel("灰度报告视图")
@Data
public class GrayToolReportVO {
    @ApiModelProperty("蓝盾项目id")
    private String projectId;
    @ApiModelProperty("工具名")
    private String toolName;
    @ApiModelProperty("统一下发生成的唯一id")
    private String codeccBuildId;
    @ApiModelProperty("上一次报告信息")
    private GrayToolReportSubVO lastReportInfo;
    @ApiModelProperty("本次报告信息")
    private GrayToolReportSubVO currentReportInfo;
    @ApiModelProperty("成功率变化比率")
    private String successRatioChange;
    @ApiModelProperty("告警变化比率")
    private String defectCountChange;
    @ApiModelProperty("扫描耗时变化比率")
    private String elapseTimeChange;
    @ApiModelProperty("扫描出告警的任务清单")
    private List<GrayDefectTaskSubVO> defectTaskList;
}
