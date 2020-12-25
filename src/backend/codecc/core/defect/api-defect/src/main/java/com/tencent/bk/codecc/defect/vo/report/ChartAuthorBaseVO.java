package com.tencent.bk.codecc.defect.vo.report;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据报表作者信息视图
 *
 * @version V1.0
 * @date 2019/12/4
 */
@Data
@ApiModel("数据报表作者信息视图")
public class ChartAuthorBaseVO
{
    @ApiModelProperty("作者名称")
    String authorName;

    @ApiModelProperty("总数")
    Integer total;
}
