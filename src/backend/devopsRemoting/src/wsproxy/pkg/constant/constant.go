package constant

const (
	RemotingApiPort = 22999
	RemotingSSHPort = 23001

	KubernetesCoreLabelName      = "bkci.dispatch.kubenetes.remoting/core"
	KubernetesCoreLabelNameValue = "workspace"
	KubernetesWorkspaceIDLabel   = "bkci.dispatch.kubenetes.remoting/workspaceID"
	KubernetesOwnerLabel         = "bkci.dispatch.kubenetes.remoting/owner"

	KubernetesWorkspaceSSHPublicKeys = "bkci.dispatch.kubenetes.remoting/sshPublicKeys"

	// DebugModEnvName debug模式名称
	DebugModEnvName = "DEVOPS_WSPROXY_DEBUG_ENABLE"
)
