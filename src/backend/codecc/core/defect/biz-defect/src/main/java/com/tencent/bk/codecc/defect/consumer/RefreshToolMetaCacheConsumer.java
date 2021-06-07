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

import com.tencent.devops.common.service.ToolMetaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 刷新规则集使用量消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class RefreshToolMetaCacheConsumer
{
    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    public void refreshToolMetaCache(String toolName)
    {
        log.info("begin refreshToolMetaCache: {}", toolName);

        try
        {
            toolMetaCacheService.loadToolBaseCache();
        }
        catch (Exception e)
        {
            log.error("refreshToolMetaCache fail.", e);
        }
        log.info("end refreshToolMetaCache: {}", toolName);
    }
}
