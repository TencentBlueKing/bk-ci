package com.tencent.bk.codecc.task.model;

import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "t_gongfeng_trigger_param")
@AllArgsConstructor
@NoArgsConstructor
public class GongFengTriggerParamEntity {

    @Indexed
    @Field("gongfeng_id")
    private Integer gongfengId;

    @Field("project_id")
    private String projectId;

    @Field("pipeline_id")
    private String pipelineId;

    @Field("task_id")
    private Long taskId;

    @Field("owner")
    private String owner;

    @Field("languages")
    private List<String> languages;

    @Field("tools")
    private List<String> tools;

    @Field("language_rule_set_map")
    private List<CheckerSetVO> languageRuleSetMap;

    @Field("code_lang")
    private Long codeLang;
}
