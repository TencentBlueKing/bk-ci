/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Collection;
import java.util.List;

/**
 * 按组织架构查询任务告警请求体
 *
 * @version V1.0
 * @date 2020/2/12
 */

@Data
@ApiModel("按组织架构查询任务告警请求体")
public class DeptTaskDefectReqVO
{
    @ApiModelProperty("工具名称")
    private String toolName;

    @ApiModelProperty("任务ID集合")
    private Collection<Long> taskIds;

    @ApiModelProperty("事业群ID")
    private Integer bgId;

    @ApiModelProperty("部门ID")
    private List<Integer> deptIds;

    @ApiModelProperty("开始时间")
    private String startDate;

    @ApiModelProperty("截止时间")
    private String endDate;

    @ApiModelProperty("创建来源")
    private List<String> createFrom;

    @ApiModelProperty("遗留告警超时天数阈值")
    private Integer timeoutDays;

    @ApiModelProperty("告警严重级别筛选")
    private Integer severity;

}
