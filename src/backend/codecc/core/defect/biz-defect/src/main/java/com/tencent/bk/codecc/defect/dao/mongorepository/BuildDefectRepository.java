package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 构建与遗留告警快照
 *
 * @version V1.0
 * @date 2019/12/16
 */
@Repository
public interface BuildDefectRepository extends MongoRepository<BuildDefectEntity, String>
{
    /**
     * 根据工具名称和构建号查询
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @return
     */
    List<BuildDefectEntity> findByTaskIdAndToolNameAndBuildId(long taskId, String toolName, String buildId);

    /**
     * 根据工具名称和构建号查询
     *
     * @param taskId
     * @param toolNameSet
     * @param buildId
     * @return
     */
    List<BuildDefectEntity> findByTaskIdAndToolNameInAndBuildId(long taskId, List<String> toolNameSet, String buildId);
}
