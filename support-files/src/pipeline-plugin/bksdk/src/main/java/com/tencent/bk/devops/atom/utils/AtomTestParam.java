package com.tencent.bk.devops.atom.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.devops.atom.utils.json.annotation.SkipLogField;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 流水线插件基础参数, 所有插件参数继承扩展他增加自己的定义
 *
 * @version 1.0
 */
@Getter
@Setter
public class AtomTestParam {

    /**
     * 工作空间
     */
    @JsonProperty("bkWorkspace")
    private String bkWorkspace;

    /**
     * 插件敏感信息
     */
    @JsonProperty(value = "bkSensitiveConfInfo",access = JsonProperty.Access.WRITE_ONLY)
    private Map<String,String> bkSensitiveConfInfo;

    public AtomTestParam() {
    }

    public AtomTestParam(String bkWorkspace, Map<String, String> bkSensitiveConfInfo) {
        this.bkWorkspace = bkWorkspace;
        this.bkSensitiveConfInfo = bkSensitiveConfInfo;
    }
}
