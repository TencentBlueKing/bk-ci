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
import com.tencent.devops.common.api.pojo.Result;
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
    public Result<Boolean> upload(String fileName, Integer chunks, Integer chunk, String uploadType, String buildId,
            InputStream file) {
        UploadVO uploadVO = new UploadVO();
        uploadVO.setFileName(fileName);
        uploadVO.setChunks(chunks);
        uploadVO.setChunk(chunk);
        uploadVO.setUploadType(uploadType);
        uploadVO.setBuildId(buildId);
        return new Result<>(uploadDownloadService.upload(uploadVO, file));
    }

    @Override
    public Result<Boolean> chunksMerge(FileChunksMergeVO fileChunksMergeVO)
    {
        return new Result<>(uploadDownloadService.chunksMerge(fileChunksMergeVO));
    }

    @Override
    public Result<Long> getFileSize(GetFileSizeVO getFileSizeVO)
    {
        return new Result<>(uploadDownloadService.getFileSize(getFileSizeVO));
    }

    @Override
    public Response download(DownloadVO downloadVO)
    {
        return uploadDownloadService.download(downloadVO);
    }

    @Override
    public Result<FileInfoModel> getFileInfo(GetFileSizeVO getFileSizeVO)
    {
        return new Result<>(uploadDownloadService.getFileInfo(getFileSizeVO));
    }

    @Override
    public Result<FileIndexVO> index(String fileName, String type)
    {
        return new Result<>(uploadDownloadService.index(fileName, type));
    }

    @Override
    public Result<FileIndexVO> getFileIndex(String fileName, String type)
    {
        return new Result<>(uploadDownloadService.getFileIndex(fileName, type));
    }
}
