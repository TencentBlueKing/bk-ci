package com.tencent.bk.codecc.defect.vo.report;

import com.tencent.devops.common.constant.ComConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 圈复杂度数据报表作者信息视图
 *
 * @version V1.0
 * @date 2019/12/4
 */
@Data
@ApiModel("圈复杂度数据报表作者信息视图")
public class CCNChartAuthorVO extends ChartAuthorBaseVO
{
    @ApiModelProperty("超高级别数量")
    private Integer superHigh;

    @ApiModelProperty("高级别数量")
    private Integer high;

    @ApiModelProperty("中级别数量")
    private Integer medium;

    @ApiModelProperty("低级别数量")
    private Integer low;

    //@ApiModelProperty("提示语")
    //private String tips;

    public CCNChartAuthorVO()
    {
        total = 0;
        superHigh = 0;
        high = 0;
        medium = 0;
        low = 0;
    }

    @Override
    public Integer getTotal()
    {
        return superHigh + high + medium + low;
    }

    public void count(int severity)
    {
        if (severity == ComConstants.RiskFactor.SH.value())
        {
            superHigh++;
        }
        else if (severity ==  ComConstants.RiskFactor.H.value())
        {
            high++;
        }
        else if (severity ==  ComConstants.RiskFactor.M.value())
        {
            medium++;
        }
        else if (severity ==  ComConstants.RiskFactor.L.value())
        {
            low++;
        }
    }
}
