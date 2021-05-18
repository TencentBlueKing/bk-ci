package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则參數配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("规则參數配置")
public class CheckerProps {

    @ApiModelProperty("参数名称")
    private String propName;

    @ApiModelProperty("参数值")
    private String propValue;

    @ApiModelProperty("显示值")
    private String displayValue;

}
