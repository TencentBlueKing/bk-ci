package com.tencent.bk.codecc.defect.model.checkerset;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/1/5
 */
@Data
@Document(collection = "t_checker_set_task_relationship")
@CompoundIndexes({
        @CompoundIndex(name = "checker_set_id_1_task_id_1", def = "{'checker_set_id': 1, 'task_id': 1}")
})
public class CheckerSetTaskRelationshipEntity extends CommonEntity
{
    /**
     * 规则集ID
     */
    @Field("checker_set_id")
    @Indexed
    private String checkerSetId;

    /**
     * 任务ID
     */
    @Field("task_id")
    @Indexed
    private Long taskId;

    /**
     * 项目ID
     */
    @Field("project_id")
    @Indexed
    private String projectId;
}
