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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;

import static com.tencent.devops.common.constant.ComConstants.AUTHOR_TRANSFER;
import static com.tencent.devops.common.constant.ComConstants.FUNC_DEFECT_MANAGE;

/**
 * 告警处理人转换业务接口
 * 
 * @date 2019/12/3
 * @version V1.0
 */
public interface IAuthorTransferBizService 
{
    @OperationHistory(funcId = FUNC_DEFECT_MANAGE, operType = AUTHOR_TRANSFER)
    Boolean authorTransfer(AuthorTransferVO authorTransferVO);
}
