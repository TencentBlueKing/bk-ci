package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.*;
import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.NeedClearTempFileBuildEntity;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 描述
 *
 * @version V1.0
 * @date 2019/12/17
 */
@Service
public class BuildDefectServiceImpl implements BuildDefectService
{
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private BuildDefectDao buildDefectDao;

    @Autowired
    private BuildDao buildDao;

    @Autowired
    private NeedClearTempFileBuildRepository needClearTempFileBuildRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Override
    public void updateBaseBuildDefectsAndClearTemp(long taskId, String toolName, String baseBuildId, String buildId, boolean isFullScan,
            List<String> deleteFiles, Set<String> currentBuildRelPaths)
    {
        // 保存本次构建之前遗留告警文件列表快照
        if (!isFullScan && StringUtils.isNotEmpty(baseBuildId))
        {
            List<BuildDefectEntity> baseDefectEntities = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, baseBuildId);
            if (CollectionUtils.isNotEmpty(baseDefectEntities))
            {
                Iterator<BuildDefectEntity> buildDefectEntityIterator = baseDefectEntities.iterator();
                while (buildDefectEntityIterator.hasNext())
                {
                    BuildDefectEntity baseDefectEntity = buildDefectEntityIterator.next();

                    // 本次构建已删除的文件不再保存告警快照
                    if (CollectionUtils.isNotEmpty(deleteFiles) && deleteFiles.contains(baseDefectEntity.getFilePath()))
                    {
                        buildDefectEntityIterator.remove();
                        continue;
                    }

                    // 本次增量告警所在的文件不再保存原有的快照
                    if (CollectionUtils.isNotEmpty(currentBuildRelPaths) && currentBuildRelPaths.contains(baseDefectEntity.getFileRelPath()))
                    {
                        buildDefectEntityIterator.remove();
                        continue;
                    }
                    baseDefectEntity.setBuildId(buildId);
                    baseDefectEntity.setEntityId(null);
                }
                if (CollectionUtils.isNotEmpty(baseDefectEntities))
                {
                    if (ComConstants.ToolPattern.CCN.name().equals(toolMetaCacheService.getToolPattern(toolName)))
                    {
                        buildDefectDao.upsertByDefectId(baseDefectEntities);
                    }
                    else
                    {
                        buildDefectDao.upsertByFilePath(baseDefectEntities);
                    }
                }
            }
        }

        // 更新下次扫描告警快照基准构建号
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);

        // 全量扫描时清除构建号小于基准的临时文件
//        if (isFullScan)
//        {
//            BuildEntity buildEntity = buildDao.getAndSaveBuildInfo(buildId);
//            String baseBuildNum = buildEntity.getBuildNo();
//            int baseNumInt = Integer.valueOf(baseBuildNum);
//            List<NeedClearTempFileBuildEntity> needClearTempFileBuildEntities = needClearTempFileBuildRepository.findByTaskIdAndToolName(taskId,
//                    toolName);
//            if (CollectionUtils.isNotEmpty(needClearTempFileBuildEntities))
//            {
//                List<BuildDefectEntity> needClearBuildDefectEntities = Lists.newArrayList();
//                for (NeedClearTempFileBuildEntity needClearTempFileBuildEntity : needClearTempFileBuildEntities)
//                {
//                    int buildNumInt = Integer.valueOf(needClearTempFileBuildEntity.getBuildNum());
//                    if (buildNumInt < baseNumInt)
//                    {
//                        List<BuildDefectEntity> buildDefectEntities = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName,
//                                needClearTempFileBuildEntity.getBuildId());
//                        if (CollectionUtils.isNotEmpty(buildDefectEntities))
//                        {
//                            for (BuildDefectEntity needClearBuildDefectEntity : buildDefectEntities)
//                            {
//                                if (needClearBuildDefectEntity.getTempCcnDefect() != null
//                                        || needClearBuildDefectEntity.getTempDefectFile() != null
//                                        || needClearBuildDefectEntity.getTempDupcDefectFile() != null)
//                                {
//                                    needClearBuildDefectEntity.setTempDefectFile(null);
//                                    needClearBuildDefectEntity.setTempCcnDefect(null);
//                                    needClearBuildDefectEntity.setTempDupcDefectFile(null);
//                                    needClearBuildDefectEntities.add(needClearBuildDefectEntity);
//                                }
//                            }
//                        }
//                    }
//                }
//                if (CollectionUtils.isNotEmpty(needClearBuildDefectEntities))
//                {
//                    buildDefectRepository.save(needClearBuildDefectEntities);
//                }
//            }
//        }
    }

    /**
     * 基于基准快照同步增量扫描时无变更的文件的告警到当前快照
     * @param taskId
     * @param toolName
     * @param baseBuildId
     * @param buildId
     * @param isFullScan
     * @param deleteFiles
     * @param currentBuildRelPaths
     */
    @Override
    public void updateBaseBuildDefects(
            long taskId,
            String toolName,
            String baseBuildId,
            String buildId,
            boolean isFullScan,
            List<String> deleteFiles,
            Set<String> currentBuildRelPaths)
    {
        if (!isFullScan && StringUtils.isNotEmpty(baseBuildId))
        {
            List<BuildDefectEntity> baseDefectEntities = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, baseBuildId);
            if (CollectionUtils.isNotEmpty(baseDefectEntities))
            {
                Iterator<BuildDefectEntity> buildDefectEntityIterator = baseDefectEntities.iterator();
                while (buildDefectEntityIterator.hasNext())
                {
                    BuildDefectEntity baseDefectEntity = buildDefectEntityIterator.next();

                    String filePath = baseDefectEntity.getFilePath();
                    String relPath = baseDefectEntity.getFileRelPath();
                    // 不需要把本次构建已删除的文件的基准快照告警同步到本次构建快照
                    if (CollectionUtils.isNotEmpty(deleteFiles) && deleteFiles.contains(filePath))
                    {
                        buildDefectEntityIterator.remove();
                        continue;
                    }

                    // 不需要把本次增量告警所在的文件的基准快照告警同步到本次构建快照
                    if (CollectionUtils.isNotEmpty(currentBuildRelPaths) && currentBuildRelPaths.contains(StringUtils.isEmpty(relPath) ? filePath : relPath))
                    {
                        buildDefectEntityIterator.remove();
                        continue;
                    }
                    baseDefectEntity.setBuildId(buildId);
                }
                if (CollectionUtils.isNotEmpty(baseDefectEntities))
                {
                    if (ComConstants.ToolPattern.CCN.name().equals(toolMetaCacheService.getToolPattern(toolName)))
                    {
                        buildDefectDao.upsertByDefectId(baseDefectEntities);
                    }
                    else
                    {
                        buildDefectDao.upsertByFilePath(baseDefectEntities);
                    }
                }
            }
        }

        // 更新下次扫描告警快照基准构建号
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
    }

    @Override
    public void addBuildDefectsAndTemp(long taskId, String toolName, String buildId, LintFileEntity tempDefectFile, CCNDefectEntity tempCcnDefect,
            DUPCDefectEntity tempDupcDefectFile, String relPath, String filePath)
    {
        // 更新遗留告警文件与构建ID快照
        BuildEntity buildEntity = buildDao.getAndSaveBuildInfo(buildId);
        BuildDefectEntity buildDefectEntity = new BuildDefectEntity();
        buildDefectEntity.setTaskId(taskId);
        buildDefectEntity.setToolName(toolName);
        buildDefectEntity.setBuildId(buildId);
        buildDefectEntity.setBuildNum(buildEntity.getBuildNo());
        buildDefectEntity.setFileRelPath(relPath);
        buildDefectEntity.setFilePath(filePath);
        if (ComConstants.ToolPattern.LINT.name().equals(toolMetaCacheService.getToolPattern(toolName)))
        {
            buildDefectEntity.setTempDefectFile(tempDefectFile);
            buildDefectDao.upsertByFilePath(Lists.newArrayList(buildDefectEntity));
        }
        else if (ComConstants.ToolPattern.CCN.name().equals(toolMetaCacheService.getToolPattern(toolName)))
        {
            buildDefectEntity.setTempCcnDefect(tempCcnDefect);
            buildDefectRepository.save(buildDefectEntity);
        }
        else if (ComConstants.ToolPattern.DUPC.name().equals(toolMetaCacheService.getToolPattern(toolName)))
        {
            buildDefectEntity.setTempDupcDefectFile(tempDupcDefectFile);
            buildDefectRepository.save(buildDefectEntity);
        }

        // 更新需要清除临时文件的构建号列表
        NeedClearTempFileBuildEntity needClearTempFileBuildEntity =  needClearTempFileBuildRepository.findByToolNameAndBuildId(toolName,
                buildId);
        if (needClearTempFileBuildEntity == null)
        {
            needClearTempFileBuildEntity = new NeedClearTempFileBuildEntity();
            needClearTempFileBuildEntity.setTaskId(taskId);
            needClearTempFileBuildEntity.setToolName(toolName);
            needClearTempFileBuildEntity.setBuildId(buildId);
            needClearTempFileBuildEntity.setBuildNum(buildEntity.getBuildNo());
            needClearTempFileBuildRepository.save(needClearTempFileBuildEntity);
        }
    }
}
