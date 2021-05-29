package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildTaskLogOverviewResource;
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.devops.common.api.constant.CommonMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class BuildTaskLogOverviewResourceImpl implements BuildTaskLogOverviewResource {

    @Autowired
    TaskLogOverviewService taskLogOverviewService;

    @Override public Result<Boolean> saveActualTools(TaskLogOverviewVO taskLogOverviewVO) {
        if (taskLogOverviewVO.getTaskId() == null || taskLogOverviewVO.getTools() == null) {
            throw new CodeCCException(CommonMessageCode.ERROR_INVALID_PARAM_, new String[] {"非法空参数"});
        }
        return new Result<>(taskLogOverviewService.saveActualExeTools(taskLogOverviewVO));
    }
}
