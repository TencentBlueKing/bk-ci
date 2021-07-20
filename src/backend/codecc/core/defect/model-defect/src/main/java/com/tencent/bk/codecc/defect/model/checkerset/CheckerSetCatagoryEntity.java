package com.tencent.bk.codecc.defect.model.checkerset;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/1/7
 */
@Data
public class CheckerSetCatagoryEntity
{
    /**
     * 英文名称
     */
    @Field("en_name")
    private String enName;

    /**
     * 中文名称
     */
    @Field("cn_name")
    private String cnName;
}
