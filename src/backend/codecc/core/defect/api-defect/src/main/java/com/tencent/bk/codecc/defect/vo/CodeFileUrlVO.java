package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * 告警代码文件仓库url
 * 
 * @date 2019/11/29
 * @version V2.0
 */
@Data
public class CodeFileUrlVO
{
    @NotEmpty(message = "流名称不能为空")
    @ApiModelProperty(value = "流名称", required = true)
    private String streamName;

    @ApiModelProperty(value = "工具名称", required = true)
    private String toolName;

    @ApiModelProperty(value = "所有文件的代码仓库url", required = true)
    private String fileList;
}
