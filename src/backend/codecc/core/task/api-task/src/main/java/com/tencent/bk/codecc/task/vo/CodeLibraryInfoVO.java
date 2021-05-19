package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务代码库配置
 *
 * @version V1.0
 * @date 2020/11/27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("任务代码库配置")
public class CodeLibraryInfoVO {
    @ApiModelProperty("链接地址")
    private String url;

    @ApiModelProperty("别名")
    private String aliasName;

    @ApiModelProperty("分支")
    private String branch;
}
