package com.tencent.bk.devops.atom.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.devops.atom.utils.json.annotation.SkipLogField;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 流水线插件基础参数, 所有插件参数继承扩展他增加自己的定义
 *
 * @version 1.0
 */
@Getter
@Setter
public class AtomBaseParam {

    /**
     * 工作空间
     */
    @JsonProperty("bkWorkspace")
    private String bkWorkspace;

    /**
     * 是否是测试版本 Y：是 N：否
     */
    @JsonProperty("testVersionFlag")
    private String testVersionFlag;

    /**
     * 流水线版本号
     */
    @JsonProperty("pipeline.version")
    private String pipelineVersion;

    /**
     * 项目名称
     */
    @JsonProperty("project.name")
    private String projectName;

    /**
     * 项目中文名称
     */
    @JsonProperty("project.name.chinese")
    private String projectNameCn;

    /**
     * 流水线Id
     */
    @JsonProperty("pipeline.id")
    private String pipelineId;

    /**
     * 流水线构建序号
     */
    @JsonProperty("pipeline.build.num")
    private String pipelineBuildNum;

    /**
     * 流水线构建Id
     */
    @JsonProperty("pipeline.build.id")
    private String pipelineBuildId;

    /**
     * 流水线名称
     */
    @JsonProperty("pipeline.name")
    private String pipelineName;

    /**
     * 流水线启动时间：毫秒
     */
    @JsonProperty("pipeline.time.start")
    private String pipelineStartTimeMills;

    /**
     * 流水线触发人
     */
    @JsonProperty("pipeline.start.user.name")
    private String pipelineStartUserName;

    /**
     * 插件敏感信息
     */
    @JsonProperty(value = "bkSensitiveConfInfo",access = JsonProperty.Access.WRITE_ONLY)
    private Map<String,String> bkSensitiveConfInfo;

    /**
     * 流水线当前插件id
     */
    @JsonProperty("pipeline.task.id")
    private String pipelineTaskId;

}
