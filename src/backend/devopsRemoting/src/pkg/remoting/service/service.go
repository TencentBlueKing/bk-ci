package service

import (
	"devopsRemoting/src/pkg/remoting/modules/ports"
	"devopsRemoting/src/pkg/remoting/modules/ssh"
	"devopsRemoting/src/pkg/remoting/modules/terminal"
	"sync"
)

type DesktopIDEStatus struct {
	Link     string `json:"link"`
	Label    string `json:"label"`
	ClientID string `json:"clientID,omitempty"`
	Kind     string `json:"kind,omitempty"`
}

type IdeReadyState struct {
	Ready bool
	Info  *DesktopIDEStatus
	Cond  *sync.Cond
}

// Wait 返回一个在 IDE 准备就绪时发出的通道
func (service *IdeReadyState) Wait() <-chan struct{} {
	ready := make(chan struct{})
	go func() {
		service.Cond.L.Lock()
		for !service.Ready {
			service.Cond.Wait()
		}
		service.Cond.L.Unlock()
		close(ready)
	}()
	return ready
}

// Get 检查是否准备就绪
func (service *IdeReadyState) Get() (bool, *DesktopIDEStatus) {
	service.Cond.L.Lock()
	ready := service.Ready
	info := service.Info
	service.Cond.L.Unlock()
	return ready, info
}

// Set 更新IDE状态
func (service *IdeReadyState) Set(ready bool, info *DesktopIDEStatus) {
	service.Cond.L.Lock()
	defer service.Cond.L.Unlock()
	if service.Ready == ready {
		return
	}
	service.Ready = ready
	service.Info = info
	service.Cond.Broadcast()
}

var ApiService *ApiServiceType

type ApiServiceType struct {
	CommandManager *CommandManager
	TermSrv        *terminal.MuxTerminalService
	Ports          *ports.PortsManager
	SSH            *ssh.SSHService
	Token          *TokenService
}

func init() {
	ApiService = new(ApiServiceType)
}
