package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用于统计每一类缺陷状态的个数")
public class CovKlocChartDateVO
{
    @ApiModelProperty("日期")
    private String date;

    @ApiModelProperty("显示日期")
    private String tips;

    @ApiModelProperty("待修复告警趋势-待修复数量")
    private int unFixCount;

    @ApiModelProperty("每天新增-总数量")
    private int newCount;

    @ApiModelProperty("每天新增-待修复的数量[ newCount, reopen ]")
    private int existCount;

    @ApiModelProperty("每天关闭-总数量[ repairedCount, ignoreCount, excludedCount ]")
    private int closedCount;

    @ApiModelProperty("每天关闭-修复数量")
    private int repairedCount;

    @ApiModelProperty("每天关闭-忽略数量")
    private int ignoreCount;

    @ApiModelProperty("每天关闭-过滤屏蔽数量")
    private int excludedCount;

    public CovKlocChartDateVO() {
        this.repairedCount = 0;
        this.excludedCount = 0;
        this.ignoreCount = 0;
        this.newCount = 0;
        this.closedCount = 0;
        this.existCount = 0;
        this.unFixCount = 0;
    }

    public void increaseRepairedCount()
    {
        this.repairedCount++;
    }

    public void increaseExcludedCount()
    {
        this.excludedCount++;
    }

    public void increaseIgnoreCount()
    {
        this.ignoreCount++;
    }

    public void increaseNewCount()
    {
        this.newCount++;
    }

    public void increaseClosedCount()
    {
        this.closedCount++;
    }

    public void increaseExistCount()
    {
        this.existCount++;
    }

    public void increaseFixCount()
    {
        this.unFixCount++;
    }
}
