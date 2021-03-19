package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 公共分页视图对象
 *
 * @version V1.0
 * @date 2019/11/22
 */

@Data
public class CommonPageVO
{
    @ApiModelProperty(value = "第几页")
    private Integer pageNum;

    @ApiModelProperty(value = "每页多少条")
    private Integer pageSize;

    @ApiModelProperty(value = "排序字段")
    private String sortField;

    @ApiModelProperty(value = "排序类型")
    private String sortType;

}
