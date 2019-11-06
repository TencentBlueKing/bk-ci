/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

package com.tencent.devops.common.cos.response;

import com.tencent.devops.common.cos.model.exception.COSException;
import okhttp3.Response;

/**
 * Created by schellingma on 2017/04/09.
 * Powered By Tencent
 */
public class AppendObjectResponse extends BaseResponse {

    /**
     * 下一次追加操作的起始点，单位：字节
     */
    private long nextAppendPosition;
    /**
     * 分段的校验值
     */
    private String trunkSha1;
    /**
     * 文件的唯一标识
     */
    private String fileETag;

    @Override
    public void parseResponse(Response response) throws COSException {
        if (response.isSuccessful()) {
            setSuccess(true);
            try {
                nextAppendPosition = Long.parseLong(response.header("x-cos-next-append-position", "0"));
            } catch (NumberFormatException ex) {
                throw new COSException("Got invalid x-cos-next-append-position header", ex);
            }
            trunkSha1 = response.header("x-cos-content-sha1", "");
            fileETag = response.header("ETag", "");
        } else {
            setSuccess(false);
            setCommonErrorMessage(response.code());
        }
    }

    public long getNextAppendPosition() {
        return nextAppendPosition;
    }

    public String getTrunkSha1() {
        return trunkSha1;
    }

    public String getFileETag() {
        return fileETag;
    }
}
