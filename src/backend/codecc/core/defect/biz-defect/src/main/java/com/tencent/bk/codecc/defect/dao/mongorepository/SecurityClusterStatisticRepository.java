package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.SecurityClusterStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SecurityClusterStatisticRepository extends MongoRepository<SecurityClusterStatisticEntity, String> {
    SecurityClusterStatisticEntity findByTaskIdAndBuildId(Long taskId, String buildId);
}
