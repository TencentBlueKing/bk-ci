package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.bk.codecc.apiquery.utils.EntityIdDeserializer;
import lombok.Data;

@Data
public class CheckerStatisticModel {
    private String id;

    private String name;

    @JsonProperty("defect_count")
    private int defectCount;

    // 1=>严重，2=>一般，3=>提示
    private int severity;
}
