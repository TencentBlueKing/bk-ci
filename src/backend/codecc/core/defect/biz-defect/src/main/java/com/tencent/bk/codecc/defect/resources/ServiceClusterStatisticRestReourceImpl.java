package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceClusterStatisticRestReource;
import com.tencent.bk.codecc.defect.service.impl.ClusterDefectServiceImpl;
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class ServiceClusterStatisticRestReourceImpl implements ServiceClusterStatisticRestReource {

    @Autowired
    ClusterDefectServiceImpl clusterDefectService;

    @Override
    public Result<List<BaseClusterResultVO>> getClusterStatistic(long taskId, String buildId) {
        return new Result<>(clusterDefectService.getClusterStatistic(taskId, buildId));
    }
}
