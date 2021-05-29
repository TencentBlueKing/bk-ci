package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class CheckerDetailModelWithId extends CheckerDetailModel {

    /**
     * 工具名 + 规则key
     */
    @JsonProperty("id")
    private String id;
}
