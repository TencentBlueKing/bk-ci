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

import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintFileQueryRepository;
import com.tencent.bk.codecc.codeccjob.service.AbstractAuthorTransBizService;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lint类工具的作者转换
 *
 * @version V1.0
 * @date 2019/11/1
 */
@Service("LINTAuthorTransBizService")
@Slf4j
public class LintAuthorTransBizServiceImpl extends AbstractAuthorTransBizService
{
    @Autowired
    private LintFileQueryRepository lintFileQueryRepository;

    @Override
    public CodeCCResult processBiz(AuthorTransferVO authorTransferVO)
    {
        List<LintFileEntity> lintFileEntityList = lintFileQueryRepository.findByTaskIdAndToolNameAndStatus(authorTransferVO.getTaskId(),
                authorTransferVO.getToolName(), ComConstants.TaskFileStatus.NEW.value());

        if (CollectionUtils.isNotEmpty(lintFileEntityList))
        {
            List<LintFileEntity> needRefreshFileList = new ArrayList<>();
            lintFileEntityList.forEach(
                    lintFileEntity ->
                    {
                        boolean needRefresh = refreshDefectAuthor(authorTransferVO,
                                lintFileEntity);
                        if (needRefresh)
                        {
                            needRefreshFileList.add(lintFileEntity);
                        }
                    }
            );
            lintFileQueryRepository.save(lintFileEntityList);
        }
        return new CodeCCResult(CommonMessageCode.SUCCESS);
    }


    private boolean refreshDefectAuthor(AuthorTransferVO authorTransferVO, LintFileEntity lintFileEntity)
    {
        boolean needRefresh = false;
        //1.设置lint文件中的作者清单
        Set<String> authorList = lintFileEntity.getAuthorList();
        lintFileEntity.setAuthorList(authorList.stream()
                .map(author -> transferAuthor(authorTransferVO.getTransferAuthorList(), author))
                .collect(Collectors.toSet()));
        //2.设置告警清单中的作者
        List<LintDefectEntity> lintDefectEntityList = lintFileEntity.getDefectList();
        for(LintDefectEntity lintDefectEntity: lintDefectEntityList)
        {
            String author = lintDefectEntity.getAuthor();
            if (StringUtils.isNotEmpty(author))
            {
                String newAuthor = transferAuthor(authorTransferVO.getTransferAuthorList(), author);
                if (!newAuthor.equals(author))
                {
                    lintDefectEntity.setAuthor(newAuthor);
                    needRefresh = true;
                }
            }
        }
        return needRefresh;
    }

}
