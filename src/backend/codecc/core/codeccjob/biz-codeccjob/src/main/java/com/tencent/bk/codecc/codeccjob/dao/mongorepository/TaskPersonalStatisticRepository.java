package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.TaskPersonalStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskPersonalStatisticRepository extends MongoRepository<TaskPersonalStatisticEntity, String> {

    void deleteByTaskId(long taskId);
}
