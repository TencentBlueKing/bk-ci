package com.tencent.bk.codecc.defect.model;

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
    @Field("task_id")
    private Long taskId;

    @Field("build_id")
    private String buildId;

    // 代码规范得分
    @Field("code_style_score")
    private double codeStyleScore;

    // 代码安全得分
    @Field("code_security_score")
    private double codeSecurityScore;

    // 代码度量得分
    @Field("code_measure_score")
    private double codeMeasureScore;

    // 指标总分
    @Field("rd_indicators_score")
    private double rdIndicatorsScore;
}

