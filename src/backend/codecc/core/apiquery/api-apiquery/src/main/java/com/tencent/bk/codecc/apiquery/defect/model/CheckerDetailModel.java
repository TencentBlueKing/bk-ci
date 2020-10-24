package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class CheckerDetailModel {

    /**
     * 工具名
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 告警类型key，唯一标识，如：qoc_lua_UseVarIfNil
     */
    @JsonProperty("checker_key")
    private String checkerKey;

    /**
     * 规则名称
     */
    @JsonProperty("checker_name")
    private String checkerName;

    /**
     * 规则详细描述
     */
    @JsonProperty("checker_desc")
    private String checkerDesc;

    /**
     * 规则详细描述-带占位符
     */
    @JsonProperty("checker_desc_model")
    private String checkerDescModel;

    /**
     * 规则严重程度，1=>严重，2=>一般，3=>提示
     */
    private int severity;

    /**
     * 规则所属语言（针对KLOCKWORK）
     */
    @JsonProperty("language")
    private long language;

    /**
     * 规则状态 0=>打开 1=>关闭;
     */
    private int status;

    /**
     * 规则类型
     */
    @JsonProperty("checker_type")
    private String checkerType;

    /**
     * 规则类型说明
     */
    @JsonProperty("checker_type_desc")
    private String checkerTypeDesc;

    /**
     * 规则类型排序序列号
     */
    @JsonProperty("checker_type_sort")
    private String checkerTypeSort;

    /**
     * 所属规则包
     */
    @JsonProperty("pkg_kind")
    private String pkgKind;

    /**
     * 项目框架（针对Eslint工具,目前有vue,react,standard）
     */
    @JsonProperty("framework_type")
    private String frameworkType;

    /**
     * 规则配置
     */
    private String props;

    /**
     * 规则所属标准
     */
    private int standard;

    /**
     * 规则是否支持配置true：支持;空或false：不支持
     */
    private Boolean editable;

    /**
     * 示例代码
     */
    @JsonProperty("code_example")
    private String codeExample;

    /**
     * 规则子类
     */
    @JsonProperty("cov_issue_type")
    private String covIssueType;

    /**
     * 规则子类
     */
    @JsonProperty("cov_property")
    private int covProperty;

    /**
     * 是否原生规则
     */
    @JsonProperty("native_checker")
    private Boolean nativeChecker;


    /**
     * 是否原生规则
     */
    @JsonProperty("cov_subcategory")
    private List<CovSubcategoryModel> covSubcategory;



    /*-------------------根据改动新增规则字段---------------------*/
    /**
     * 规则对应语言，都存文字，mongodb对按位与不支持
     */
    @JsonProperty("checker_language")
    private Set<String> checkerLanguage;

    /**
     * 规则类型
     */
    @JsonProperty("checker_category")
    private String checkerCategory;

    /**
     * 规则标签
     */
    @JsonProperty("checker_tag")
    private Set<String> checkerTag;

    /**
     * 规则推荐类型
     */
    @JsonProperty("checker_recommend")
    private String checkerRecommend;

    @JsonProperty("err_example")
    private String errExample;

    @JsonProperty("right_example")
    private String rightExample;
}
