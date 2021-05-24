package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

@Data
@ApiModel
public class MetricsVO {

    @ApiModelProperty(value = "分析结果，各工具告警数")
    List<Analysis> lastAnalysisResultList;

    @ApiModelProperty(value = "任务ID")
    private long taskId;

    @ApiModelProperty(value = "本次度量计算的代码库版本")
    private String commitId;

    @ApiModelProperty(value = "代码库路径")
    private String repoUrl;

    @ApiModelProperty(value = "对应 CodeCC 页面路径")
    private String codeccUrl;

    @ApiModelProperty(value = "项目ID")
    private String projectId;

    @ApiModelProperty(value = "代码规范计算得分")
    private double codeStyleScore;

    @ApiModelProperty(value = "代码安全计算得分")
    private double codeSecurityScore;

    @ApiModelProperty(value = "代码度量计算得分")
    private double codeMeasureScore;

    @ApiModelProperty(value = "圈复杂度计算得分")
    private double codeCcnScore;

    @ApiModelProperty(value = "缺陷计算得分(Coverity)")
    private double codeDefectScore;

    @ApiModelProperty(value = "代码库总分")
    private double rdIndicatorsScore;

    @ApiModelProperty(value = "平均千行复杂度超标数")
    private Double averageThousandDefect;

    @ApiModelProperty(value = "一般程度规范告警数")
    private int codeStyleNormalDefectCount;

    @ApiModelProperty(value = "一般程度规范告警平均千行数")
    private double averageNormalStandardThousandDefect;

    @ApiModelProperty(value = "严重程度规范告警数")
    private int codeStyleSeriousDefectCount;

    @ApiModelProperty(value = "严重程度规范告警平均千行数")
    private double averageSeriousStandardThousandDefect;

    @ApiModelProperty(value = "一般程度度量告警数")
    private int codeDefectNormalDefectCount;

    @ApiModelProperty(value = "一般程度缺陷告警平均千行数")
    private double averageNormalDefectThousandDefect;

    @ApiModelProperty(value = "严重程度度量告警数")
    private int codeDefectSeriousDefectCount;

    @ApiModelProperty(value = "严重程度缺陷告警平均千行数")
    private double averageSeriousDefectThousandDefect;

    @ApiModelProperty(value = "一般程度安全告警数")
    private int codeSecurityNormalDefectCount;

    @ApiModelProperty(value = "严重程度安全告警数")
    private int codeSecuritySeriousDefectCount;

    @ApiModelProperty(value = "是否符合开源治理环境")
    private boolean isOpenScan;

    @ApiModelProperty(value = "扫描任务状态")
    private int status;

    @ApiModelProperty(value = "扫描失败信息")
    private String message;

    @ApiModelProperty(value = "分析时间")
    private long lastAnalysisTime;

    @Data
    public static class Analysis {

        private String toolName;

        private String displayName;

        private String type;

        private long elapseTime;

        private int buildNum;

        private String pattern;

        private int defectCount;

        private String defectUrl;
    }
}
