/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.common.cos.model.pojo;

import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by schellingma on 2017/04/27.
 * Powered By Tencent
 */
@XmlRootElement(name = "ListBucketResult")
public class ListBucketResult implements Serializable {

    private String bucketName;
    private String prefix;
    private String marker;
    private int maxKeys;
    private Boolean isTruncated;
    private String nextMarker;
    private List<FileItem> fileItems;

    public String getFilePath(FileItem fileItem) {
        if(fileItem == null || StringUtils.isEmpty(fileItem.getKey())) {
            return prefix;
        }
        return String.format("%s/%s", StringUtils.strip(prefix, " /"), fileItem.getKey());
    }

    public String getBucketName() {
        return bucketName;
    }

    @XmlElement(name = "Name")
    public ListBucketResult setBucketName(final String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    @XmlElement(name = "Prefix")
    public ListBucketResult setPrefix(final String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getMarker() {
        return marker;
    }

    @XmlElement(name = "Marker", defaultValue = "0")
    public ListBucketResult setMarker(String marker) {
        this.marker = marker;
        return this;
    }

    public int getMaxKeys() {
        return maxKeys;
    }

    @XmlElement(name = "MaxKeys", defaultValue = "0")
    public ListBucketResult setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
        return this;
    }

    public Boolean getIsTruncated() {
        return isTruncated;
    }

    @XmlElement(name = "IsTruncated", defaultValue = "False")
    @XmlJavaTypeAdapter(CosXmlBooleanAdapter.class)
    public ListBucketResult setIsTruncated(final Boolean isTruncated) {
        this.isTruncated = isTruncated;
        return this;
    }

    public String getNextMarker() {
        return nextMarker;
    }

    @XmlElement(name = "NextMarker", required = false, defaultValue = "0")
    public ListBucketResult setNextMarker(String nextMarker) {
        this.nextMarker = nextMarker;
        return this;
    }

    public List<FileItem> getFileItems() {
        return fileItems;
    }

    /**
     * nillable，表示元素个数可为0
     * @param fileItems
     * @return
     */
    @XmlElement(name = "Contents", nillable = true)
    public ListBucketResult setFileItems(final List<FileItem> fileItems) {
        this.fileItems = fileItems;
        return this;
    }


    @XmlRootElement(name = "Content")
    public static class FileItem implements Serializable {
        private String key;
        private Date lastModified;
        private String sha1;
        private String objType;
        private String eTag;
        private long size;

        private String downloadUrl;
        private String downloadHeaderHost;
        private String downloadHeaderAuth;

        public String getKey() {
            return key;
        }

        @XmlElement(name = "Key")
        public FileItem setKey(final String key) {
            this.key = key;
            return this;
        }

        public Date getLastModified() {
            return lastModified;
        }

        @XmlElement(name = "LastModified")
        @XmlJavaTypeAdapter(CosXmlDateAdapter.class)
        public FileItem setLastModified(final Date lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public String getSha1() {
            return sha1;
        }

        @XmlElement(name = "CosSha1")
        public FileItem setSha1(final String sha1) {
            this.sha1 = sha1;
            return this;
        }

        public String getObjType() {
            return objType;
        }

        @XmlElement(name = "CosObjtype")
        public FileItem setObjType(final String objType) {
            this.objType = objType;
            return this;
        }

        public String getETag() {
            return eTag;
        }

        @XmlElement(name = "ETag")
        public FileItem setETag(final String eTag) {
            this.eTag = eTag;
            return this;
        }

        public long getSize() {
            return size;
        }

        @XmlElement(name = "Size")
        public FileItem setSize(long size) {
            this.size = size;
            return this;
        }


        public String getDownloadUrl() {
            return downloadUrl;
        }

        public FileItem setDownloadUrl(final String downloadUrl) {
            this.downloadUrl =downloadUrl;
            return this;
        }

        public String getDownloadHeaderHost() {
            return downloadHeaderHost;
        }

        public FileItem setDownloadHeaderHost(final String downloadHeaderHost) {
            this.downloadHeaderHost = downloadHeaderHost;
            return this;
        }

        public String getDownloadHeaderAuth() {
            return downloadHeaderAuth;
        }

        public FileItem setDownloadHeaderAuth(final String downloadHeaderAuth) {
            this.downloadHeaderAuth = downloadHeaderAuth;
            return this;
        }

    }

    public static class CosXmlDateAdapter extends XmlAdapter<String, Date> {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public String marshal(Date v) throws Exception {
            synchronized (dateFormat) {
                return dateFormat.format(v);
            }
        }

        @Override
        public Date unmarshal(String v) throws Exception {
            synchronized (dateFormat) {
                return dateFormat.parse(v);
            }
        }

    }

    /**
     * COS 中返回的结果为大写开头的 "True"/"False"， 在默认转换时会都为 false；
     * 此处有大坑。注意在使用时，类型中的属性和getter/setter器的变量类型都得与该 Adapter 中的转出类型保持一致，
     *  如下面转出为 Boolean，在类型中对应的属性不可定义为 boolean 类型
     */
    public static class CosXmlBooleanAdapter extends XmlAdapter<String, Boolean> {
        @Override
        public String marshal(Boolean v) throws Exception {
            return String.valueOf(v);
        }

        @Override
        public Boolean unmarshal(String v) throws Exception {
            return StringUtils.isEmpty(v) ? false : v.toLowerCase().equals("true") ? true : false;
        }

    }
}
