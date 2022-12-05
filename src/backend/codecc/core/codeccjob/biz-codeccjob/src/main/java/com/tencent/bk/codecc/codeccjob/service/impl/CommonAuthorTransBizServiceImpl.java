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
 
package com.tencent.bk.codecc.codeccjob.service.impl;

import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.DefectRepository;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.IBizService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.SetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Lint类工具的作者转换
 * 
 * @date 2019/11/1
 * @version V1.0
 */
@Service("CommonAuthorTransBizService")
@Slf4j
public class CommonAuthorTransBizServiceImpl implements IBizService<AuthorTransferVO>
{
    @Autowired
    private DefectRepository defectRepository;
    @Override
    public Result processBiz(AuthorTransferVO authorTransferVO)
    {
        long taskId = authorTransferVO.getTaskId();
        String toolName = authorTransferVO.getToolName();
        List<DefectEntity> defectList = defectRepository.findByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.DefectStatus.NEW.value());

        if (CollectionUtils.isNotEmpty(defectList))
        {
            Map<List<String>, List<String>> transferAuthorPairMap = new HashMap<>();
            List<AuthorTransferVO.TransferAuthorPair> transferAuthorList = authorTransferVO.getTransferAuthorList();
            transferAuthorList.forEach(transferAuthorPair ->
            {
                List<String> sourceAuthorList = Arrays.asList(transferAuthorPair.getSourceAuthor().split(ComConstants.SEMICOLON));
                List<String> targetAuthorList = Arrays.asList(transferAuthorPair.getTargetAuthor().split(ComConstants.SEMICOLON));
                transferAuthorPairMap.put(sourceAuthorList, targetAuthorList);
            });

            List<DefectEntity> needRefreshDefectList = new ArrayList<>();
            defectList.forEach(defectEntity ->
            {
                Set<String> authorList = defectEntity.getAuthorList();
                Set<String> newAuthorList = new HashSet<>(authorList);
                if (CollectionUtils.isNotEmpty(newAuthorList))
                {
                    Set<Map.Entry<List<String>, List<String>>> entrySet = transferAuthorPairMap.entrySet();
                    for (Map.Entry<List<String>, List<String>> entry : entrySet)
                    {
                        List<String> sourceAuthorList = entry.getKey();
                        List<String> targetAuthorList = entry.getValue();
                        if (newAuthorList.containsAll(sourceAuthorList))
                        {
                            newAuthorList.removeAll(sourceAuthorList);
                            newAuthorList.addAll(targetAuthorList);
                        }
                    }
                }
                if (!SetUtils.isEqualSet(newAuthorList, defectEntity.getAuthorList()))
                {
                    defectEntity.setAuthorList(newAuthorList);
                    needRefreshDefectList.add(defectEntity);
                }
            });

            if (CollectionUtils.isNotEmpty(needRefreshDefectList))
            {
                defectRepository.saveAll(needRefreshDefectList);
            }
        }

        return new Result(CommonMessageCode.SUCCESS);
    }
}
