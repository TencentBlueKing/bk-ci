package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceDefectDataResource;
import com.tencent.bk.codecc.defect.service.RefreshDefectBizService;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

@RestResource
public class ServiceDefectDataResourceImpl implements ServiceDefectDataResource {
    @Autowired
    private RefreshDefectBizService refreshDefectBizService;

    @Override
    public Result<String> freshToolStatic(Set<Long> taskIds) {
        return new Result<>(refreshDefectBizService.freshClocDefectByPage(taskIds));
    }
}
