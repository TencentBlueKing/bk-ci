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

package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 告警提交消息队列的消费者抽象类
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class SuperLargeDefectCommitConsumer extends AbstractDefectCommitConsumer
{
    /**
     * 告警提交
     *
     * @param commitDefectVO
     */
    @Override
    public void commitDefect(CommitDefectVO commitDefectVO)
    {
        try
        {
            log.info("commit defect! {}", commitDefectVO);

            // 发送开始提单的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            // 发送提单成功的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), "告警文件大小超过1G，无法入库");
        }
        catch (Throwable e)
        {
            log.error("commit defect fail!", e);
        }
    }

    @Override
    protected void uploadDefects(CommitDefectVO commitDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap, Map<String, RepoSubModuleVO> codeRepoIdMap)
    {

    }
}
