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

package com.tencent.bk.codecc.apiquery.service.impl;

import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CodeRepoFromAnalyzeLogDao;
import com.tencent.bk.codecc.apiquery.service.CodeRepoFromAnalyzeLogService;
import com.tencent.bk.codecc.apiquery.task.model.CodeRepoFromAnalyzeLogModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CodeRepoFromAnalyzeLogService服务接口
 *
 * @version V2.0
 * @date 2020/5/12
 */
@Slf4j
@Service
public class CodeRepoFromAnalyzeLogServiceImpl implements CodeRepoFromAnalyzeLogService {
    @Autowired
    private CodeRepoFromAnalyzeLogDao codeRepoFromAnalyzeLogDao;

    /**
     * 根据taskId获取代码仓库地址
     *
     * @param taskIds
     * @return list
     */
    @Override
    public List<CodeRepoFromAnalyzeLogModel> getCodeRepoListByTaskIds(List<Long> taskIds) {
        return codeRepoFromAnalyzeLogDao.getCodeRepoListByTaskIds(taskIds);
    }
}