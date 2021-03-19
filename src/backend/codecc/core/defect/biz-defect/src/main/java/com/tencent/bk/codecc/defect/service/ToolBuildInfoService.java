package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.SetForceFullScanReqVO;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;

import java.util.List;

/**
 * 工具构建信息服务
 *
 * @version V1.0
 * @date 2019/11/17
 */
public interface ToolBuildInfoService
{
    /**
     * 查询工具构建信息
     * @param analyzeConfigInfoVO
     * @return
     */
    AnalyzeConfigInfoVO getBuildInfo(AnalyzeConfigInfoVO analyzeConfigInfoVO);

    Boolean setToolBuildStackFullScan(Long taskId, SetForceFullScanReqVO setForceFullScanReqVO);

    /**
     * 更新强制全量扫描标志位
     */
    Boolean setForceFullScan(Long taskId, List<String> toolNames);
}
