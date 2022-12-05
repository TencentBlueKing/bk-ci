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

import com.tencent.bk.codecc.defect.dao.mongorepository.TransferAuthorRepository;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.service.IQueryTransferAuthorBizService;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import lombok.extern.slf4j.Slf4j;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 查询告警处理人转换关系表的实现类
 * 
 * @date 2019/12/3
 * @version V1.0
 */
@Service
@Slf4j
public class QueryTransferAuthorBizServiceImpl implements IQueryTransferAuthorBizService
{
    @Autowired
    private TransferAuthorRepository transferAuthorRepository;

    @Override
    public AuthorTransferVO getAuthorTransfer(long taskId)
    {
        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findFirstByTaskId(taskId);
        AuthorTransferVO authorTransferVO = new AuthorTransferVO();
        if (transferAuthorEntity != null)
        {
            BeanUtils.copyProperties(transferAuthorEntity, authorTransferVO);
        }
        else
        {
            authorTransferVO.setTaskId(taskId);
        }
        return authorTransferVO;
    }
}
