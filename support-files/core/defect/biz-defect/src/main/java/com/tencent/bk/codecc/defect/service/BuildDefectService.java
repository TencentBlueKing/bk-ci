package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.*;

import java.util.List;

/**
 * 构建与告警快照服务接口
 *
 * @version V1.0
 * @date 2019/12/17
 */
public interface BuildDefectService
{
    /**
     * 保存Lint类工具的构建告警快照
     *  @param taskId
     * @param toolName
     * @param buildEntity
     * @param allNewDefectList
     */
    void saveLintBuildDefect(long taskId, String toolName, BuildEntity buildEntity, List<LintDefectV2Entity> allNewDefectList);

    /**
     * 保存圈复杂度的构建告警快照
     * @param taskId
     * @param toolName
     * @param buildEntity
     * @param allNewDefectList
     */
    void saveCCNBuildDefect(long taskId, String toolName, BuildEntity buildEntity, List<CCNDefectEntity> allNewDefectList);

    /**
     * 保存圈复杂度的构建告警快照
     * @param taskId
     * @param toolName
     * @param buildEntity
     * @param allNewDefectList
     */
    void saveCommonBuildDefect(long taskId, String toolName, BuildEntity buildEntity, List<DefectEntity> allNewDefectList);
}
