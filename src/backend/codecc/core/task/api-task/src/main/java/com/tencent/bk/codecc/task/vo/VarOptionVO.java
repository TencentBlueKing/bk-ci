package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel("规则参数操作选项")
public class VarOptionVO
{
    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("ID")
    private String id;
}