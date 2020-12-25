package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
    private BuildDefectDao buildDefectDao;

    @Override
    public void saveLintBuildDefect(long taskId, String toolName, BuildEntity buildEntity, List<LintDefectV2Entity> allNewDefectList)
    {
        // 告警按文件分组
        Map<String, List<LintDefectV2Entity>> defectGroupByPathMap = allNewDefectList.stream()
                .filter(defect -> defect.getStatus() == ComConstants.DefectStatus.NEW.value())
                .collect(Collectors.groupingBy(defect -> StringUtils.isEmpty(defect.getRelPath()) ? defect.getFilePath() : defect.getRelPath()));
        List<BuildDefectEntity> buildDefectEntityList = new ArrayList<>();
        defectGroupByPathMap.forEach((file, defectList) ->
        {
            Set<String> fileDefectIds = defectList.stream().map(LintDefectV2Entity::getEntityId).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(fileDefectIds))
            {
                BuildDefectEntity buildDefectEntity = new BuildDefectEntity();
                buildDefectEntity.setTaskId(taskId);
                buildDefectEntity.setToolName(toolName);
                buildDefectEntity.setBuildId(buildEntity.getBuildId());
                buildDefectEntity.setBuildNum(buildEntity.getBuildNo());
                buildDefectEntity.setFilePath(defectList.get(0).getRelPath());
                buildDefectEntity.setFileDefectIds(fileDefectIds);
                buildDefectEntityList.add(buildDefectEntity);
            }
        });

        buildDefectDao.upsertByFilePath(buildDefectEntityList);

        // 更新下次扫描告警快照基准构建号
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildEntity.getBuildId());
    }

    @Override
    public void saveCCNBuildDefect(long taskId, String toolName, BuildEntity buildEntity, List<CCNDefectEntity> allNewDefectList)
    {
        List<BuildDefectEntity> buildDefectEntityList = allNewDefectList.stream()
                .filter(defect -> defect.getStatus() == ComConstants.DefectStatus.NEW.value())
                .map(ccnDefectEntity ->
                {
                    BuildDefectEntity buildDefectEntity = new BuildDefectEntity();
                    buildDefectEntity.setTaskId(taskId);
                    buildDefectEntity.setToolName(toolName);
                    buildDefectEntity.setBuildId(buildEntity.getBuildId());
                    buildDefectEntity.setBuildNum(buildEntity.getBuildNo());
                    buildDefectEntity.setDefectId(ccnDefectEntity.getEntityId());
                    return buildDefectEntity;
                }).collect(Collectors.toList());

        buildDefectDao.upsertByDefectId(buildDefectEntityList);

        // 更新下次扫描告警快照基准构建号
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildEntity.getBuildId());
    }

    @Override
    public void saveCommonBuildDefect(long taskId, String toolName, BuildEntity buildEntity, List<DefectEntity> allNewDefectList)
    {
        List<BuildDefectEntity> buildDefectEntityList = allNewDefectList.stream()
                .filter(defect -> defect.getStatus() == ComConstants.DefectStatus.NEW.value())
                .map(defectEntity ->
                {
                    BuildDefectEntity buildDefectEntity = new BuildDefectEntity();
                    buildDefectEntity.setTaskId(taskId);
                    buildDefectEntity.setToolName(toolName);
                    buildDefectEntity.setBuildId(buildEntity.getBuildId());
                    buildDefectEntity.setBuildNum(buildEntity.getBuildNo());
                    buildDefectEntity.setDefectId(defectEntity.getId());
                    return buildDefectEntity;
                }).collect(Collectors.toList());

        buildDefectDao.upsertByDefectId(buildDefectEntityList);

        // 更新下次扫描告警快照基准构建号
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildEntity.getBuildId());
    }
}
