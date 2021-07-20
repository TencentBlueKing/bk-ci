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

package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工具配置的基本信息
 *
 * @version V1.0
 * @date 2019/4/30
 */
@Data
@ApiModel("工具配置的基本信息")
public class ToolConfigBaseVO
{
    @ApiModelProperty(value = "任务ID", required = true)
    private long taskId;

    @ApiModelProperty(value = "工具ID", required = true)
    private String toolName;

    @ApiModelProperty(value = "工具展示名称", required = true)
    private String toolDisplayName;

    @ApiModelProperty(value = "工具模型", required = true)
    private String toolPattern;

    @ApiModelProperty(value = "当前步骤", required = true)
    private int curStep;

    @ApiModelProperty(value = "当前步骤状态，0成功/1失败", required = true)
    private int stepStatus;

    @ApiModelProperty(value = "跟进状态，0/1-未跟进，2-体验，3-接入中，4-已接入，5-挂起，6-下架/停用", required = true)
    private int followStatus;

    @ApiModelProperty(value = "规则集")
    private CheckerSetVO checkerSet;

    @ApiModelProperty(value = "工具框架化参数总和")
    private String paramJson;

    @ApiModelProperty(value = "工具类型")
    private String toolType;

    @ApiModelProperty(value = "当前构建id")
    private String currentBuildId;
}
