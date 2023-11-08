package constant

const (
	// 构建机默认国际化语言
	DEFAULT_LANGUAGE_TYPE = "zh_CN"
	// 构建机接取任务间隔时间
	BuildIntervalInSeconds = 5
	// api 鉴权的头信息
	AuthHeaderBuildType = "X-DEVOPS-BUILD-TYPE" // 构建类型
	AuthHeaderProjectId = "X-DEVOPS-PROJECT-ID" // 项目ID
	AuthHeaderAgentId   = "X-DEVOPS-AGENT-ID"   // Agent ID

	AuthHeaderBuildTypeValue = "AGENT"
)
