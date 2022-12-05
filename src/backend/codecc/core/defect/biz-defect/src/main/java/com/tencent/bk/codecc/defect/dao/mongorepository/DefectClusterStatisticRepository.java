package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.DefectClusterStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DefectClusterStatisticRepository extends MongoRepository<DefectClusterStatisticEntity, String> {
    DefectClusterStatisticEntity findFirstByTaskIdAndBuildId(Long taskId, String buildId);
}
