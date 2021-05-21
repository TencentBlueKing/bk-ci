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


import com.tencent.devops.common.api.pojo.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;


/**
 * 工具告警响应视图
 *
 * @version V1.0
 * @date 2019/11/25
 */

@Data
@ApiModel("工具告警响应视图")
public class ToolDefectRspVO {
    @ApiModelProperty("任务主键")
    private long taskId;

    @ApiModelProperty("任务英文名")
    private String nameEn;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("严重规则数")
    private Integer seriousCheckerCount;

    @ApiModelProperty("正常规则数")
    private Integer normalCheckerCount;

    @ApiModelProperty("提示规则数")
    private Integer promptCheckerCount;

    @ApiModelProperty("新增文件数")
    private Integer newDefectCount;

    @ApiModelProperty("历史文件数")
    private Integer historyDefectCount;

    @ApiModelProperty("规则总数")
    private Integer totalCheckerCount;

    @ApiModelProperty("首次分析时间")
    private Long firstAnalysisSuccessTime;

    @ApiModelProperty("lint类告警清单")
    private Page<LintDefectVO> lintDefectList;

    @Deprecated
    @ApiModelProperty("lint类文件清单")
    private Page<LintFileVO> lintFileList;

    @ApiModelProperty("Cov类文件清单")
    private Page<DefectDetailVO> defectList;

    /**
     * 风险系数极高的个数
     */
    @ApiModelProperty("风险系数极高的个数")
    private Integer superHighCount;

    @ApiModelProperty("风险系数高的个数")
    private Integer highCount;

    @ApiModelProperty("风险系数中的个数")
    private Integer mediumCount;

    @ApiModelProperty("风险系数低的个数")
    private Integer lowCount;

    @ApiModelProperty("告警总数")
    private Integer totalCount;

    @ApiModelProperty("待修复告警数")
    private Integer existCount;

    @ApiModelProperty("已修复告警数")
    private Integer fixCount;

    @ApiModelProperty("已忽略告警数")
    private Integer ignoreCount;

    @ApiModelProperty(value = "圈复杂度阀值")
    private Integer ccnThreshold;

    @ApiModelProperty("缺陷列表")
    private Page<CCNDefectVO> ccnDefectList;

    @ApiModelProperty("新老告警判定时间")
    private Long newDefectJudgeTime;

    @ApiModelProperty("重复率缺陷列表")
    private Page<DUPCDefectVO> dupcDefectList;

    @ApiModelProperty("任务详情Map")
    private Map<Long, TaskInfoExtVO> taskDetailVoMap;
}
