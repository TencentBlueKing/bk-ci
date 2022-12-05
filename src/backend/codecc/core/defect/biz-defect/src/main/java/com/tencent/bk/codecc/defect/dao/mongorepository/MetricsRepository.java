package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.MetricsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricsRepository extends MongoRepository<MetricsEntity, String> {
    MetricsEntity findFirstByTaskIdAndBuildId(Long taskId, String buildId);
}
