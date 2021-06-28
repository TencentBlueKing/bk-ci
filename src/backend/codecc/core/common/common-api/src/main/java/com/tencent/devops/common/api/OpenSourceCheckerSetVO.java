package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import lombok.Data;

@Data
@ApiModel
public class OpenSourceCheckerSetVO {
    @ApiModelProperty
    private String checkerSetId;

    @ApiModelProperty
    private Set<String> toolList;

    @ApiModelProperty
    private String checkerSetType;

    @ApiModelProperty
    private Integer version;
}
