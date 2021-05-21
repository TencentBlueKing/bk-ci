package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserOverviewRestResource;
import com.tencent.bk.codecc.defect.service.TaskPersonalStatisticService;
import com.tencent.bk.codecc.defect.vo.TaskPersonalStatisticwVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestResource
public class UserOverviewRestResourceImpl implements UserOverviewRestResource {

    @Autowired
    private TaskPersonalStatisticService taskPersonalStatisticService;

    @Override
    public Result<TaskPersonalStatisticwVO> overview(Long taskId, String username) {
        return new Result<>(taskPersonalStatisticService.getPersonalStatistic(taskId, username));
    }

    @Override
    public Result<List<TaskPersonalStatisticwVO>> overviewList(Long taskId) {
        return new Result<>(taskPersonalStatisticService.getPersonalStatistic(taskId));
    }
}
