package com.tencent.bk.codecc.defect.model.pipelinereport;

import com.tencent.bk.codecc.defect.model.RedLineMetaEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 质量红线数据实体类
 *
 * @version V1.0
 * @date 2019/12/19
 */
@Data
@Document(collection = "t_red_line")
public class RedLineEntity extends RedLineMetaEntity
{
    /**
     * 构建ID
     */
    @Field("build_id")
    private String buildId;

    /**
     * 数据的值
     */
    @Field("value")
    private String value;
}
