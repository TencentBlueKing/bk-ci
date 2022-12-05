package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.incremental.NeedClearTempFileBuildEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 待清除临时文件信息的构建仓库接口
 *
 * @version V1.0
 * @date 2019/12/17
 */
public interface NeedClearTempFileBuildRepository extends MongoRepository<NeedClearTempFileBuildEntity, String>
{
    /**
     * 根据工具名称和构建ID查询
     *
     * @param toolName
     * @param buildId
     * @return
     */
    NeedClearTempFileBuildEntity findFirstByToolNameAndBuildId(String toolName, String buildId);

    /**
     * 根据任务ID和工具名称查询
     *
     * @param taskId
     * @param toolName
     * @return
     */
    List<NeedClearTempFileBuildEntity> findByTaskIdAndToolName(long taskId, String toolName);
}
