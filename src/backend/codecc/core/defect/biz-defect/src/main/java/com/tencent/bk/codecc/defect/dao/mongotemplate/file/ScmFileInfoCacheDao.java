package com.tencent.bk.codecc.defect.dao.mongotemplate.file;

import com.tencent.bk.codecc.defect.model.file.ScmFileInfoCacheEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class ScmFileInfoCacheDao
{

    @Autowired
    private MongoTemplate mongoTemplate;

    public void batchSave(List<ScmFileInfoCacheEntity> fileInfoEntities)
    {
        if (CollectionUtils.isNotEmpty(fileInfoEntities))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ScmFileInfoCacheEntity.class);
            for (ScmFileInfoCacheEntity fileInfo : fileInfoEntities)
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(fileInfo.getTaskId()));
                query.addCriteria(Criteria.where("tool_name").is(fileInfo.getToolName()));
                query.addCriteria(Criteria.where("file_rel_path").is(fileInfo.getFileRelPath()));

                Update update = new Update();
                update.set("file_md5", fileInfo.getMd5())
                        .set("file_update_time", fileInfo.getFileUpdateTime())
                        .set("branch", fileInfo.getBranch())
                        .set("file_path", fileInfo.getFilePath())
                        .set("file_rel_path", fileInfo.getFileRelPath())
                        .set("revision", fileInfo.getRevision())
                        .set("scm_type", fileInfo.getScmType())
                        .set("url", fileInfo.getUrl())
                        .set("build_id", fileInfo.getBuildId())
                        .set("change_records", fileInfo.getChangeRecords());

                ops.upsert(query, update);
            }
            ops.execute();
        }
    }
}
