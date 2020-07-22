package com.tencent.bk.codecc.defect.model.checkerset;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 规则集版本实体
 *
 * @version V4.0
 * @date 2019/11/1
 */
@Data
public class CheckerSetVersionEntity
{
    /**
     * 规则集ID
     */
    @Field("checker_set_id")
    private String checkerSetId;

    /**
     * 版本号
     */
    private Integer version;
}
