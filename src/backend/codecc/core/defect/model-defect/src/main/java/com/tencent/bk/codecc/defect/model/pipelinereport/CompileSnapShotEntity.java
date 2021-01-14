package com.tencent.bk.codecc.defect.model.pipelinereport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.defect.model.NotRepairedAuthorEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 编译类工具产出物报告快照实体类
 *
 * @version V1.0
 * @date 2019/12/11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompileSnapShotEntity extends ToolSnapShotEntity
{
    /**
     * 分析开始时间
     */
    @Field("start_time")
    @JsonProperty("start_time")
    private long startTime;

    /**
     * 分析结束时间
     */
    @Field("end_time")
    @JsonProperty("end_time")
    private long endTime;

    /**
     * 最近一次分析新增的告警数
     */
    @Field("latest_new_add_count")
    @JsonProperty("latest_new_add_count")
    private int latestNewAddCount;

    /**
     * 最近一次分析关闭的告警数
     */
    @Field("latest_closed_count")
    @JsonProperty("latest_closed_count")
    private int latestClosedCount;

    /**
     * 最近一次分析遗留的告警数
     */
    @Field("latest_exist_count")
    @JsonProperty("latest_exist_count")
    private int latestExistCount;

    /**
     * 所有待修复
     */
    @Field("total_new")
    @JsonProperty("total_new")
    private int totalNew;

    /**
     * 所有已修复
     */
    @Field("total_close")
    @JsonProperty("total_close")
    private int totalClose;

    /**
     * 所有已忽略
     */
    @Field("total_ignore")
    @JsonProperty("total_ignore")
    private int totalIgnore;

    /**
     * 所有已屏蔽
     */
    @Field("total_excluded")
    @JsonProperty("total_excluded")
    private int totalExcluded;

    /**
     * 所有待修复严重
     */
    @Field("total_new_serious")
    @JsonProperty("total_new_serious")
    private int totalNewSerious;

    /**
     * 所有待修复一般
     */
    @Field("total_new_normal")
    @JsonProperty("total_new_normal")
    private int totalNewNormal;

    /**
     * 所有待修复提示
     */
    @Field("total_new_prompt")
    @JsonProperty("total_new_prompt")
    private int totalNewPrompt;

    /**
     * 待修复告警作者
     */
    @Field("author_list")
    @JsonProperty("author_list")
    private List<NotRepairedAuthorEntity> authorList;

    /**
     * cloc扫描总行数
     */
    @Field("total_lines")
    @JsonProperty("total_lines")
    private long totalLines;

    /**
     * cloc扫描总空白行数
     */
    @Field("total_blank_lines")
    @JsonProperty("total_blank_lines")
    private long totalBlankLines;

    /**
     * cloc扫描总注释行数
     */
    @Field("total_comment_lines")
    @JsonProperty("total_comment_lines")
    private long totalCommentLines;
}
