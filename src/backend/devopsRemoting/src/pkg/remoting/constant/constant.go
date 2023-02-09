package constant

const (
	// RemotingUserHome 远程开发登录用户的home目录
	RemotingUserHome = "/root"
	// DebugModEnvName debug模式名称
	DebugModEnvName = "DEVOPS_REMOTING_DEBUG_ENABLE"
	// DevfileDir 项目devfile的文件夹
	DevfileDir = ".preci"
	// DefaultDevFileName 默认devfile的名称
	DefaultDevFileName = "devfile.yaml"
	// GitOAuthUser 工蜂oauth鉴权的用户名
	GitOAuthUser = "oauth2"
	// WorkspaceSchema 工作空间的类型，TODO：未来应该全部改为https
	WorkspaceSchema = "http://"
	// blank模板不需要默认的devfile
	BlankDevfileName = "BLANK"
)
