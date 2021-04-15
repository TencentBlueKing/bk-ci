package com.tencent.devops.common.api.clusterresult;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonTypeInfo(use = Id.NAME, property = "type", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StandardClusterResultVO.class, name = "STANDARD"),
        @JsonSubTypes.Type(value = DefectClusterResultVO.class, name = "DEFECT"),
        @JsonSubTypes.Type(value = DupcClusterResultVO.class, name = "DUPC"),
        @JsonSubTypes.Type(value = CcnClusterResultVO.class, name = "CCN"),
        @JsonSubTypes.Type(value = SecurityClusterResultVO.class, name = "SECURITY")
})
@ApiModel
public class BaseClusterResultVO {
    @ApiModelProperty
    private Long taskId;

    @ApiModelProperty
    private String buildId;

    @ApiModelProperty
    private String type;

    @ApiModelProperty
    private Integer toolNum;

    @ApiModelProperty
    private List<String> toolList;

    @ApiModelProperty
    private Integer totalCount;

    @ApiModelProperty
    private Integer newCount;

    @ApiModelProperty
    private Integer fixCount;

    @ApiModelProperty
    private Integer maskCount;

    @ApiModelProperty
    private Long totalLines;

    @ApiModelProperty
    private Integer ccnBeyondThresholdSum;
}
