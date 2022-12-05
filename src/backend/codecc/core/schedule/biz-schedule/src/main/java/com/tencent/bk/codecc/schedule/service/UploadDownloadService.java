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

package com.tencent.bk.codecc.schedule.service;

import com.tencent.bk.codecc.schedule.vo.*;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * 文件上传业务逻辑服务接口
 *
 * @version V1.0
 * @date 2019/10/10
 */
public interface UploadDownloadService
{
    Boolean upload(UploadVO uploadVO, InputStream file);

    Boolean chunksMerge(FileChunksMergeVO fileChunksMergeVO);

    Long getFileSize(GetFileSizeVO getFileSizeVO);

    Response download(DownloadVO downloadVO);

    FileInfoModel getFileInfo(GetFileSizeVO getFileSizeVO);

    FileIndexVO index(String fileName, String type);

    FileIndexVO getFileIndex(String fileName, String type);

    Response download(String downloadType, String fileName);

    FileIndexVO updateFileIndex(FileIndexVO fileInfoVo);
}
