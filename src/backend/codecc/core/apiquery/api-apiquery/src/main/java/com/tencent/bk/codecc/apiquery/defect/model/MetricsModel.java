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

package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 度量实体model
 *
 * @version V1.0
 * @date 2021/3/22
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricsModel {

    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("build_id")
    private String buildId;

    // 当前度量计算是否符合开源扫描要求
    @JsonProperty("is_open_scan")
    private boolean isOpenScan;

    // 代码规范得分
    @JsonProperty("code_style_score")
    private double codeStyleScore;

    // 代码安全得分
    @JsonProperty("code_security_score")
    private double codeSecurityScore;

    // 代码度量得分
    @JsonProperty("code_measure_score")
    private double codeMeasureScore;

    // 圈复杂度得分
    @JsonProperty("code_ccn_score")
    private double codeCcnScore;

    // Coverity 缺陷得分
    @JsonProperty("code_defect_score")
    private double codeDefectScore;

    // 指标总分
    @JsonProperty("rd_indicators_score")
    private double rdIndicatorsScore;

    // 千行平均圈复杂度超标数
    @JsonProperty("average_thousand_defect")
    private double averageThousandDefect;

    @JsonProperty("code_style_normal_defect_count")
    private int codeStyleNormalDefectCount;

    @JsonProperty("average_normal_standard_thousand_defect")
    private double averageNormalStandardThousandDefect;

    @JsonProperty("code_style_serious_defect_count")
    private int codeStyleSeriousDefectCount;

    @JsonProperty("average_serious_standard_thousand_defect")
    private double averageSeriousStandardThousandDefect;

    @JsonProperty("code_defect_normal_defect_count")
    private int codeDefectNormalDefectCount;

    @JsonProperty("average_normal_defect_thousand_defect")
    private double averageNormalDefectThousandDefect;

    @JsonProperty("code_defect_serious_defect_count")
    private int codeDefectSeriousDefectCount;

    @JsonProperty("average_serious_defect_thousand_defect")
    private double averageSeriousDefectThousandDefect;

    @JsonProperty("code_security_normal_defect_count")
    private int codeSecurityNormalDefectCount;

    @JsonProperty("code_security_serious_defect_count")
    private int codeSecuritySeriousDefectCount;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricsModel) {
            MetricsModel metricsModel = (MetricsModel) obj;
            return (taskId.equals(metricsModel.taskId) && buildId.equals(metricsModel.buildId));
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return buildId.hashCode();
    }
}
