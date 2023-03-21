package types

type PortsStatusResponse struct {
	Ports []*PortsStatus `json:"ports,omitempty"`
}

type PortsStatus struct {
	// LocalPort 应用在本地监听的端口
	LocalPort uint32 `json:"localPort,omitempty"`
	// served 当前端口是否有进程占用
	Served bool `json:"served,omitempty"`
	// Exposed 暴露端口是的信息
	Exposed *ExposedPortInfo `json:"exposed,omitempty"`
	// AutoExposure 自动暴露端口操作的状态
	AutoExposure PortAutoExposure `json:"autoExposure,omitempty"`
	// Tunneled 当端口tunneld的时候会返回值，否则为空
	Tunneled *TunneledPortInfo
	// Desc 端口描述来自devfile
	Desc string `json:"desc,omitempty"`
	// Name 端口名称来自devfile
	Name string `json:"name,omitempty"`
	// 如何打开port
	OnOpen PortsStatusOnOpenAction
}

type ExposedPortInfo struct {
	// 可以被外界访问的范围
	Visibility PortVisibility `json:"visibility,omitempty"`
	// url 端口可以被外界访问的url
	Url string `json:"url,omitempty"`
}

type TunnelVisiblity int32

const (
	TunnelVisiblityNone    TunnelVisiblity = 0
	TunnelVisiblityHost    TunnelVisiblity = 1
	TunnelVisiblityNetwork TunnelVisiblity = 2
)

type TunneledPortInfo struct {
	TargetPort uint32            `json:"target_port,omitempty"`
	Visibility TunnelVisiblity   `json:"visibility,omitempty"`
	Clients    map[string]uint32 `json:"clients,omitempty"`
}

type PortVisibility int32

const (
	PortVisibilityPrivate PortVisibility = 0
	PortVisibilityPublic  PortVisibility = 1
)

const (
	PortVisibilityPrivateName string = "private"
	PortVisibilityPublicName  string = "public"
)

type PortsStatusOnOpenAction int32

const (
	PortsStatusIgnore        PortsStatusOnOpenAction = 0
	PortsStatusOpenBrowser   PortsStatusOnOpenAction = 1
	PortsStatusOpenPreview   PortsStatusOnOpenAction = 2
	PortsStatusNotify        PortsStatusOnOpenAction = 3
	PortsStatusNotifyPrivate PortsStatusOnOpenAction = 4
)

const (
	PortsStatusIgnoreName        string = "ignore"
	PortsStatusOpenBrowserName   string = "openBrowser"
	PortsStatusOpenPreviewName   string = "openPreview"
	PortsStatusNotifyName        string = "notify"
	PortsStatusNotifyPrivateName string = "notifyPrivate"
)

type PortAutoExposure int32

const (
	PortAutoExposureTrying    PortAutoExposure = 0
	PortAutoExposureSucceeded PortAutoExposure = 1
	PortAutoExposureFailed    PortAutoExposure = 2
)

// PortConfig 从yaml转变为的模型，方便使用
type PortConfig struct {
	Name       string
	Desc       string
	Port       float64
	OnOpen     string
	Visibility string
}
