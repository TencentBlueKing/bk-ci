package com.tencent.bk.codecc.defect.model.checkerset;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 规则参数实体
 *
 * @version V4.0
 * @date 2019/11/1
 */
@Data
public class CheckerPropsEntity
{
    /**
     * 规则集所在的工具名称
     */
    @Field("tool_name")
    private String toolName;

    /**
     * 工具特殊参数
     */
    @Field("param_json")
    private String paramJson;

    /**
     * 规则集所在的规则唯一键
     */
    @Field("checker_key")
    private String checkerKey;

    /**
     * 规则集所在的规则名称
     */
    @Field("checker_name")
    private String checkerName;

    /**
     * 规则集所在的工具名称
     */
    private String props;
}
