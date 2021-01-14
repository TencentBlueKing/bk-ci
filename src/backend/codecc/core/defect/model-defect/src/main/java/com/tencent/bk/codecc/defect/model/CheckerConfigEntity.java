package com.tencent.bk.codecc.defect.model;


import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 规则参数配置
 * 唯一：task_id + tool_name + checker_key
 *
 * @date 2019/11/12
 * @version V1.0
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_checker_config")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1_checker_key", def = "{'task_id': 1, 'tool_name': 1, 'checker_key':1}")
})
public class CheckerConfigEntity extends CommonEntity {

    /**
     * 任务ID
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 工具名称
     */
    @Field("tool_name")
    private String toolName;

    /**
     * 告警类型key，唯一标识，如：qoc_lua_UseVarIfNil
     */
    @Field("checker_key")
    private String checkerKey;

    /**
     * 规则配置
     */
    private String props;

    /**
     * 规则参数值
     */
    private String paramValue;

    /**
     * 规则描述
     */
    private String checkerDesc;

    /**
     * 规则信息
     */
    @DBRef
    @Field("checker_detail")
    private CheckerDetailEntity checkerDetail;


}


