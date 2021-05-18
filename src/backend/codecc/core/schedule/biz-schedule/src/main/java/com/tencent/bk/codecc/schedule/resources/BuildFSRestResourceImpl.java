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

import com.tencent.bk.codecc.schedule.api.BuildFSRestResource;
import com.tencent.bk.codecc.schedule.vo.*;
import com.tencent.bk.codecc.schedule.service.UploadDownloadService;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * 文件上传下载接口
 *
 * @version V1.0
 * @date 2019/10/9
 */
@RestResource
public class BuildFSRestResourceImpl implements BuildFSRestResource
{
    @Autowired
    private UploadDownloadService uploadDownloadService;

    @Override
    public CodeCCResult<Boolean> upload(String fileName, Integer chunks, Integer chunk, String uploadType, InputStream file)
    {
        UploadVO uploadVO = new UploadVO();
        uploadVO.setFileName(fileName);
        uploadVO.setChunks(chunks);
        uploadVO.setChunk(chunk);
        uploadVO.setUploadType(uploadType);
        return new CodeCCResult<>(uploadDownloadService.upload(uploadVO, file));
    }

    @Override
    public CodeCCResult<Boolean> chunksMerge(FileChunksMergeVO fileChunksMergeVO)
    {
        return new CodeCCResult<>(uploadDownloadService.chunksMerge(fileChunksMergeVO));
    }

    @Override
    public CodeCCResult<Long> getFileSize(GetFileSizeVO getFileSizeVO)
    {
        return new CodeCCResult<>(uploadDownloadService.getFileSize(getFileSizeVO));
    }

    @Override
    public Response download(DownloadVO downloadVO)
    {
        return uploadDownloadService.download(downloadVO);
    }

    @Override
    public CodeCCResult<FileInfoModel> getFileInfo(GetFileSizeVO getFileSizeVO)
    {
        return new CodeCCResult<>(uploadDownloadService.getFileInfo(getFileSizeVO));
    }

    @Override
    public CodeCCResult<FileIndexVO> index(String fileName, String type)
    {
        return new CodeCCResult<>(uploadDownloadService.index(fileName, type));
    }

    @Override
    public CodeCCResult<FileIndexVO> getFileIndex(String fileName, String type)
    {
        return new CodeCCResult<>(uploadDownloadService.getFileIndex(fileName, type));
    }
}
