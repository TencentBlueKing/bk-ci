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

/**
 * 灰度报告子视图
 * 
 * @date 2021/1/8
 * @version V1.0
 */
@ApiModel("灰度报告子视图")
@Data
public class GrayToolReportSubVO
{
    @ApiModelProperty("灰度总数")
    private Integer grayNum;

    @ApiModelProperty("总执行次数")
    private Integer totalNum;

    @ApiModelProperty("成功执行次数")
    private Integer successNum;

    @ApiModelProperty("成功执行次数")
    private String successRatio;

    @ApiModelProperty("告警数")
    private Integer defectCount;

    @ApiModelProperty("耗时")
    private Long elapsedTime;
}
