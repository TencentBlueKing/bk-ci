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

package com.tencent.bk.codecc.schedule.resources;

import com.tencent.bk.codecc.schedule.api.UserFSRestResource;
import com.tencent.bk.codecc.schedule.service.UploadDownloadService;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;

/**
 * 文件上传下载接口
 *
 * @version V1.0
 * @date 2019/10/9
 */
@RestResource
public class UserFSRestResourceImpl implements UserFSRestResource
{
    @Autowired
    private UploadDownloadService uploadDownloadService;

    @Override
    public Response download(String downloadType, String fileName)
    {
        return uploadDownloadService.download(downloadType, fileName);
    }
}
