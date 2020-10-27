package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 更新
     *
     * @param taskId
     * @param toolName
     * @param forceFullScan
     * @param baseBuildId
     */
    public void upsert(long taskId, String toolName, String forceFullScan, String baseBuildId)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName));

        Update update = new Update();
        update.set("task_id", taskId)
                .set("tool_name", toolName);
        if (StringUtils.isNotEmpty(forceFullScan))
        {
            update.set("force_full_scan", forceFullScan);
        }
        if (StringUtils.isNotEmpty(baseBuildId))
        {
            update.set("defect_base_build_id", baseBuildId);
        }

        mongoTemplate.upsert(query, update, ToolBuildInfoEntity.class);
    }

    /**
     * 设置强制全量扫描标志
     *
     * @param taskId
     * @param toolName
     */
    public void setForceFullScan(long taskId, String toolName)
    {
        upsert(taskId, toolName, ComConstants.CommonJudge.COMMON_Y.value(), null);
    }

    /**
     * 设置强制全量扫描标志
     *
     * @param taskId
     * @param toolName
     */
    public void clearForceFullScan(long taskId, String toolName)
    {
        upsert(taskId, toolName, ComConstants.CommonJudge.COMMON_N.value(), null);
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
        upsert(taskId, toolName, null, buildId);
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
        ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
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
}
