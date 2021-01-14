package com.tencent.bk.codecc.defect.model.checkerset;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Data
public class ToolCheckerSetEntity
{
    /**
     * 工具名称
     */
    @Field("tool_name")
    private String toolName;

    /**
     * 规则集ID
     */
    @Field("checker_set_id")
    private String checkerSetId;

    /**
     * 版本号
     */
    private int version;
}
