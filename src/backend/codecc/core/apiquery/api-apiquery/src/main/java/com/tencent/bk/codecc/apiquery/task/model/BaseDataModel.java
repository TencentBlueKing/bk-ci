package com.tencent.bk.codecc.apiquery.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.apiquery.defect.model.OpenSourceCheckerSet;
import lombok.Data;

import java.util.List;

/**
 * 基础数据表，要保持这个表只有少量的数据
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Data
public class BaseDataModel {

    @JsonProperty("param_code")
    private String paramCode;

    @JsonProperty("param_name")
    private String paramName;

    @JsonProperty("param_value")
    private String paramValue;

    @JsonProperty("param_type")
    private String paramType;

    @JsonProperty("param_status")
    private String paramStatus;

    @JsonProperty("param_extend1")
    private String paramExtend1;

    @JsonProperty("param_extend2")
    private String paramExtend2;

    @JsonProperty("param_extend3")
    private String paramExtend3;

    @JsonProperty("param_extend4")
    private String paramExtend4;

    @JsonProperty("param_extend5")
    private String paramExtend5;

    @JsonProperty("lang_full_key")
    private String langFullKey;

    @JsonProperty("lang_type")
    private String langType;

    @JsonProperty("open_source_checker_sets")
    private List<OpenSourceCheckerSet> openSourceCheckerSets;
}
