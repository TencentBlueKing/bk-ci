package com.tencent.bk.devops.atom.pojo;

import com.tencent.bk.devops.atom.common.DataType;
import lombok.Getter;
import lombok.Setter;

/**
 * "out_var_2": {
 * "type": "string",
 * "value": "xxxx"
 * }
 *
 * @version 1.0
 */
@Getter
@Setter
@SuppressWarnings("all")
public class StringData extends DataField {

    public StringData(String value) {
        super(DataType.string);
        this.value = value;
    }

    private String value;
}
