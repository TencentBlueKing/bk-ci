package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CheckerConfigEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 描述
 *
 * @version V2.0
 * @date 2020/04/16
 */
@Slf4j
@Repository
public class BuildDefectDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    public void upsertByFilePath(Collection<BuildDefectEntity> buildDefectEntityList)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, BuildDefectEntity.class);
        if (CollectionUtils.isNotEmpty(buildDefectEntityList))
        {
            for (BuildDefectEntity buildDefectEntity : buildDefectEntityList)
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(buildDefectEntity.getTaskId())
                        .and("tool_name").is(buildDefectEntity.getToolName())
                        .and("build_id").is(buildDefectEntity.getBuildId())
                        .and("file_path").is(buildDefectEntity.getFilePath()));
                Update update = new Update();
                update.set("task_id", buildDefectEntity.getTaskId())
                        .set("tool_name", buildDefectEntity.getToolName())
                        .set("build_id", buildDefectEntity.getBuildId())
                        .set("build_num", buildDefectEntity.getBuildNum())
                        .set("file_path", buildDefectEntity.getFilePath())
                        .set("file_defect_ids", buildDefectEntity.getFileDefectIds());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }


    public void upsertByDefectId(Collection<BuildDefectEntity> buildDefectEntityList)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, BuildDefectEntity.class);
        if (CollectionUtils.isNotEmpty(buildDefectEntityList))
        {
            for (BuildDefectEntity buildDefectEntity : buildDefectEntityList)
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(buildDefectEntity.getTaskId())
                        .and("tool_name").is(buildDefectEntity.getToolName())
                        .and("build_id").is(buildDefectEntity.getBuildId())
                        .and("defect_id").is(buildDefectEntity.getDefectId()));
                Update update = new Update();
                update.set("task_id", buildDefectEntity.getTaskId())
                        .set("tool_name", buildDefectEntity.getToolName())
                        .set("build_id", buildDefectEntity.getBuildId())
                        .set("build_num", buildDefectEntity.getBuildNum())
                        .set("defect_id", buildDefectEntity.getDefectId());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }
}
