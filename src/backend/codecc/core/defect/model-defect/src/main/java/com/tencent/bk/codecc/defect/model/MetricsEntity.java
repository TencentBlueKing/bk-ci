package com.tencent.bk.codecc.defect.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "t_metrics")
@AllArgsConstructor
@NoArgsConstructor
@CompoundIndexes(
    @CompoundIndex(name = "taskid_buildid_idx", def = "{'task_id':1, 'build':1}", background = true)
)
public class MetricsEntity {
    public MetricsEntity(Long taskId, String buildId, double codeStyleScore, double codeSecurityScore, double codeMeasureScore, double rdIndicatorsScore) {
        this.taskId = taskId;
        this.buildId = buildId;
        this.codeStyleScore = codeStyleScore;
        this.codeSecurityScore = codeSecurityScore;
        this.codeMeasureScore = codeMeasureScore;
        this.rdIndicatorsScore = rdIndicatorsScore;
    }

    @Field("task_id")
    private Long taskId;

    @Field("build_id")
    private String buildId;

    // 当前度量计算是否符合开源扫描要求
    @Field("is_open_scan")
    private boolean isOpenScan;

    // 代码规范得分
    @Field("code_style_score")
    private double codeStyleScore;

    // 代码安全得分
    @Field("code_security_score")
    private double codeSecurityScore;

    // 代码度量得分
    @Field("code_measure_score")
    private double codeMeasureScore;

    // 圈复杂度得分
    @Field("code_ccn_score")
    private double codeCcnScore;

    // Coverity 缺陷得分
    @Field("code_defect_score")
    private double codeDefectScore;

    // 指标总分
    @Field("rd_indicators_score")
    private double rdIndicatorsScore;

    // 千行平均圈复杂度超标数
    @Field("average_thousand_defect")
    private double averageThousandDefect;

    @Field("code_style_normal_defect_count")
    private int codeStyleNormalDefectCount;

    @Field("average_normal_standard_thousand_defect")
    private double averageNormalStandardThousandDefect;

    @Field("code_style_serious_defect_count")
    private int codeStyleSeriousDefectCount;

    @Field("average_serious_standard_thousand_defect")
    private double averageSeriousStandardThousandDefect;

    @Field("code_defect_normal_defect_count")
    private int codeDefectNormalDefectCount;

    @Field("average_normal_defect_thousand_defect")
    private double averageNormalDefectThousandDefect;

    @Field("code_defect_serious_defect_count")
    private int codeDefectSeriousDefectCount;

    @Field("average_serious_defect_thousand_defect")
    private double averageSeriousDefectThousandDefect;

    @Field("code_security_normal_defect_count")
    private int codeSecurityNormalDefectCount;

    @Field("code_security_serious_defect_count")
    private int codeSecuritySeriousDefectCount;
}
