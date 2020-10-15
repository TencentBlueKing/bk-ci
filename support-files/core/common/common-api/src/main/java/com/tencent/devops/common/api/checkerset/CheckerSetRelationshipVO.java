package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Data;

import javax.ws.rs.PathParam;
import java.util.Set;

/**
 * 规则集与其他对象关系视图
 *
 * @version V1.0
 * @date 2020/1/5
 */
@Data
@ApiModel("规则集与其他对象关系视图")
public class CheckerSetRelationshipVO
{
    @ApiModelProperty(value = "关系类型", required = true)
    private String type;

    @ApiModelProperty(value = "项目ID")
    private String projectId;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "规则集版本号")
    Integer version;

    @ApiModelProperty(value = "规则集集合")
    Set<String> checkerSetIds;
}
