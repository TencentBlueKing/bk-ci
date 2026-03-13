# 适配器模式（Adapter Pattern）

## 代码库服务适配器

**位置**：`repository/biz-repository/src/main/kotlin/com/tencent/devops/repository/service/`

将不同 SCM（Git、SVN 等）的 API 统一适配到 `IRepositoryService` 接口。

```kotlin
interface IRepositoryService {
    fun getRepository(projectId: String, repositoryId: String): Repository
    fun listRepositories(projectId: String): List<Repository>
    fun createRepository(projectId: String, request: RepositoryCreateRequest): Repository
}

@Service
class CodeGitRepositoryService(
    private val gitApi: GitApi,
    private val credentialService: CredentialService
) : IRepositoryService {

    override fun getRepository(projectId: String, repositoryId: String): Repository {
        val gitRepo = gitApi.getRepository(repositoryId)
        return Repository(
            projectId = projectId,
            repositoryId = repositoryId,
            aliasName = gitRepo.name,
            url = gitRepo.url,
            type = ScmType.CODE_GIT
        )
    }

    override fun listRepositories(projectId: String): List<Repository> {
        val gitRepos = gitApi.listRepositories(projectId)
        return gitRepos.map { adaptToRepository(it) }
    }
}

@Service
class CodeSvnRepositoryService(
    private val svnApi: SVNApi,
    private val credentialService: CredentialService
) : IRepositoryService {

    override fun getRepository(projectId: String, repositoryId: String): Repository {
        val svnRepo = svnApi.getRepository(repositoryId)
        return Repository(
            projectId = projectId,
            repositoryId = repositoryId,
            aliasName = svnRepo.name,
            url = svnRepo.url,
            type = ScmType.CODE_SVN
        )
    }
}
```

**服务注册**：
```kotlin
@Component
class CodeRepositoryServiceLoader : BeanPostProcessor {
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is IRepositoryService) {
            CodeRepositoryServiceRegistrar.register(bean)
        }
        return bean
    }
}

object CodeRepositoryServiceRegistrar {
    private val services = mutableMapOf<ScmType, IRepositoryService>()

    fun register(service: IRepositoryService) {
        services[service.getScmType()] = service
    }

    fun getService(scmType: ScmType): IRepositoryService {
        return services[scmType] ?: throw IllegalArgumentException("Unsupported scm type: $scmType")
    }
}
```

**使用方式**：
```kotlin
val service = CodeRepositoryServiceRegistrar.getService(ScmType.CODE_GIT)
val repo = service.getRepository(projectId, repositoryId)
```
