package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;

import java.util.List;
import java.util.Set;

/**
 * 构建与告警快照服务接口
 *
 * @version V1.0
 * @date 2019/12/17
 */
public interface BuildDefectService
{
    /**
     * 更新基准构建ID，并清除临时告警和文件
     *
     * @param taskId
     * @param toolName
     * @param baseBuildId
     * @param buildId
     * @param isFullScan
     * @param deleteFiles
     * @param currentBuildRelPaths
     */
    void updateBaseBuildDefectsAndClearTemp(long taskId, String toolName, String baseBuildId, String buildId, boolean isFullScan,
            List<String> deleteFiles, Set<String> currentBuildRelPaths);

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
    void updateBaseBuildDefects(long taskId, String toolName, String baseBuildId, String buildId, boolean isFullScan,
                                List<String> deleteFiles, Set<String> currentBuildRelPaths);

    /**
     * 新增构建与告警快照以及临时告警和文件
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param tempDefectFile
     * @param tempCcnDefect
     * @param tempDupcDefectFile
     * @param relPath
     * @param filePath
     */
    void addBuildDefectsAndTemp(long taskId, String toolName, String buildId, LintFileEntity tempDefectFile, CCNDefectEntity tempCcnDefect,
            DUPCDefectEntity tempDupcDefectFile, String relPath, String filePath);
}
