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

import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 工具配置信息
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("工具配置信息")
public class ToolConfigInfoVO extends CommonVO
{
    /**
     * 配置信息对应的项目ID
     */
    @ApiModelProperty(value = "配置信息对应的项目ID", required = true)
    private long taskId;

    /**
     * 工具的名称
     */
    @ApiModelProperty(value = "工具的名称", required = true)
    private String toolName;

    /**
     * 工具当前任务执行步骤
     */
    @ApiModelProperty(value = "工具当前任务执行步骤", required = true)
    private int curStep;

    /**
     * 工具当前任务步骤的状态，成功/失败
     */
    @ApiModelProperty(value = "工具当前任务步骤的状态，成功/失败", required = true)
    private int stepStatus;

    /**
     * 扫描类型 0:全量扫描  1:增量扫描
     */
    @ApiModelProperty(value = "扫描类型 0:全量扫描  1:增量扫描", required = true, allowableValues = "{0,1}")
    private String scanType;

    @ApiModelProperty(value = "工具框架化参数总和")
    private String paramJson;

    /**
     * 跟进状态 对照PREFIX_FOLLOW_STATUS
     */
    @ApiModelProperty(value = "跟进状态", required = true)
    private int followStatus;

    /**
     * 上次跟进状态
     */
    @ApiModelProperty(value = "上次跟进状态", required = true)
    private int lastFollowStatus;


    @ApiModelProperty(value = "工具显示名称")
    private String displayName;

    @ApiModelProperty(value = "分析完成时间")
    private long endTime;

    @ApiModelProperty(value = "启动时间")
    private long startTime;

    @ApiModelProperty("规则集")
    private ToolCheckerSetVO checkerSet;

    @ApiModelProperty("最新一次构建号")
    private String latestBuildNo;

    @ApiModelProperty("当前构件号")
    private String currentBuildId;

}
