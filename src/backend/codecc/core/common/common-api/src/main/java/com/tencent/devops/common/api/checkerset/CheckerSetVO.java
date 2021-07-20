package com.tencent.devops.common.api.checkerset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Set;

/**
 * 规则集视图
 *
 * @version V4.0
 * @date 2019/10/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则集视图")
public class CheckerSetVO extends CommonVO
{
    /**
     * 规则集ID
     */
    @ApiModelProperty(value = "规则集ID", required = true)
    @Pattern(regexp = "^[0-9a-zA-Z_]{1,50}$", message = "输入的规则集ID不符合命名规则")
    private String checkerSetId;

    /**
     * 规则集名称
     */
    @ApiModelProperty(value = "规则集名称", required = true)
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,50}", message = "输入的规则集名称不符合命名规则")
    private String checkerSetName;

    /**
     * 规则集支持的语言
     */
    private Long codeLang;

    /**
     * 语言
     */
    private String checkerSetLang;

    /**
     * 规则集支持的语言，给流水线使用
     */
    private List<String> codeLangList;

    /**
     * 规则集包含的工具
     */
    private Set<String> toolList;

    /**
     * 规则集可见范围1：公开；2：仅我的项目；
     */
    @ApiModelProperty(value = "规则集可见范围", required = true)
    private Integer scope;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 最近修改时间
     */
    private Long lastUpdateTime;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 最新版本号
     */
    private Integer latestVersion;

    /**
     * 规则数
     */
    private Integer checkerCount;

    /**
     * 是否推荐
     */
    private Integer recommended;

    /**
     * 是否是默认规则集
     */
    private Boolean defaultCheckerSet;

    /**
     * 规则状态
     */
    private Integer status;

    /**
     * 工具特殊参数
     */
    private String paramJson;

    /**
     * 任务使用量
     */
    private Integer taskUsage;

    /**
     * 规则集包含的规则和参数
     */
    private List<CheckerPropVO> checkerProps;

    /**
     * 是否启用1：启用；2：下架
     */
    private Integer enable;

    /**
     * 排序权重
     */
    private Integer sortWeight;

    /**
     * 规则类型
     */
    private List<CheckerSetCategoryVO> catagories;

    /**
     * 规则集描述
     */
    private String description;

    /**
     * 基准规则集ID
     */
    private String baseCheckerSetId;

    /**
     * 基准规则集版本号
     */
    private Integer baseCheckerSetVersion;

    /**
     * 使用中的任务列表
     */
    private List<Long> tasksInUse;

    /**
     * 任务使用中
     */
    private Boolean taskUsing;

    /**
     * 版本列表
     */
    private List<CheckerSetVersionVO> versionList;

    /**
     * 是否官方
     */
    @JsonIgnore
    private Integer official;

    /**
     * 是否是V2版本规则集，V2版本规则集只用于旧版本流水线插件
     */
    private Boolean legacy;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 是否初始化过规则列表，如果刚创建的规则集没有添加过规则集，值为false，其他情况为true
     */
    private Boolean initCheckers;

    /**
     * 规则集来源
     */
    private String checkerSetSource;

    /**
     * --------------已废弃-----------工具名称
     */
    @JsonIgnore
    private String toolName;

    /**
     * 项目是否已安装
     */
    private Boolean projectInstalled;
}
