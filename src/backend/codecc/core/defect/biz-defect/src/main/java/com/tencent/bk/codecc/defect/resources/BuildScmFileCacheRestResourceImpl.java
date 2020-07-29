package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildScmFileCacheRestResource;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.vo.file.ScmFileMd5Info;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestResource
public class BuildScmFileCacheRestResourceImpl implements BuildScmFileCacheRestResource {

    @Autowired
    private ScmFileInfoService scmFileInfoService;

    @Override
    public CodeCCResult<List<ScmFileMd5Info>> listMd5FileInfos(long taskId, String toolName) {
        return new CodeCCResult<>(scmFileInfoService.listMd5FileInfos(taskId, toolName));
    }
}
