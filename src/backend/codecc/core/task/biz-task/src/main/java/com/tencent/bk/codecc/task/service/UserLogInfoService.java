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

import com.tencent.bk.codecc.task.model.UserLogInfoEntity;
import com.tencent.bk.codecc.task.model.UserLogInfoStatEntity;


/**
 * 用户日志统计服务接口
 */
public interface UserLogInfoService {

    /**
     * 更新用户日志统计
     *
     * @param statEntity statEntity
     */
    public void findAndUpdateLogInfo(UserLogInfoStatEntity statEntity);


    /**
     * 仅用于刷一次存量用户统计数据
     */
    public Boolean intiUserLogInfoStatScript();

}
