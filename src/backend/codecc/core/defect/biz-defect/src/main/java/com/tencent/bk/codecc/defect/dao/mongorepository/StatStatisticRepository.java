package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.StatStatisticEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatStatisticRepository extends MongoRepository<StatStatisticEntity, String> {

    List<StatStatisticEntity> findByTaskIdAndBuildIdAndToolName(long taskId, String buildId, String toolName);

    StatStatisticEntity findFirstByTaskIdAndToolNameOrderByTimeDesc(long taskId, String toolName);
}
