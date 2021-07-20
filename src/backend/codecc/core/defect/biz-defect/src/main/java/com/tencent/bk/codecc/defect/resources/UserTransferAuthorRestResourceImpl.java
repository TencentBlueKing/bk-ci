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
 
package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserTransferAuthorRestResource;
import com.tencent.bk.codecc.defect.service.IQueryTransferAuthorBizService;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 告警处理人转换服务实现
 * 
 * @date 2019/12/3
 * @version V1.0
 */
@RestResource
@AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
public class UserTransferAuthorRestResourceImpl implements UserTransferAuthorRestResource
{
    @Autowired
    private IQueryTransferAuthorBizService queryTransferAuthorBizService;

    @Override
    public Result<AuthorTransferVO> getAuthorTransfer(long taskId)
    {
        return new Result<>(queryTransferAuthorBizService.getAuthorTransfer(taskId));
    }
}
