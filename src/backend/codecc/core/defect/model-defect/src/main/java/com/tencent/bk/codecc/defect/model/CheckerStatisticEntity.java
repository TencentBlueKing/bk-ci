package com.tencent.bk.codecc.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class CheckerStatisticEntity {

    private String id;

    private String name;

    @Field("defect_count")
    @JsonProperty("defect_count")
    private int defectCount;

    // 1=>严重，2=>一般，3=>提示
    private int severity;
}
