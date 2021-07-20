package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceScmFileCacheRestResource;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@RestResource
public class ServiceScmFileCacheRestResourceImpl implements ServiceScmFileCacheRestResource {

    @Autowired
    private ScmFileInfoService scmFileInfoService;

    @Override
    public Result<Map<String, ScmBlameVO>> loadAuthorInfoMap(long taskId, String streamName, String toolName, String buildId) {
        return new Result<>(scmFileInfoService.loadAuthorInfoMap(taskId, streamName, toolName, buildId));
    }
}
