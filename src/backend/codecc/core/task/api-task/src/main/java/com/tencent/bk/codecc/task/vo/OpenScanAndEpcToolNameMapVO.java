package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Set;

/**
 * 开源治理/EPC对应规则集下，所对应的工具映射
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("开源治理或EPC规则集所有对应工具映射")
public class OpenScanAndEpcToolNameMapVO {

    /**
     * 开源治理规则集对应的工具
     */
    @ApiModelProperty("开源治理规则集对应的工具")
    HashMap<String, Set<String>> openScan;

    /**
     * EPC规则集对应的工具
     */
    @ApiModelProperty("EPC规则集对应的工具")
    HashMap<String, Set<String>> epcScan;
}
