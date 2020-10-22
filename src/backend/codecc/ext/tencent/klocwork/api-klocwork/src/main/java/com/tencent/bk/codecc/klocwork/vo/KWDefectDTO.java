package com.tencent.bk.codecc.klocwork.vo;

import lombok.Data;

import java.util.List;

/**
 * klocwork告警model
 *
 * @version V3.0
 * @date 2019/11/20
 */
@Data
public class KWDefectDTO
{
    private Long id;

    /**
     * 告警的实质状态：New(首次创建的状态),Existing(非首次创建并且未被修复的告警状态),Fixed(已修复)
     */
    private String state;

    private String severity;

    private int severityCode;

    private String code;

    private String title;

    private String message;

    private String file;

    private String method;

    private String owner;

    private String taxonomyName;

    private long dateOriginated;

    private int line;

    /**
     * 构建版本号，如:build_1
     */
    private String build;

    private List<Integer> issueIds;

    private List<Trace> trace;

    @Data
    public class Trace
    {
        private String file;

        private String entity;

        private List<Line> lines;
    }

    @Data
    public class Line
    {
        private int line;

        private String text;

        private Trace trace;
    }
}
