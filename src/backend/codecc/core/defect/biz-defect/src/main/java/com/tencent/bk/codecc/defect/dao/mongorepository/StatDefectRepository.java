package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.StatDefectEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatDefectRepository extends MongoRepository<StatDefectEntity, String> {

    /**
     * 根据任务id，工具名和状态查询
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    List<StatDefectEntity> findByTaskIdAndToolNameAndStatus(long taskId, String toolName, String status);


}
