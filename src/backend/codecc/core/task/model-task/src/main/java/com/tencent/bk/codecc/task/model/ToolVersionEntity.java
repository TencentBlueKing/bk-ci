package com.tencent.bk.codecc.task.model;

import com.tencent.devops.common.constant.ComConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工具版本实体
 *
 * @version V4.0
 * @date 2020/12/29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolVersionEntity {
    /**
     * 工具版本类型，T-测试版本，G-灰度版本，P-正式发布版本
     */
    @Field("version_type")
    private String versionType;

    /**
     * docker启动运行的命令，命令由工具开发者提供，并支持带选项--json传入input.json
     */
    @Field("docker_trigger_shell")
    private String dockerTriggerShell;

    /**
     * docker镜像存放URL，如：xxx.xxx.xxx.com/paas/public/tlinux2.2_codecc_tools
     */
    @Field("docker_image_url")
    private String dockerImageURL;

    /**
     * 工具docker镜像版本号
     */
    @Field("docker_image_version")
    private String dockerImageVersion;

    /**
     * 工具外部docker镜像版本号，用于关联第三方直接提供的docker镜像版本
     */
    @Field("foreign_docker_image_version")
    private String foreignDockerImageVersion;

    /**
     * docker镜像hash值
     */
    @Field("docker_image_hash")
    private String dockerImageHash;

    /**
     * 创建时间
     */
    @Field("create_date")
    private Long createdDate;

    /**
     * 创建人
     */
    @Field("created_by")
    private String createdBy;

    /**
     * 更新时间
     */
    @Field("updated_date")
    private Long updatedDate;

    /**
     * 更新人
     */
    @Field("updated_by")
    private String updatedBy;


    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ToolVersionEntity toolVersion = (ToolVersionEntity) obj;
        return (versionType.equals(toolVersion.versionType));
    }

    @Override
    public int hashCode() {
        return versionType.hashCode();
    }
}
