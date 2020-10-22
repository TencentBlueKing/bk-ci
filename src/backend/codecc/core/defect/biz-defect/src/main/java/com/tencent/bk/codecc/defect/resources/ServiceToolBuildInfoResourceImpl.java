package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.bk.codecc.defect.vo.SetForceFullScanReqVO;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 工具构建信息实现类
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Slf4j
@RestResource
public class ServiceToolBuildInfoResourceImpl implements ServiceToolBuildInfoResource
{
    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    @Override
    public CodeCCResult<Boolean> setForceFullScan(Long taskId, List<String> toolNames)
    {
        return new CodeCCResult<>(toolBuildInfoService.setForceFullScan(taskId, toolNames));
    }

    @Override
    public CodeCCResult<Boolean> setToolBuildStackFullScan(Long taskId, SetForceFullScanReqVO setForceFullScanReqVO)
    {
        return new CodeCCResult<>(toolBuildInfoService.setToolBuildStackFullScan(taskId, setForceFullScanReqVO));
    }
}
