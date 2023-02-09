package config

type Config struct {
	Config     RemotingConfig
	IDE        *IDEConfig
	DesktopIDE IDEConfig
	WorkSpace  WorkspaceConfig
}

// DevopsRemoting相关配置
type RemotingConfig struct {
	// IDEConfigLocation IDE相关配置的路径
	IDEConfigLocation string `json:"ideConfigLocation"`

	// DesktopIDEConfigLocation 桌面IDE相关配置的路径
	DesktopIDEConfigLocation string `json:"desktopIdeConfigLocation"`

	// FrontendLocation DevopsRemoting前端文件路径
	FrontendLocation string `json:"frontendLocation"`

	// APIServerPort DevopsRemoting Api服务器端口
	APIServerPort int `json:"apiServerPort"`

	// SSHPort SSH服务器端口
	SSHPort int `json:"sshPort"`
}

// IDEConfig 针对IDE进程的特殊配置
type IDEConfig struct {
	// Entrypoint 是DevopsRemoting的启动进程
	Entrypoint string `json:"entrypoint"`

	// EntrypointArgs
	EntrypointArgs []string `json:"entrypointArgs"`

	// ReadinessProbe 配置IDE服务是否正常的状态检测器
	ReadinessProbe struct {
		Type ReadinessProbeType `json:"type"`

		// HTTPProbe 配置http类型的检测
		HTTPProbe struct {
			// Schema http/https默认为http
			Schema string `json:"schema"`

			// Host 请求地址默认为localhost
			Host string `json:"host"`

			// Port 请求端口，默认为DevopsRemoting中配置的端口
			Port int `json:"port"`

			// Path 请求路由，默认为 /
			Path string `json:"path"`
		} `json:"http"`
	} `json:"readinessProbe"`
}

// ReadinessProbeType 检测器类型
type ReadinessProbeType string

const (
	// ReadinessProcessProbe IDE 进程启动后，返回就绪。
	ReadinessProcessProbe ReadinessProbeType = ""

	// ReadinessHTTPProbe 一旦针对 IDE 的单个 HTTP 请求成功，返回 ready。
	ReadinessHTTPProbe ReadinessProbeType = "http"
)

// WorkspaceConfig 工作区特定配置，需要从环境变量中读取
type WorkspaceConfig struct {
	// DebugEnable 是否开启remoting的debug
	DebugEnable bool `env:"DEVOPS_REMOTING_DEBUG_ENABLE"`

	// IDEPort IDE的启动端口，由服务端指定
	IDEPort int `env:"DEVOPS_REMOTING_IDE_PORT"`

	// WorkspaceId 当前工作空间ID
	WorkspaceId string `env:"DEVOPS_REMOTING_WORKSPACE_ID"`

	// WorkspaceRootPath 工作空间根路径
	WorkspaceRootPath string `env:"DEVOPS_REMOTING_WORKSPACE_ROOT_PATH"`

	// GitRemoteRepoUrl Git 远程库地址，用来clone
	GitRemoteRepoUrl string `env:"DEVOPS_REMOTING_GIT_REMOTE_REPO_URL"`

	// GitRemoteRepoBranch Git 远程库分支，用来clone
	GitRemoteRepoBranch string `env:"DEVOPS_REMOTING_GIT_REMOTE_REPO_BRANCH"`

	// RepoRoot Git 存储库位置
	GitRepoRootPath string `env:"DEVOPS_REMOTING_GIT_REPO_ROOT_PATH"`

	// GitUsername 配置glob git username
	GitUsername string `env:"DEVOPS_REMOTING_GIT_USERNAME"`
	// GitEmail 配置glob git email
	GitEmail string `env:"DEVOPS_REMOTING_GIT_EMAIL"`

	// DevopsRemotingYaml 远程开发yaml名称
	DevopsRemotingYaml string `env:"DEVOPS_REMOTING_YAML_NAME"`

	// 是否是首次启动，用来区分命令声明周期即 postCreateCommand和postStartCommand
	WorkspaceFirstCreate string `env:"DEVOPS_REMOTING_WORKSPACE_FIRST_CREATE"`

	// DotfileRepo 用户dotfile的仓库
	DotfileRepo string `env:"DEVOPS_REMOTING_DOTFILE_REPO"`

	// LogRateLimit 限制IDE进程的日志输出。
	// 任何超过此限制的输出都会被静默丢弃。
	// 以 kb/sec 表示
	WorkspaceLogRateLimit int `env:"DEVOPS_REMOTING_IDE_RATELIMIT_LOG"`

	// PreCIDownUrl preci文件的下载地址
	PreCIDownUrl string `env:"DEVOPS_REMOTING_PRECI_DOWN_URL"`
	// PreciGateWayUrl preci网关地址
	PreciGateWayUrl string `env:"DEVOPS_REMOTING_PRECI_GATEWAY_URL"`
	// BackendHost 蓝盾后台地址
	BackendHost string `env:"DEVOPS_REMOTING_BACKEND_HOST"`
}

type InjectFile struct {
	// Form 文件所在位置
	From string `json:"from"`
	// To 文件需要注入的位置
	To string `json:"to"`
}
