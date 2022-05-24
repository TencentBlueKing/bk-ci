package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工具构建运行时栈表持久化，每次启动分析是生成
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Repository
public interface ToolBuildStackRepository extends MongoRepository<ToolBuildStackEntity, String>
{
    /**
     * 根据任务ID和工具名称、buildId查询
     *
     * @param taskId
     * @param toolName
     * @return
     */
    ToolBuildStackEntity findFirstByTaskIdAndToolNameAndBuildId(long taskId, String toolName, String buildId);

    /**
     * 根据任务ID和工具名称查询
     *
     * @param taskId
     * @param toolNames
     * @param buildId
     * @return
     */
    List<ToolBuildStackEntity> findByTaskIdAndToolNameInAndBuildId(long taskId, List<String> toolNames, String buildId);
}
