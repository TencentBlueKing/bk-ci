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

package com.tencent.devops.common.cos.util;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by schellingma on 2017/04/22.
 * Powered By Tencent
 */
public class FileUtil {

    public static void removeFileAndParentsIfEmpty(Path path, Path basePath)
            throws IOException {
        if(path == null || path.endsWith(basePath)) return;

        if (Files.isRegularFile(path)) {
            Files.deleteIfExists(path);
        } else if(Files.isDirectory(path)) {
            try {
                Files.delete(path);
            } catch(DirectoryNotEmptyException e) {
                return;
            }
        }

        removeFileAndParentsIfEmpty(path.getParent(), basePath);
    }

    public static String getImgContentType(final String fileSuffix) {
        if(StringUtils.isEmpty(fileSuffix)) {
            return Constants.DEFAULT_CONTENT_TYPE;
        }
        switch (StringUtils.strip(fileSuffix.toLowerCase(), ".")) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpe":
            case "jpeg":
                return "image/jpeg";
            case "tif":
            case "tiff":
                return "image/tiff";
            case "gif":
                return "image/gif";
            case "ico":
                return "image/x-icon";
            default:
                    return Constants.DEFAULT_CONTENT_TYPE;

        }

    }
}
