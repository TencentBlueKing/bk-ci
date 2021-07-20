package com.tencent.bk.codecc.defect.vo.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 构建视图
 *
 * @version V1.0
 * @date 2019/12/23
 */
@Data
@ApiModel("构建视图")
public class BuildVO
{
    @ApiModelProperty("构建ID")
    private String buildId;

    @ApiModelProperty("构建号")
    private String buildNum;

    @ApiModelProperty("启动构建时间")
    private Long buildTime;

    @ApiModelProperty("启动构建的用户")
    private String buildUser;
}
