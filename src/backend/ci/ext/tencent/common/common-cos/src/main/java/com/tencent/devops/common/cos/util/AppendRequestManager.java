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

package com.tencent.devops.common.cos.util;

import okhttp3.MediaType;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by schellingma on 2017/04/11.
 * Powered By Tencent
 */
public class AppendRequestManager {

    private static final ConcurrentMap<String, ManagerItem> requests = new ConcurrentHashMap<>();

    public static int addRequest(String uuid, ManagerItem request) {
        requests.put(uuid, request);
        return requests.size();
    }

    public static ManagerItem getRequest(String uuid) {
        return requests.get(uuid);
    }

    public static boolean hasRequest(String uuid) {
        return requests.containsKey(uuid);
    }

    public static int removeRequest(String uuid) {
        requests.remove(uuid);
        return requests.size();
    }

    /**
     * 返回不可修改的  Map<> requests
     * @return
     */
    public static Map<String, ManagerItem> getAllRequestsUnmodifiable() {
        return Collections.unmodifiableMap(requests);
    }

    public enum UploadTo {
        COS,
        Ceph
    }

    public static class ManagerItem {
        private String uuid;
        private UploadTo uploadTo;
        private long nextPosition;

        private String bucketName;
        private String fileName;
        private MediaType mediaType;
        private Map<String, String> headerParams;
        private Map<String, String> queryParams;


        public ManagerItem(String uuid, UploadTo uploadTo, long nextPosition,
                           String bucketName, String objName, Map<String, String> headerParams) {
            this.uuid = uuid;
            this.uploadTo = uploadTo;
            this.nextPosition = nextPosition;

            this.bucketName = bucketName;
            this.fileName = objName;
            this.headerParams = headerParams;
        }

        public String getUuid() {
            return uuid;
        }

        public ManagerItem setUuid(final String uuid) {
            this.uuid = uuid;
            return this;
        }

        public UploadTo getUploadTo() {
            return uploadTo;
        }

        public ManagerItem setUploadTo(final UploadTo uploadTo) {
            this.uploadTo = uploadTo;
            return this;
        }

        public long getNextPosition() {
            return nextPosition;
        }

        public ManagerItem setNextPosition(long nextPosition) {
            this.nextPosition = nextPosition;
            return this;
        }

        public long increaseNextPosition(long addPosition) {
            this.nextPosition += addPosition;
            return this.nextPosition;
        }

        public String getBucketName() {
            return bucketName;
        }

        public ManagerItem setBucketName(final String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public String getFileName() {
            return fileName;
        }

        public ManagerItem setFileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Map<String, String> getHeaders() {
            return headerParams;
        }

        public ManagerItem setHeaders(final Map<String, String> headers) {
            this.headerParams = headers;
            return this;
        }

        public void resetUploadToCeph() {
            this.nextPosition = 0L;
            this.uploadTo = UploadTo.Ceph;
        }

    }



}
