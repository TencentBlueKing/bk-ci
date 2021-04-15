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

package com.tencent.bk.codecc.apiquery.utils;

import com.tencent.bk.codecc.schedule.api.ServiceFSRestResource;
import com.tencent.bk.codecc.schedule.vo.FileIndexVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 外部服务公共调度器
 *
 * @version V1.0
 * @date 2021/3/29
 */

@Slf4j
@Component
public class ThirdPartySystemCaller {

    @Autowired
    private Client client;


    /**
     * 获取文件索引
     *
     * @param fileName 文件名
     * @param type     文件类型
     * @return fileNamePath
     */
    public String getFileIndex(String fileName, String type) {
        Result<FileIndexVO> result = client.get(ServiceFSRestResource.class).index(fileName, type);
        if (result.isNotOk() || null == result.getData()) {
            log.error("apiquery get file {} index fail: {}", fileName, result);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{fileName}, null);
        }
        FileIndexVO fileIndex = result.getData();
        return String.format("%s/%s", fileIndex.getFileFolder(), fileIndex.getFileName());
    }

}
