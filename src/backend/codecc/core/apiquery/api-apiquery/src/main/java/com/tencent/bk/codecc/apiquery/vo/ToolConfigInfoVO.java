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

package com.tencent.bk.codecc.apiquery.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具配置信息视图
 *
 * @version V2.0
 * @date 2020/5/11
 */
@Data
@ApiModel("工具配置信息视图")
@EqualsAndHashCode(callSuper = true)
public class ToolConfigInfoVO extends CommonVO
{
    @ApiModelProperty("配置信息对应的项目ID")
    private long taskId;

    @ApiModelProperty("工具的名称")
    private String toolName;

    @ApiModelProperty("工具当前任务执行步骤")
    private int curStep;

    @ApiModelProperty("工具当前任务步骤的状态，成功/失败")
    private int stepStatus;

    @ApiModelProperty("扫描类型 0:全量扫描  1:增量扫描")
    private String scanType;

    @ApiModelProperty("工具框架化参数总和")
    private String paramJson;

    @ApiModelProperty("跟进状态 FOLLOW_STATUS")
    private int followStatus;

    @ApiModelProperty("上次跟进状态")
    private int lastFollowStatus;

    @ApiModelProperty("工具显示名称")
    private String displayName;

    @ApiModelProperty("分析完成时间")
    private long endTime;

    @ApiModelProperty("启动时间")
    private long startTime;

    @ApiModelProperty("规则集")
    private ToolCheckerSetVO checkerSet;

    @ApiModelProperty("最新一次构建号")
    private String latestBuildNo;

    @ApiModelProperty("当前构件号")
    private String currentBuildId;

    @ApiModelProperty("工具特殊配置")
    private String specConfig;
}
