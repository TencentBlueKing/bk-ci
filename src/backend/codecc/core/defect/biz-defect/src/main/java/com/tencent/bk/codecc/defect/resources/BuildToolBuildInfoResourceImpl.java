package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildToolBuildInfoResource;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.vo.SetForceFullScanReqVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 工具构建信息实现类
 *
 * @version V1.0
 * @date 2020/3/10
 */
@Slf4j
@RestResource
public class BuildToolBuildInfoResourceImpl implements BuildToolBuildInfoResource
{
    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    @Override
    public Result<Boolean> setToolBuildStackFullScan(Long taskId, SetForceFullScanReqVO setForceFullScanReqVO)
    {
        return new Result<>(toolBuildInfoService.setToolBuildStackFullScan(taskId, setForceFullScanReqVO));
    }
}
