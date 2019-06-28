package com.tencent.bk.devops.atom.pojo;

import com.tencent.bk.devops.atom.common.DataType;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * "out_var_2": {
 * "type": "artifact",
 * "value": ["file_path_1", "file_path_2"] # 本地文件路径，指定后，agent自动将这些文件归档到仓库
 * }
 *
 * @version 1.0
 */
@Getter
@Setter
@SuppressWarnings("all")
public class ArtifactData extends DataField {

    private Set<String> value;

    public ArtifactData(Set<String> value) {
        super(DataType.artifact);
        this.value = value;
    }
}
