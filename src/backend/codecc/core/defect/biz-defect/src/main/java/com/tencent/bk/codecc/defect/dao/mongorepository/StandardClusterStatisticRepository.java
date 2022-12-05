package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.StandardClusterStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StandardClusterStatisticRepository extends MongoRepository<StandardClusterStatisticEntity, String> {
    StandardClusterStatisticEntity findFirstByTaskIdOrderByTimeDesc(Long taskId);

    StandardClusterStatisticEntity findFirstByTaskIdAndBuildId(Long taskId, String buildId);
}
