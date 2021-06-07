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

import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import lombok.NoArgsConstructor;

/**
 * 任务信息概览数据
 *
 * @version V1.0
 * @date 2019/6/8
 */
@Data
@ApiModel("任务信息概览数据")
public class TaskOverviewVO
{
    @ApiModelProperty(value = "任务主键id", required = true)
    private long taskId;

    @ApiModelProperty(value = "任务扫描状态")
    private Integer status;

    @ApiModelProperty(value = "任务扫描结果总分")
    private Double rdIndicatorsScore;

    @ApiModelProperty(value = "任务扫描结果规范分数")
    private double codeStyleScore;

    @ApiModelProperty(value = "任务扫描结果安全分数")
    private double codeSecurityScore;

    @ApiModelProperty(value = "任务扫描结果度量分数")
    private double codeMeasureScore;

    @ApiModelProperty(value = "缺陷扫描结果缺陷分数(Coverity)")
    private double codeDefectScore;

    @ApiModelProperty(value = "任务扫描结果圈复杂度分数")
    private double codeCcnScore;

    @ApiModelProperty(value = "扫描场景是否符合开源扫描要求")
    private boolean isOpenScan;

    @ApiModelProperty(value = "平均千行圈复杂度超标数")
    private Double averageThousandDefect;

    @ApiModelProperty(value = "规范普通问题数")
    private int codeStyleNormalDefectCount;

    @ApiModelProperty(value = "一般程度规范告警平均千行数")
    private double averageNormalStandardThousandDefect;

    @ApiModelProperty(value = "规范严重问题数")
    private int codeStyleSeriousDefectCount;

    @ApiModelProperty(value = "严重程度规范告警平均千行数")
    private double averageSeriousStandardThousandDefect;

    @ApiModelProperty(value = "缺陷普通问题数")
    private int codeDefectNormalDefectCount;

    @ApiModelProperty(value = "一般程度缺陷告警平均千行数")
    private double averageNormalDefectThousandDefect;

    @ApiModelProperty(value = "缺陷严重问题数")
    private int codeDefectSeriousDefectCount;

    @ApiModelProperty(value = "严重程度缺陷告警平均千行数")
    private double averageSeriousDefectThousandDefect;

    @ApiModelProperty(value = "安全普通问题数")
    private int codeSecurityNormalDefectCount;

    @ApiModelProperty(value = "安全严重问题数")
    private int codeSecuritySeriousDefectCount;

    @ApiModelProperty(value = "工具最近一次分析结果列表")
    private List<LastAnalysis> lastAnalysisResultList;

    @ApiModelProperty(value = "工具最近一次分析结果列表")
    private List<LastCluster> lastClusterResultList;

    @Data
    public static class LastAnalysis
    {
        @ApiModelProperty(value = "工具名称", required = true)
        private String toolName;

        @ApiModelProperty(value = "当前步骤", required = true)
        private int curStep;

        @ApiModelProperty(value = "当前步骤状态，0成功/1失败", required = true)
        private int stepStatus;

        @ApiModelProperty(value = "最近一次分析时间", required = true)
        private long lastAnalysisTime;

        @ApiModelProperty(value = "最近一次分析耗时", required = true)
        private long elapseTime;

        @ApiModelProperty("构建ID")
        private String buildId;

        @ApiModelProperty("构建号")
        private String buildNum;

        @ApiModelProperty(value = "最近一次分析结果", required = true)
        private BaseLastAnalysisResultVO lastAnalysisResult;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LastCluster {
        @ApiModelProperty(value = "最近一次分析结果", required = true)
        private BaseClusterResultVO baseClusterResultVO;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LastCluster that = (LastCluster) o;
            return this.baseClusterResultVO.getType().equals(that.baseClusterResultVO.getType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(baseClusterResultVO.getType());
        }
    }
}
