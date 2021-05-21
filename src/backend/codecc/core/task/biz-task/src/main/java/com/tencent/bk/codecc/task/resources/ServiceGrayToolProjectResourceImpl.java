package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceGrayToolProjectResource;
import com.tencent.bk.codecc.task.service.GrayToolProjectService;
import com.tencent.bk.codecc.task.service.impl.GrayTaskRegisterServiceImpl;
import com.tencent.bk.codecc.task.vo.GrayTaskStatVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Set;

@RestResource
public class ServiceGrayToolProjectResourceImpl implements ServiceGrayToolProjectResource {
    @Autowired
    private GrayToolProjectService grayToolProjectService;

    @Qualifier("grayTaskRegisterServiceImpl")
    @Autowired
    private GrayTaskRegisterServiceImpl grayTaskRegisterService;

    @Override
    public Result<GrayToolProjectVO> getGrayToolProjectInfoByProjrctId(String projectId) {
        return new Result<>(grayToolProjectService.findGrayInfoByProjectId(projectId));
    }

    @Override
    public Result<List<GrayToolProjectVO>> getGrayToolProjectByProjectIds(Set<String> projectIdSet) {
        return new Result<>(grayToolProjectService.findGrayToolProjectByProjectIds(projectIdSet));
    }

    @Override
    public Result<Boolean> processGrayReport(Long taskId, String buildId, GrayTaskStatVO grayTaskStatVO) {
        grayTaskRegisterService.processGrayReport(buildId, taskId, grayTaskStatVO);
        return new Result<>(true);
    }
}
