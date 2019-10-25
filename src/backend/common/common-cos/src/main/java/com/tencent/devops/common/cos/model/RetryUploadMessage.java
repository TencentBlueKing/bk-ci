package com.tencent.devops.common.cos.model;

import com.tencent.devops.common.cos.util.AppendRequestManager;
import com.tencent.devops.common.cos.util.Constants;

import java.util.Map;

/**
 * Created by schellingma on 2017/04/21.
 * Powered By Tencent
 */
public class RetryUploadMessage {

    private String uuid;
    private String bucketName;
    private String fileName;
//    private String mediaType;
    private Map<String, String> headersParams;
    private AppendRequestManager.UploadTo uploadTo = AppendRequestManager.UploadTo.COS;
    private long nextPosition;
    private int maxRetryCount;
    private int retryCount;


    public RetryUploadMessage setFromManagerItem(AppendRequestManager.ManagerItem managerItem, int maxRetryCount) {
        if(managerItem != null) {
            this.uuid = managerItem.getUuid();
            this.bucketName = managerItem.getBucketName();
            this.fileName = managerItem.getFileName();
            this.headersParams = managerItem.getHeaders();
            this.nextPosition = managerItem.getNextPosition();
            this.uploadTo = managerItem.getUploadTo();
        }
        this.maxRetryCount = maxRetryCount;
        this.retryCount = 0;

        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public RetryUploadMessage setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getBucketName() {
        return bucketName;
    }

    public RetryUploadMessage setBucketName(final String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public RetryUploadMessage setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public Map<String, String> getHeadersParams() {
        return headersParams;
    }

    public RetryUploadMessage setHeadersParams(Map<String, String> headersParams) {
        this.headersParams = headersParams;
        return this;
    }

    public AppendRequestManager.UploadTo getUploadTo() {
        return uploadTo;
    }

    public RetryUploadMessage setUploadTo(AppendRequestManager.UploadTo uploadTo) {
        this.uploadTo = uploadTo;
        return this;
    }

    public long getNextPosition() {
        return nextPosition;
    }

    public RetryUploadMessage setNextPosition(long nextPosition) {
        this.nextPosition = nextPosition;
        return this;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public RetryUploadMessage setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public RetryUploadMessage setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    /**
     * 重试次数加1
     * @return
     */
    public int countUpRetryCount() {
        this.retryCount++;
        return retryCount;
    }

    /**
     * 是否还可以重试
     * @return
     */
    public boolean stillRetry() {
        return retryCount <= maxRetryCount;
    }

    public int getRetryDelaySeconds() {
        switch (retryCount) {
            case 0:
            case 1:
                return Constants.SECONDS_RETRY_DELAY_1ST;
            case 2:
                return Constants.SECONDS_RETRY_DELAY_2ND;
            case 3:
                return Constants.SECONDS_RETRY_DELAY_3RD;
        }
        return Constants.SECONDS_RETRY_DELAY_3RD;
    }


    @Override
    public String toString() {
        return String.format("file(%s) to %s, uuid:%s, retryCount:%s, maxRetryCount:%s, delay:%s, nextPosition:%s ",
                this.getFileName(),
                this.getUploadTo(),
                this.getUuid(),
                this.getRetryCount(),
                this.getMaxRetryCount(),
                this.getRetryDelaySeconds(),
                this.getNextPosition()
        );
    }


}
