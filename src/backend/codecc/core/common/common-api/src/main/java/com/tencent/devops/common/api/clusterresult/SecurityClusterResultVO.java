package com.tencent.devops.common.api.clusterresult;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SecurityClusterResultVO extends BaseClusterResultVO {
    public SecurityClusterResultVO(String type, Integer totalCount, List<String> toolList) {
        this.setToolNum(totalCount);
        this.setType(type);
        this.setToolList(toolList);
    }

    @ApiModelProperty
    private Integer totalCount;

    @ApiModelProperty
    private Integer newDefectCount;

    @ApiModelProperty
    private Integer fixDefectCount;

    @ApiModelProperty
    private Integer maskDefectCount;
}
