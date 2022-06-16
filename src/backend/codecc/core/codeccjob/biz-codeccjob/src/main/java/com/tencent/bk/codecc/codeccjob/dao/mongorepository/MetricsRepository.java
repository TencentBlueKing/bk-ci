package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.MetricsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MetricsRepository extends MongoRepository<MetricsEntity, String> {
    MetricsEntity findFirstByTaskIdAndBuildId(Long taskId, String buildId);
}

