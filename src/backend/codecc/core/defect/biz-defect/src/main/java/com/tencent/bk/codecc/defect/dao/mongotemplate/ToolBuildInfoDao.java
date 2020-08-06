package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.redis.lock.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 工具构建信息DAO
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Repository
@Slf4j
public class ToolBuildInfoDao
{
    /**
     * 字符串锁前缀
     */
    private static final String LOCK_KEY_PREFIX = "UPDATE_TOOL_BUILD_INFO:";

    /**
     * 分布式锁超时时间
     */
    private static final Long LOCK_TIMEOUT = 20L;

    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 设置强制全量扫描标志
     *
     * @param taskId
     * @param toolName
     */
    public void setForceFullScan(long taskId, String toolName)
    {
        RedisLock lock = getLock(taskId, toolName);
        try
        {
            ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
            if (toolBuildInfoEntity == null || !ComConstants.CommonJudge.COMMON_Y.value().equals(toolBuildInfoEntity.getForceFullScan()))
            {
                boolean needUpdate = false;
                lock.lock();
                toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
                if (toolBuildInfoEntity == null)
                {
                    toolBuildInfoEntity = new ToolBuildInfoEntity();
                    toolBuildInfoEntity.setTaskId(taskId);
                    toolBuildInfoEntity.setToolName(toolName);
                    toolBuildInfoEntity.setForceFullScan(ComConstants.CommonJudge.COMMON_Y.value());
                    needUpdate = true;
                }

                if (toolBuildInfoEntity != null && !ComConstants.CommonJudge.COMMON_Y.value().equals(toolBuildInfoEntity.getForceFullScan()))
                {
                    toolBuildInfoEntity.setForceFullScan(ComConstants.CommonJudge.COMMON_Y.value());
                    needUpdate = true;
                }
                if (needUpdate)
                {
                    toolBuildInfoRepository.save(toolBuildInfoEntity);
                }
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }

    /**
     * 设置强制全量扫描标志
     *
     * @param taskId
     * @param toolName
     */
    public void clearForceFullScan(long taskId, String toolName)
    {
        RedisLock lock = getLock(taskId, toolName);
        try
        {
            ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
            if (toolBuildInfoEntity != null && ComConstants.CommonJudge.COMMON_Y.value().equals(toolBuildInfoEntity.getForceFullScan()))
            {
                lock.lock();
                toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
                if (toolBuildInfoEntity != null && ComConstants.CommonJudge.COMMON_Y.value().equals(toolBuildInfoEntity.getForceFullScan()))
                {
                    toolBuildInfoEntity.setForceFullScan(ComConstants.CommonJudge.COMMON_N.value());
                    toolBuildInfoRepository.save(toolBuildInfoEntity);
                }
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }

    /**
     * 更新删除文件列表
     *
     * @param taskId
     * @param toolName
     * @param deleteFiles
     * @param updateType
     */
    public void updateDeleteFiles(long taskId, String toolName, List<String> deleteFiles, DefectConstants.UpdateToolDeleteFileType updateType)
    {
        RedisLock lock = getLock(taskId, toolName);
        try
        {
            ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
            if (needUpdateDeleteFiles(toolBuildInfoEntity, deleteFiles, updateType))
            {
                lock.lock();
                toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
                if (needUpdateDeleteFiles(toolBuildInfoEntity, deleteFiles, updateType))
                {
                    if (toolBuildInfoEntity == null)
                    {
                        toolBuildInfoEntity = new ToolBuildInfoEntity();
                        toolBuildInfoEntity.setTaskId(taskId);
                        toolBuildInfoEntity.setToolName(toolName);
                    }
                    updateDeleteFiles(toolBuildInfoEntity, deleteFiles, updateType);
                    toolBuildInfoRepository.save(toolBuildInfoEntity);
                }
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }

    /**
     * 更新告警快照基准构建ID
     *
     * @param taskId
     * @param toolName
     * @param buildId
     */
    public void updateDefectBaseBuildId(long taskId, String toolName, String buildId)
    {
        RedisLock lock = getLock(taskId, toolName);
        try
        {
            ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
            if (toolBuildInfoEntity == null || !buildId.equals(toolBuildInfoEntity.getDefectBaseBuildId()))
            {
                lock.lock();
                boolean needUpdate = false;
                toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
                if (toolBuildInfoEntity == null)
                {
                    toolBuildInfoEntity = new ToolBuildInfoEntity();
                    toolBuildInfoEntity.setTaskId(taskId);
                    toolBuildInfoEntity.setToolName(toolName);
                    toolBuildInfoEntity.setDefectBaseBuildId(buildId);
                    needUpdate = true;
                }
                if (toolBuildInfoEntity != null && !buildId.equals(toolBuildInfoEntity.getDefectBaseBuildId()))
                {
                    toolBuildInfoEntity.setDefectBaseBuildId(buildId);
                    needUpdate = true;
                }
                if (needUpdate)
                {
                    toolBuildInfoRepository.save(toolBuildInfoEntity);
                }
            }
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }

    private void updateDeleteFiles(ToolBuildInfoEntity toolBuildInfoEntity, List<String> deleteFiles,
            DefectConstants.UpdateToolDeleteFileType updateType)
    {
        Set<String> toolDeleteFileSet;
        if (toolBuildInfoEntity.getDeleteFiles() != null)
        {
            toolDeleteFileSet = Sets.newHashSet(toolBuildInfoEntity.getDeleteFiles());
        }
        else
        {
            toolDeleteFileSet = Sets.newHashSet();
        }
        if (DefectConstants.UpdateToolDeleteFileType.ADD == updateType)
        {
            toolDeleteFileSet.addAll(deleteFiles);
        }
        else if (DefectConstants.UpdateToolDeleteFileType.REMOVE == updateType)
        {
            toolDeleteFileSet.removeAll(deleteFiles);
        }
        toolBuildInfoEntity.setDeleteFiles(Lists.newArrayList(toolDeleteFileSet));
    }

    private RedisLock getLock(long taskId, String toolName)
    {
        RedisLock lock = new RedisLock(redisTemplate, LOCK_KEY_PREFIX + taskId + ComConstants.SEPARATOR_SEMICOLON + toolName, LOCK_TIMEOUT);

        return lock;
    }

    private boolean needUpdateDeleteFiles(ToolBuildInfoEntity toolBuildInfoEntity, List<String> deleteFiles, DefectConstants.UpdateToolDeleteFileType updateType)
    {
        if (CollectionUtils.isEmpty(deleteFiles))
        {
            return false;
        }

        if (toolBuildInfoEntity == null)
        {
            return true;
        }

        if (DefectConstants.UpdateToolDeleteFileType.ADD == updateType)
        {
            if (CollectionUtils.isEmpty(toolBuildInfoEntity.getDeleteFiles()) || !toolBuildInfoEntity.getDeleteFiles().containsAll(deleteFiles))
            {
                return true;
            }
        }

        if (DefectConstants.UpdateToolDeleteFileType.REMOVE == updateType)
        {
            if (CollectionUtils.isNotEmpty(toolBuildInfoEntity.getDeleteFiles()))
            {
                for (String currentDeleteFile : toolBuildInfoEntity.getDeleteFiles())
                {
                    if (deleteFiles.contains(currentDeleteFile))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 批量设置强制全量扫描标志
     *
     * @param toolBuildInfoEntities
     */
    public void batchSetForceFullScan(List<ToolBuildInfoEntity> toolBuildInfoEntities)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ToolBuildInfoEntity.class);

        for (ToolBuildInfoEntity toolBuildInfoEntity : toolBuildInfoEntities)
        {
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(new ObjectId(toolBuildInfoEntity.getEntityId())));
            Update update = new Update();
            update.set("force_full_scan", "Y");
            ops.updateOne(query, update);
        }
        ops.execute();
    }
}
