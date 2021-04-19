package com.tencent.bk.codecc.codeccjob.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.TaskPersonalStatisticEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public class TaskPersonalStatisticDao {

    @Autowired
    MongoTemplate mongoTemplate;

    public boolean batchSave(Collection<TaskPersonalStatisticEntity> entities) {
        if (entities.isEmpty()) {
            return false;
        }
        mongoTemplate.insert(entities, TaskPersonalStatisticEntity.class);
        return true;
    }
}
