package com.tencent.bk.devops.atom.pojo.quality;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QualityValue {

    public QualityValue(String value) {
        this.value = value;
    }

    /**
     * 数值
     */
    private String value;
}
