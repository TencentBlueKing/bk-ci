package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 代码仓库信息查询
 *
 * @version V4.0
 * @date 2019/10/16
 */
@Repository
public interface CodeRepoInfoRepository extends MongoRepository<CodeRepoInfoEntity, String>
{
    /**
     * 按任务ID查询
     *
     * @param taskId
     * @param buildId
     * @return
     */
    CodeRepoInfoEntity findByTaskIdAndBuildId(long taskId, String buildId);
}
