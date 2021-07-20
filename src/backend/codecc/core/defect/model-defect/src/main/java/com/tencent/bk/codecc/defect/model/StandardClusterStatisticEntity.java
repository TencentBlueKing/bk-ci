package com.tencent.bk.codecc.defect.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 每次分析结束的统计数据
 *
 * @version V5.0
 * @date 2020/12/07
 */

@Data
@Document(collection = "t_standard_cluster_statistic")
@AllArgsConstructor
@NoArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_build_id_1_time_1", def = "{'task_id': 1, 'build_id': 1, 'time': 1}")
})
public class StandardClusterStatisticEntity {
    /**
     * 任务ID
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 构建ID
     */
    @Field("build_id")
    private String buildId;

    /**
     * 工具数量
     */
    @Field("tool_list")
    private List<String> toolList;

    /**
     * 统计的时间
     */
    @Field("time")
    private long time;

    @Field("defect_count")
    private Integer totalCount;

    @Field("defect_change")
    private Integer defectChange;

    @Field("average_thousand_defect")
    private Double averageThousandDefect;

    @Field("average_thousand_defect_change")
    private Double averageThousandDefectChange;
}
