package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Coverity规则子选项
 *
 * @version V4.0
 * @date 2019/10/29
 */
@Data
public class CovSubcategoryEntity
{
    @Field("checker_subcategory_key")
    private String checkerSubcategoryKey;

    @Field("checker_subcategory_name")
    private String checkerSubcategoryName;

    @Field("checker_subcategory_detail")
    private String checkerSubcategoryDetail;

    @Field("checker_key")
    private String checkerKey;

    @Field("checker_name")
    private String checkerName;

    private int language;
}
