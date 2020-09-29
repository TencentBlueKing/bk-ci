package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CovSubcategoryModel {

    @JsonProperty("checker_subcategory_key")
    private String checkerSubcategoryKey;

    @JsonProperty("checker_subcategory_name")
    private String checkerSubcategoryName;

    @JsonProperty("checker_subcategory_detail")
    private String checkerSubcategoryDetail;

    @JsonProperty("checker_key")
    private String checkerKey;

    @JsonProperty("checker_name")
    private String checkerName;

    private int language;
}
