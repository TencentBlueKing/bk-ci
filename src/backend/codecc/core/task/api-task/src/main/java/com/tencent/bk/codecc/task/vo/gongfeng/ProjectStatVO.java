package com.tencent.bk.codecc.task.vo.gongfeng;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工蜂项目度量统计视图
 *
 * @version V1.0
 * @date 2019/12/6
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("工蜂项目度量统计视图")
public class ProjectStatVO
{
    @ApiModelProperty("工蜂项目id")
    private Integer id;

    @ApiModelProperty("事业群ID")
    @JsonProperty("bg_id")
    private Integer bgId;

    @ApiModelProperty("项目所属的组织架构,可能有多个用逗号分隔")
    @JsonProperty("org_paths")
    private String orgPaths;

    @ApiModelProperty("项目路径(namespace/project)")
    private String path;

    @ApiModelProperty("项目描述")
    private String description;

    @ApiModelProperty("项目可见性")
    private String visibility;

    @ApiModelProperty("是否开源:私有(0),公共(10)")
    @JsonProperty("visibility_level")
    private Integer visibilityLevel;

    @ApiModelProperty("项目归属:个人(personal);团队(team)")
    private String belong;

    @ApiModelProperty("项目成员user1,user2")
    private String owners;

    @ApiModelProperty("创建时间")
    @JsonProperty("created_at")
    private String createdAt;

    @ApiModelProperty("创建者")
    private String creator;

    @ApiModelProperty("Web访问地址")
    private String url;

    @ApiModelProperty("归档状态:归档项目(true);未归档(false)")
    private Boolean archived;

    @ApiModelProperty("是否为敏感项目")
    @JsonProperty("is_sensitive")
    private Boolean isSensitive;

    @ApiModelProperty("敏感项目的理由,如果该字段为空则表示此项目不是敏感项目")
    @JsonProperty("sensitive_reason")
    private String sensitiveReason;

    @ApiModelProperty("开源可见性[100:全部可见;90:不支持clone与下载;80:仅issue和wiki可见]")
    @JsonProperty("public_visibility")
    private Integer publicVisibility;
}
