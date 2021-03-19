package com.tencent.bk.codecc.defect.vo.customtool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 自定义工具告警实体类
 *
 * @version V4.0
 * @date 2019/10/16
 */
@Data
public class CustomToolDefectVO
{
    @JsonProperty("checkerName")
    private String checker;

    @JsonProperty("description")
    private String message;

    private String filePath;

    @JsonProperty("line")
    private int lineNum;

    private String pinpointHash;
}
