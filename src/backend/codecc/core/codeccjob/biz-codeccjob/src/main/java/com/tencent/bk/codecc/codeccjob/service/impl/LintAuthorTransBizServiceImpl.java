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

import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.codeccjob.service.AbstractAuthorTransBizService;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;

    @Override
    public Result processBiz(AuthorTransferVO authorTransferVO)
    {
        Set<String> sourceAuthorSet = authorTransferVO.getTransferAuthorList().stream().map(AuthorTransferVO.TransferAuthorPair::getSourceAuthor).collect(Collectors.toSet());
        List<LintDefectV2Entity> lintDefectEntityList = lintDefectV2Repository.findDefectsNeedTransferAuthor(authorTransferVO.getTaskId(),
                authorTransferVO.getToolName(), ComConstants.TaskFileStatus.NEW.value(), sourceAuthorSet);

        if (CollectionUtils.isNotEmpty(lintDefectEntityList))
        {
            lintDefectEntityList.forEach(defect ->
            {
                String newAuthor = transferAuthor(authorTransferVO.getTransferAuthorList(), defect.getAuthor());
                defect.setAuthor(newAuthor);
            });
            lintDefectV2Dao.batchUpdateDefectAuthor(authorTransferVO.getTaskId(), lintDefectEntityList);
        }
        return new Result(CommonMessageCode.SUCCESS);
    }

}
