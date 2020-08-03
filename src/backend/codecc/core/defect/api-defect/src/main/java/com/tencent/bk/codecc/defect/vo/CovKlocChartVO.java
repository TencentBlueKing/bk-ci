package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;


@Data
@ApiModel("数据报表节点的model类")
public class CovKlocChartVO {

    @ApiModelProperty("待修复图表展示的最高数量")
    private int unFixMaxHeight;

    @ApiModelProperty("待修复图表展示的最低数量")
    private int unFixMinHeight;

    @ApiModelProperty("每日新图表展示的最高数量")
    private int newMaxHeight;

    @ApiModelProperty("每日新图表展示的最低数量")
    private int newMinHeight;

    @ApiModelProperty("每日关闭图表展示的最高数量")
    private int closeMaxHeight;

    @ApiModelProperty("每日关闭图表展示的最低数量")
    private int closeMinHeight;

    @ApiModelProperty("前端图表数据列表")
    private List<CovKlocChartDateVO> elemList;


}
