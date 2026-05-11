import com.tencent.devops.store.pojo.image.response.DockerRepoList
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "获取镜像列表返回模型")
data class ImagePageData(
    @get:Schema(title = "镜像列表")
    val imageList: List<DockerRepoList>,
    @get:Schema(title = "分页start")
    val start: Int,
    @get:Schema(title = "分页限制")
    val limit: Int,
    @get:Schema(title = "总共数量")
    val total: Int
)
