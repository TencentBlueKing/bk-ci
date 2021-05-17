package com.tencent.bk.codecc.apiquery.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.bk.codecc.apiquery.defect.model.CommonModel;
import com.tencent.bk.codecc.apiquery.utils.LocalDateDeserializer;
import com.tencent.bk.codecc.apiquery.utils.LocalDateTimeDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;


/**
 * 用户信息实体类
 *
 * @date 2020/10/20
 * @version V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserLogInfoModel extends CommonModel {

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("url")
    private String url;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("login_date")
    private LocalDate loginDate;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonProperty("login_time")
    private LocalDateTime loginTime;

    /**
     * 用于分组统计
     */
    @JsonProperty("user_count")
    private Integer userCount;

    /**
     * 用于分组统计
     */
    @JsonProperty("user_name_list")
    private Set<String> userNameList;
}
