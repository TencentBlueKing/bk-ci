package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceTaskLogOverviewResource;
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.admin.TaskLogOverviewReqVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class ServiceTaskLogOverviewResourceImpl implements ServiceTaskLogOverviewResource {

    @Autowired
    TaskLogOverviewService taskLogOverviewService;

    @Override
    public Result<TaskLogOverviewVO> getTaskLogOverview(Long taskId, String buildId, Integer status) {
        return new Result<>(taskLogOverviewService.getTaskLogOverview(taskId, buildId, status));
    }

    @Override
    public Result<TaskLogOverviewVO> getAnalyzeResult(Long taskId, String buildId, String buildNum, Integer status) {
        return new Result<>(taskLogOverviewService.getAnalyzeResult(taskId, buildId, buildNum, status));
    }

    @Override
    public Result<Integer> getTaskAnalyzeCount(@NotNull QueryTaskListReqVO reqVO) {
        return new Result<>(taskLogOverviewService
                .statTaskAnalyzeCount(reqVO.getTaskIds(), reqVO.getStatus(), reqVO.getStartTime(), reqVO.getEndTime()));
    }

}
