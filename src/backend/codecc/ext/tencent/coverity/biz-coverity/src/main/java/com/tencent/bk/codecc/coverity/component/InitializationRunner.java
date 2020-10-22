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

package com.tencent.bk.codecc.coverity.component;

import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * coverity初始化
 *
 * @version V1.0
 * @date 2019/10/2
 */
@Component
@Slf4j
public class InitializationRunner implements CommandLineRunner
{
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Override
    public void run(String... args)
    {
        List<PlatformVO> covPlatformList = thirdPartySystemCaller.getAllPlatform(ComConstants.Tool.COVERITY.name());
        CoverityService.initAllPlatform(covPlatformList);
        log.info("init finished! Coverity platform client count:{}", covPlatformList.size());
    }
}
