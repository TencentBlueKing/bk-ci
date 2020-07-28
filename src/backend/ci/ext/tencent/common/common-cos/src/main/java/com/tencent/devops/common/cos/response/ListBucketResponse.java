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
import com.tencent.devops.common.cos.model.pojo.ListBucketResult;
import okhttp3.Response;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;

/**
 * Created by schellingma on 2017/04/27.
 * Powered By Tencent
 */
public class ListBucketResponse extends BaseResponse {

    private ListBucketResult listBucketResult;

    @Override
    public void parseResponse(Response response) throws COSException {
        if (response.isSuccessful()) {
            try {
                setSuccess(true);
                parseContent(response.body().bytes());

            } catch (IOException e) {
                setSuccess(false);
                setErrorMessage(e.getMessage());
            }
        } else {
            setSuccess(false);
            setCommonErrorMessage(response.code());
        }
    }

    private void parseContent(byte[] content) throws IOException {
        if(content == null || content.length == 0)
            return;

        try {
            JAXBContext context = JAXBContext.newInstance(ListBucketResult.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            listBucketResult = (ListBucketResult) unmarshaller.unmarshal(new ByteArrayInputStream(content));
        } catch (JAXBException e) {
            throw new IOException("Parse ListBucketResult XML content failed", e);
        }
    }

    public ListBucketResult getListBucketResult() {
        return listBucketResult;
    }



}
