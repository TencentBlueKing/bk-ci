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
