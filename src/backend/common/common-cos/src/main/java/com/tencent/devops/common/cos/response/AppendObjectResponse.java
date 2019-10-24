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
