package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.CcnClusterStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CcnClusterStatisticRepository extends MongoRepository<CcnClusterStatisticEntity, String> {
    CcnClusterStatisticEntity findFirstByTaskIdOrderByTimeDesc(long taskId);

    CcnClusterStatisticEntity findFirstByTaskIdAndBuildId(Long taskId, String buildId);
}
