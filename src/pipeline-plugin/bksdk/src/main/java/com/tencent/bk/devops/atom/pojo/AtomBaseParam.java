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
    @JsonProperty("BK_CI_PIPELINE_VERSION")
    private String pipelineVersion;

    /**
     * 项目名称
     */
    @JsonProperty("BK_CI_PROJECT_NAME")
    private String projectName;

    /**
     * 项目中文名称
     */
    @JsonProperty("BK_CI_PROJECT_NAME_CN")
    private String projectNameCn;

    /**
     * 流水线Id
     */
    @JsonProperty("BK_CI_PIPELINE_ID")
    private String pipelineId;

    /**
     * 流水线构建序号
     */
    @JsonProperty("BK_CI_BUILD_NUM")
    private String pipelineBuildNum;

    /**
     * 流水线构建Id
     */
    @JsonProperty("BK_CI_BUILD_ID")
    private String pipelineBuildId;

    /**
     * 流水线名称
     */
    @JsonProperty("BK_CI_PIPELINE_NAME")
    private String pipelineName;

    /**
     * 流水线启动时间：毫秒
     */
    @JsonProperty("BK_CI_BUILD_START_TIME")
    private String pipelineStartTimeMills;

    /**
     * 流水线触发人
     */
    @JsonProperty("BK_CI_START_USER_NAME")
    private String pipelineStartUserName;

    /**
     * 插件敏感信息
     */
    @JsonProperty(value = "bkSensitiveConfInfo",access = JsonProperty.Access.WRITE_ONLY)
    private Map<String,String> bkSensitiveConfInfo;

    /**
     * 流水线当前插件id
     */
    @JsonProperty("BK_CI_BUILD_TASK_ID")
    private String pipelineTaskId;

}
