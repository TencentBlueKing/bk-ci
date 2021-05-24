package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceOverviewRestResource;
import com.tencent.bk.codecc.defect.service.TaskPersonalStatisticService;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class ServiceOverviewRestResourceImpl implements ServiceOverviewRestResource {
    @Autowired
    private TaskPersonalStatisticService taskPersonalStatisticService;

    @Override
    public Result<Boolean> refresh(Long taskId, String extraInfo) {
        taskPersonalStatisticService.refresh(taskId, extraInfo);
        return new Result<>(true);
    }
}
