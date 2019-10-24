package com.tencent.devops.common.cos.util;

/**
 * Created by schellingma on 2017/04/21.
 * Powered By Tencent
 */
public class Constants {
    public static final int MAX_RETRY_UPLOAD_COUNT = 3;
    public static final String EXCHANGE_FILE = "exchange_file";
    public static final String QUEUE_FILE = "queue_file";
    public static final String ROUTE_FILE = "file";

    public static final String DEFAULT_CONTENT_TYPE = "text/plain";

//    /**
//     * 第1次重试上传的时间延迟，秒
//     */
//    public static final int SECONDS_RETRY_DELAY_1ST = 10 * 60;
//    /**
//     * 第2次重试上传的时间延迟，秒
//     */
//    public static final int SECONDS_RETRY_DELAY_2ND = 1 * 60 * 60;
//    /**
//     * 第3次重试上传的时间延迟，秒
//     */
//    public static final int SECONDS_RETRY_DELAY_3RD = 12 * 60 * 60;


    /**
     * 第1次重试上传的时间延迟，秒
     */
    public static final int SECONDS_RETRY_DELAY_1ST = 10;
    /**
     * 第2次重试上传的时间延迟，秒
     */
    public static final int SECONDS_RETRY_DELAY_2ND = 1 * 60;
    /**
     * 第3次重试上传的时间延迟，秒
     */
    public static final int SECONDS_RETRY_DELAY_3RD = 1 * 60;


    public static final int RETRY_UPLOAD_TRUNK_SIZE = 4 * 1024 * 1024;
}
