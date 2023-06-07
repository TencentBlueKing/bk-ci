package imagedebug

import (
	"net/http"
	"os"
	"sync"

	"github.com/docker/docker/client"
	docker "github.com/fsouza/go-dockerclient"
	dockerclient "github.com/fsouza/go-dockerclient"
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
	// Start create docker client
	Start() error
	CreateExecNoHttp(*WebSocketConfig) (*docker.Exec, error)

	// handler container web console
	StartExec(http.ResponseWriter, *http.Request, *WebSocketConfig)
	CreateExec(http.ResponseWriter, *http.Request, *WebSocketConfig)
	ResizeExec(http.ResponseWriter, *http.Request, *WebSocketConfig)
}

type manager struct {
	sync.RWMutex
	conf                *ConsoleProxyConfig
	dockerClient        *dockerclient.Client
	connectedContainers map[string]bool
}

// NewManager create a Manager object
func NewManager(conf *ConsoleProxyConfig) Manager {
	return &manager{
		conf:                conf,
		connectedContainers: make(map[string]bool),
	}
}

// Start create docker client
func (m *manager) Start() error {
	var err error
	m.dockerClient, err = dockerclient.NewVersionedClientFromEnv(os.Getenv(client.EnvOverrideAPIVersion))
	return err
}
