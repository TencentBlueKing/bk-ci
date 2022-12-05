package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.TaskPersonalStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskPersonalStatisticRepository extends MongoRepository<TaskPersonalStatisticEntity, String> {

    TaskPersonalStatisticEntity findFirstByTaskIdAndUsername(long taskId, String username);

    List<TaskPersonalStatisticEntity> findByTaskId(long taskId);
}
