package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskLogOverviewRepository extends MongoRepository<TaskLogOverviewEntity, String> {
    TaskLogOverviewEntity findFirstByTaskIdOrderByStartTimeDesc(long taskId);

    TaskLogOverviewEntity findFirstByTaskIdAndBuildId(Long taskId, String buildId);

    TaskLogOverviewEntity findFirstByTaskIdAndStatusOrderByStartTimeDesc(long taskId, int status);

    TaskLogOverviewEntity findFirstByTaskIdAndBuildIdAndStatus(long taskId, String buildId, int status);

    Long countByTaskId(Long taskId);

    TaskLogOverviewEntity findFirstByTaskIdAndBuildNum(Long taskId, String buildNum);
}
