# Worker 任务类型参考

## 内置任务

| 任务类型 | 类名 | 路径 | 说明 |
|----------|------|------|------|
| `linuxScript` | `LinuxScriptTask` | `worker-common/.../task/` | Linux Shell 脚本 |
| `windowsScript` | `WindowsScriptTask` | `worker-common/.../task/` | Windows Bat 脚本 |
| `marketBuild` | `MarketAtomTask` | `worker-common/.../task/` | 研发商店插件 |
| `marketBuildLess` | `MarketAtomTask` | `worker-common/.../task/` | 无编译环境插件 |

## 插件任务（plugin 包，自动扫描注册）

| 任务类型 | 类名 | 说明 |
|----------|------|------|
| `reportArchive` | `ReportArchiveTask` | 报告归档 |
| `singleFileArchive` | `SingleFileArchiveTask` | 单文件归档 |
| `buildArchiveGet` | `BuildArchiveGetTask` | 获取构建产物 |
| `customizeArchiveGet` | `CustomizeArchiveGetTask` | 获取自定义产物 |
| `codeGitPull` | `CodeGitPullTask` | Git 代码拉取 |
| `codeGitlabPull` | `CodeGitlabPullTask` | GitLab 代码拉取 |
| `codeSvnPull` | `CodeSvnPullTask` | SVN 代码拉取 |
| `githubPull` | `GithubPullTask` | GitHub 代码拉取 |

## 工具类

### ShellUtil

```kotlin
object ShellUtil {
    fun execute(
        buildId: String, script: String, dir: File, workspace: File,
        buildEnvs: List<BuildEnv>, runtimeVariables: Map<String, String>,
        errorMessage: String = "Fail to run the script"
    ): String
}
```

### BatScriptUtil

```kotlin
object BatScriptUtil {
    fun execute(
        buildId: String, script: String, runtimeVariables: Map<String, String>,
        dir: File, workspace: File, errorMessage: String
    ): String
}
```

### CredentialUtils

```kotlin
// 解析 ${{credentials.xxx}} 格式凭据占位符
fun String.parseCredentialValue(
    context: Map<String, String>?,
    acrossProjectId: String?
): String
```

### WorkspaceUtils

```kotlin
object WorkspaceUtils {
    fun getPipelineWorkspace(pipelineId: String, workspace: String): File
    fun getPipelineLogDir(pipelineId: String): File
    fun getBuildLogProperty(...): TaskBuildLogProperty
}
```
