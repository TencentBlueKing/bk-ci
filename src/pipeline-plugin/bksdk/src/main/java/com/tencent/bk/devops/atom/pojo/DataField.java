package com.tencent.bk.devops.atom.pojo;

import com.tencent.bk.devops.atom.common.DataType;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @version 1.0
 */
@Setter
@Getter
@SuppressWarnings("all")
public abstract class DataField {

    public DataField(DataType type) {
        this.type = type;
    }

    /**
     * 类型
     */
    private DataType type;
}
