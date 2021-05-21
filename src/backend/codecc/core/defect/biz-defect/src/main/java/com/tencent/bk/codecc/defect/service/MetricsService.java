package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.MetricsVO;

public interface MetricsService {
    MetricsVO getMetrics(String repoId, String commitId);
    MetricsVO getMetrics(Long taskId, String buildId);
}
