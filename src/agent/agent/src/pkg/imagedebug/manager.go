package imagedebug

import (
	"net/http"
	"os/exec"
	"sync"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/dockercli"
)

// WebSocketConfig is config
type WebSocketConfig struct {
	Height      int
	Width       int
	Privilege   bool
	Cmd         []string
	Tty         bool
	ContainerID string
	Token       string
	Origin      string
	User        string
	ExecID      string
}

// Manager is an interface
type Manager interface {
	// Start initialize runtime state
	Start() error
	CreateExecNoHttp(*WebSocketConfig) (*ExecRef, error)

	// StartExec handler container web console
	StartExec(http.ResponseWriter, *http.Request, *WebSocketConfig)
	CreateExec(http.ResponseWriter, *http.Request, *WebSocketConfig)
	ResizeExec(http.ResponseWriter, *http.Request, *WebSocketConfig)
}

type manager struct {
	sync.RWMutex
	conf                *ConsoleProxyConfig
	runner              *dockercli.Runner
	connectedContainers map[string]bool
	execSessions        map[string]*execSession
	// 因为目前是一个链接启动一个server，所以直接给每个实例绑定一个管道做为ws关闭的通知
	doneChan *OnceChan[struct{}]
}

type ExecRef struct {
	ID string `json:"Id"`
}

type execSession struct {
	conf *WebSocketConfig
	cmd  *exec.Cmd
	pty  PtyFile
}

type PtyFile interface {
	Close() error
	Read([]byte) (int, error)
	Write([]byte) (int, error)
}

// NewManager create a Manager object
func NewManager(conf *ConsoleProxyConfig, doneChan *OnceChan[struct{}]) Manager {
	return &manager{
		conf:                conf,
		connectedContainers: make(map[string]bool),
		execSessions:        make(map[string]*execSession),
		doneChan:            doneChan,
	}
}

// Start initialize runtime runner
func (m *manager) Start() error {
	m.runner = dockercli.NewRunner("", func(format string, args ...interface{}) {
		imageDebugLogs.Infof(format, args...)
	})
	return nil
}
