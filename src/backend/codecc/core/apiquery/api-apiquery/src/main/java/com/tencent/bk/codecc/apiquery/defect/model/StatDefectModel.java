package com.tencent.bk.codecc.apiquery.defect.model;

import java.util.Map;
import lombok.Data;

@Data
public class StatDefectModel extends CommonModel {
    Map<String, Object> statInfo;

    public StatDefectModel(Map<String, Object> statInfo) {
        this.statInfo = statInfo;
    }
}
