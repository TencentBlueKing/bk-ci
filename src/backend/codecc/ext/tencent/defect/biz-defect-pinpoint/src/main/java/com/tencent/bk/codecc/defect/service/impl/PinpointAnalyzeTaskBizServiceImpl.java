/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.CodeFileUrlEntity;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import com.tencent.devops.common.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * pinpoint工具记录上报接口实现
 *
 * @version V1.0
 * @date 2019/12/10
 */
@Slf4j
@Service("PINPOINTAnalyzeTaskBizService")
public class PinpointAnalyzeTaskBizServiceImpl extends CommonAnalyzeTaskBizServiceImpl
{
    @Autowired
    private ScmFileInfoService scmFileInfoService;

    @Override
    protected void postHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO)
    {
        log.info("begin postHandleDefectsAndStatistic...");
        // 代码扫描步骤结束，则开始变更告警的状态并统计本次分析的告警信息
        if (uploadTaskLogStepVO.getStepNum() == ComConstants.Step4MutliTool.SCAN.value()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value()
                && !uploadTaskLogStepVO.isFastIncrement())
        {
            log.info("begin commit defect.");
            asyncCommitDefect(uploadTaskLogStepVO, taskVO);
        }
        else if (uploadTaskLogStepVO.getStepNum() == getSubmitStepNum()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value()
                && !uploadTaskLogStepVO.isFastIncrement())
        {
            log.info("begin statistic defect count.");
            handleSubmitSuccess(uploadTaskLogStepVO, taskVO);
        }
    }

    @Override
    protected void updateCodeRepository(UploadTaskLogStepVO uploadTaskLogStepVO, TaskLogEntity taskLogEntity)
    {
        if (uploadTaskLogStepVO.getStepNum() == getSubmitStepNum()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value())
        {
            ThreadPoolUtil.addRunnableTask(() ->
            {
                /*
                    工具侧整理输出的文件路径格式跟工具无关，如下：
                    window：D:/workspace/svnauth_svr/app/utils/jgit_proxy/SSHProxySessionFactory.java
                    linux:  /data/landun/workspace/test/parallel/test-string-decoder-fuzz.js
                    后台存入t_code_file_url表中时，做了如下处理：
                    window下的路径将盘号去掉，变成
                    /workspace/svnauth_svr/app/utils/jgit_proxy/SSHProxySessionFactory.java
                    另外url转换成标准http格式
                 */
                saveCodeFileUrl(uploadTaskLogStepVO);

                // 保存代码仓信息
                saveCodeRepoInfo(uploadTaskLogStepVO);
            });

        }
    }

    /**
     * 保存代码文件的URL信息
     *
     * @param uploadTaskLogStepVO
     */
    private void saveCodeFileUrl(UploadTaskLogStepVO uploadTaskLogStepVO)
    {
        // 获取文件作者信息
        Map<String, ScmBlameVO> fileChangeRecordsMap = scmFileInfoService.loadAuthorInfoMap(
                uploadTaskLogStepVO.getTaskId(),
                uploadTaskLogStepVO.getStreamName(),
                uploadTaskLogStepVO.getToolName(),
                uploadTaskLogStepVO.getPipelineBuildId());

        long currTime = System.currentTimeMillis();
        List<CodeFileUrlEntity> codeFileUrlEntityList = fileChangeRecordsMap.values().stream()
                .filter(scmBlameVO -> StringUtils.isNotEmpty(scmBlameVO.getFilePath()) && StringUtils.isNotEmpty(scmBlameVO.getUrl()))
                .map(scmBlameVO ->
                {
                    CodeFileUrlEntity codeFileUrlEntity = new CodeFileUrlEntity();
                    String filePath = scmBlameVO.getFilePath();
                    if (StringUtils.isNotEmpty(filePath))
                    {
                        filePath = PathUtils.trimWinPathPrefix(filePath);
                        codeFileUrlEntity.setFile(filePath);
                    }
                    codeFileUrlEntity.setUrl(PathUtils.formatFileRepoUrlToHttp(scmBlameVO.getUrl() + scmBlameVO.getFileRelPath()));
                    codeFileUrlEntity.setTaskId(scmBlameVO.getTaskId());
                    codeFileUrlEntity.setVersion(scmBlameVO.getRevision());
                    codeFileUrlEntity.setFileRelPath(scmBlameVO.getFileRelPath());
                    codeFileUrlEntity.setUpdatedDate(currTime);
                    return codeFileUrlEntity;
                }).collect(Collectors.toList());

        codeFileUrlDao.upsert(uploadTaskLogStepVO.getTaskId(), codeFileUrlEntityList);
    }

    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    @Override
    public int getCodeDownloadStepNum()
    {
        return ComConstants.Step4MutliTool.DOWNLOAD.value();
    }
}
