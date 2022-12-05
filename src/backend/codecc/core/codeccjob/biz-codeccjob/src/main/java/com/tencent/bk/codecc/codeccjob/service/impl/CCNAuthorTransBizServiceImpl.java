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

import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.AbstractAuthorTransBizService;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ccn类工具的作者转换
 *
 * @version V1.0
 * @date 2019/11/1
 */
@Service("CCNAuthorTransBizService")
@Slf4j
public class CCNAuthorTransBizServiceImpl extends AbstractAuthorTransBizService
{
    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Override
    public Result processBiz(AuthorTransferVO authorTransferVO)
    {
        List<CCNDefectEntity> ccnDefectEntityList = ccnDefectRepository.findNotRepairedDefect(authorTransferVO.getTaskId(),
                ComConstants.DEFECT_STATUS_CLOSED);

        if (CollectionUtils.isNotEmpty(ccnDefectEntityList))
        {
            List<CCNDefectEntity> needRefreshDefectList = new ArrayList<>();
            ccnDefectEntityList.forEach(ccnDefectEntity ->
                    {
                        String author = ccnDefectEntity.getAuthor();
                        String newAuthor = transferAuthor(authorTransferVO.getTransferAuthorList(), author);
                        if (!newAuthor.equals(author))
                        {
                            ccnDefectEntity.setAuthor(newAuthor);
                            needRefreshDefectList.add(ccnDefectEntity);
                        }
                    }
            );

            if (CollectionUtils.isNotEmpty(needRefreshDefectList))
            {
                ccnDefectRepository.saveAll(needRefreshDefectList);
            }
        }

        return new Result(CommonMessageCode.SUCCESS);
    }
}
