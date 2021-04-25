/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.schedule.api;

import com.tencent.bk.codecc.schedule.vo.*;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * 文件上传下载接口
 *
 * @version V2.0
 * @date 2019/09/28
 */
@Api(tags = {"SERVICE_FS"}, description = "文件上传下载接口")
@Path("/build/cfs")
public interface BuildCFSRestResource
{
    @ApiOperation(value = "分片上传", notes = "客户端将文件分片后逐个上传")
    @Path("/upload")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    Result<Boolean> upload(
            @NotNull(message = "文件名不能为空")
            @ApiParam(value = "文件名", required = true)
            @FormDataParam("fileName")
                    String fileName,
            @ApiParam(value = "分片总数")
            @FormDataParam("chunks")
                    Integer chunks,
            @ApiParam(value = "当前分片")
            @FormDataParam("chunk")
                    Integer chunk,
            @NotNull(message = "上传类型不能为空")
            @ApiParam(value = "上传类型")
            @FormDataParam("uploadType")
                    String uploadType,
            @ApiParam(value = "构建id")
            @FormDataParam("buildId")
                    String buildId,
            @NotNull(message = "文件内容不能为空")
            @ApiParam(value = "文件", required = true)
            @FormDataParam("file")
                    InputStream file);

    /**
     * 分片通过nio合并， 合并成功后，将文件上传至CFS
     * nio合并优点： 有效防止大文件的内存溢出
     *
     * @param fileChunksMergeVO
     * @return
     */
    @ApiOperation(value = "分片合并", notes = "所有分片上传成功后，调用该接口对分片进行合并")
    @Path("/merge")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Result<Boolean> chunksMerge(
            @ApiParam(value = "分片合并的请求信息", required = true)
                    FileChunksMergeVO fileChunksMergeVO);


    @ApiOperation("获取待下载文件的大小")
    @Path("/download/fileSize")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Result<Long> getFileSize(
            @ApiParam(value = "获取待下载文件大小的请求信息", required = true)
                    GetFileSizeVO getFileSizeVO);


    @ApiOperation("下载文件")
    @Path("/download")
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_JSON)
    Response download(
            @ApiParam(value = "下载文件的请求体", required = true)
                    DownloadVO downloadVO);

    @ApiOperation("获取待下载文件的信息")
    @Path("/download/fileInfo")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Result<FileInfoModel> getFileInfo(
            @ApiParam(value = "获取待下载文件信息的请求信息", required = true)
                    GetFileSizeVO getFileSizeVO);


    @ApiOperation("获取文件索引")
    @Path("/index/{type}/{fileName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Result<FileIndexVO> index(
            @ApiParam(value = "文件名", required = true)
            @PathParam("fileName")
                    String fileName,
            @ApiParam(value = "类型", required = true)
            @PathParam("type")
                    String type);


    @ApiOperation("获取索引信息")
    @Path("/fileindex/{type}/{fileName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Result<FileIndexVO> getFileIndex(
            @ApiParam(value = "文件名", required = true)
            @PathParam("fileName")
                    String fileName,
            @ApiParam(value = "类型", required = true)
            @PathParam("type")
                    String type);
}
