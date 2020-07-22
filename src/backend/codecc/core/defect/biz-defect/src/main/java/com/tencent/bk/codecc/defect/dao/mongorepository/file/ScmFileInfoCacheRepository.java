package com.tencent.bk.codecc.defect.dao.mongorepository.file;

import com.tencent.bk.codecc.defect.model.file.ScmFileInfoCacheEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 构建与遗留告警快照
 *
 * @version V1.0
 * @date 2019/12/16
 */
@Repository
public interface ScmFileInfoCacheRepository extends MongoRepository<ScmFileInfoCacheEntity, String>
{
    @Query(fields = "{'file_path':1, 'file_rel_path':1, 'file_md5': 1, 'file_update_time': 1}", value = "{'task_id': ?0, 'tool_name': ?1}")
    List<ScmFileInfoCacheEntity> findSimpleByTaskIdAndToolName(long taskId, String toolName);

    List<ScmFileInfoCacheEntity> findByTaskIdAndToolName(long taskId, String toolName);
}
