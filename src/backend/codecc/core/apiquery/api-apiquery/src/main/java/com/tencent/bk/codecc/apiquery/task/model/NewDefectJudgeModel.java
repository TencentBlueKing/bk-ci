package com.tencent.bk.codecc.apiquery.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 新告警判定实体类
 *
 * @version V1.0
 * @date 2019/12/3
 */
@Data
public class NewDefectJudgeModel
{
    /**
     * 判定方式1：按日期；2：按构建
     */
    @JsonProperty("judge_by")
    private Integer judgeBy;

    /**
     * 新告警开始的日期
     */
    @JsonProperty("from_date")
    private String fromDate;

    /**
     * 新告警开始的日期时间戳
     */
    @JsonProperty("from_date_time")
    private Long fromDateTime;

    /**
     * 最近几次构建产生的告警为新告警
     */
    @JsonProperty("lastest_times")
    private Integer lastestTimes;
}
