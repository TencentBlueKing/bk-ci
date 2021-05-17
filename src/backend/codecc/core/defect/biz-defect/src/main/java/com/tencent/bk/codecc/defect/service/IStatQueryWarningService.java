package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.StatDefectQueryRespVO;

import java.util.List;

/**
 * 统计累工具查询信息入口
 */
public interface IStatQueryWarningService {
    List<StatDefectQueryRespVO> processQueryWarningRequest(long taskId, String toolName, long startTime, long endTime);
}
