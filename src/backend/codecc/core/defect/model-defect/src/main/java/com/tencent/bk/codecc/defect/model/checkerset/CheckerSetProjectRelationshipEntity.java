package com.tencent.bk.codecc.defect.model.checkerset;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/1/5
 */
@Data
@Document(collection = "t_checker_set_project_relationship")
@CompoundIndexes({
        @CompoundIndex(name = "checker_set_id_1_project_id_1", def = "{'checker_set_id': 1, 'project_id': 1}")
})
public class CheckerSetProjectRelationshipEntity extends CommonEntity
{
    /**
     * 规则集ID
     */
    @Field("checker_set_id")
    @Indexed
    private String checkerSetId;

    /**
     * 项目ID
     */
    @Field("project_id")
    @Indexed
    private String projectId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 是否使用最新版本
     */
    @Field("use_latest_version")
    private Boolean uselatestVersion;

    /**
     * 是否项目内默认规则集
     */
    private Boolean defaultCheckerSet;
}
