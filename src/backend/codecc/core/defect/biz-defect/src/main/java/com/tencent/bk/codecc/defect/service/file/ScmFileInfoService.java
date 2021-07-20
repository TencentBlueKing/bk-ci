package com.tencent.bk.codecc.defect.service.file;

import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.defect.vo.file.ScmFileMd5Info;

import java.util.List;
import java.util.Map;

public interface ScmFileInfoService {

    List<ScmFileMd5Info> listMd5FileInfos(long taskId, String toolName, String buildId);

    void parseFileInfo(
        long taskId,
        String streamName,
        String toolName,
        String buildId);

    Map<String, ScmBlameVO> loadAuthorInfoMap(
        long taskId,
        String streamName,
        String toolName,
        String buildId);
}
