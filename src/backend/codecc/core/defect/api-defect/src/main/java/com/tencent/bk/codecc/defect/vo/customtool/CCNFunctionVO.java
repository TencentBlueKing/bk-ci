package com.tencent.bk.codecc.defect.vo.customtool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CCNFunctionVO
{
    /**
     * 签名，用于唯一认证
     */
    private String funcSignature;

    /**
     * 任务id
     */
    private long taskId;

    /**
     * 方法名
     */
    @JsonProperty("function_name")
    private String functionName;

    /**
     * 方法的完整名称
     */
    @JsonProperty("long_name")
    private String longName;

    /**
     * 圈复杂度
     */
    private int ccn;

    /**
     * 方法最后更新时间
     */
    @JsonProperty("latest_datetime")
    private Long latestDateTime;

    /**
     * 方法最后更新作者
     */
    private String author;

    /**
     * 方法开始行号
     */
    @JsonProperty("startLine")
    private Integer startLines;

    /**
     * 方法结束行号
     */
    @JsonProperty("endLine")
    private Integer endLines;

    /**
     * 方法总行数
     */
    @JsonProperty("total_lines")
    private Integer totalLines;

    /**
     * 包含圈复杂度计算节点的行号
     */
    @JsonProperty("condition_lines")
    private String conditionLines;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    private int status;

    /**
     * 风险系数，极高-1, 高-2，中-4，低-8
     * 该参数不入库，因为风险系数是可配置的
     */
    private int riskFactor;

    /**
     * 告警创建时间
     */
    private Long createTime;

    /**
     * 告警修复时间
     */
    private Long fixedTime;

    /**
     * 告警忽略时间
     */
    private Long ignoreTime;

    /**
     * 告警屏蔽时间
     */
    private Long excludeTime;

    /**
     * 文件相对路径
     */
    @JsonProperty("rel_path")
    private String relPath;

    /**
     * 文件路径
     */
    @JsonProperty("filePath")
    private String filePath;

    /**
     * 代码仓库地址
     */
    private String url;

    /**
     * 仓库id
     */
    @JsonProperty("repo_id")
    private String repoId;

    /**
     * 文件版本号
     */
    private String revision;

    /**
     * 分支名
     */
    private String branch;

    /**
     * Git子模块
     */
    @JsonProperty("sub_module")
    private String subModule;

    /**
     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复，格式：
     * ANALYSIS_VERSION:projId:toolName
     */
    private String analysisVersion;

    /**
     * 创建时的构建号
     */
    private String createBuildNumber;

    /**
     * 修复时的构建号
     */
    private String fixedBuildNumber;


    /**
     * pinpoint hash值
     */
    private String pinpointHash;
}
