package com.tencent.devops.common.api.analysisresult;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("CLOC工具最近一次分析结果")
public class CLOCLastAnalysisResultVO extends BaseLastAnalysisResultVO {
    @ApiModelProperty("代码行总数")
    private Long sumCode;

    @ApiModelProperty("空白行总数")
    private Long sumBlank;

    @ApiModelProperty("注释行总数")
    private Long sumComment;

    @ApiModelProperty("代码行变化")
    private Long codeChange;

    @ApiModelProperty("空白行变化")
    private Long blankChange;

    @ApiModelProperty("注释行变化")
    private Long commentChange;

    @ApiModelProperty("总行数")
    private Long totalLines;

    @ApiModelProperty("行数总变化")
    private Long linesChange;

    @ApiModelProperty("扫描总文件数量")
    private Long fileNum;

    @ApiModelProperty("文件数量变化")
    private Long fileNumChange;
}
