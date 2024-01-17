import com.tencent.devops.common.api.enums.PlatformEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本体验-新增外部链接公开体验")
data class ExperiencePublicExternalAdd(
    @Schema(title = "体验名称", required = true)
    val experienceName: String,
    @Schema(title = "产品类别", required = true)
    val category: Int,
    @Schema(title = "操作系统", required = true)
    val platform: PlatformEnum,
    @Schema(title = "LOGO地址", required = true)
    val logoUrl: String,
    @Schema(title = "外部链接", required = true)
    val externalLink: String
)
