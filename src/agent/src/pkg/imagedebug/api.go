package imagedebug

import (
	"context"
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"sync"
)

// Router is api router
type Router struct {
	sync.RWMutex
	conf    *ConsoleProxyConfig
	backend Manager
}

// CreateExecReq is createExec request struct
type CreateExecReq struct {
	ContainerID string   `json:"container_id,omitempty"`
	Cmd         []string `json:"cmd,omitempty"`
	User        string   `json:"user,omitempty"`
}

// ResizeExecReq is resizeExec request struct
type ResizeExecReq struct {
	ExecID string `json:"exec_id,omitempty"`
	Width  int    `json:"width,omitempty"`
	Height int    `json:"height,omitempty"`
}

// InitRouter return api server
func InitRouter(ctx context.Context, b Manager, conf *ConsoleProxyConfig, errorChan chan error) *http.Server {
	r := &Router{
		backend: b,
		conf:    conf,
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/create_exec", r.createExec)
	mux.HandleFunc("/start_exec", r.startExec)
	mux.HandleFunc("/resize_exec", r.resizeExec)
	s := &http.Server{
		Addr:        fmt.Sprintf("%s:%d", r.conf.Address, r.conf.Port),
		Handler:     mux,
		BaseContext: func(net.Listener) context.Context { return ctx },
	}

	imageDebugLogs.Infof("Start http service on(%s:%d)", r.conf.Address, r.conf.Port)

	go func() {
		err := s.ListenAndServe()
		if err != nil {
			errorChan <- err
		}
	}()
	return s
}

func (r *Router) createExec(w http.ResponseWriter, req *http.Request) {
	var createExecReq CreateExecReq
	decoder := json.NewDecoder(req.Body)
	err := decoder.Decode(&createExecReq)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	if createExecReq.ContainerID == "" {
		http.Error(w, "container_id must be provided", http.StatusBadRequest)
		return
	}

	if createExecReq.User == "" {
		createExecReq.User = "root"
	}
	if createExecReq.Cmd == nil {
		createExecReq.Cmd = r.conf.Cmd
	}
	webconsole := &WebSocketConfig{
		ContainerID: createExecReq.ContainerID,
		User:        createExecReq.User,
		Cmd:         createExecReq.Cmd,
	}

	r.backend.CreateExec(w, req, webconsole)
}

func (r *Router) startExec(w http.ResponseWriter, req *http.Request) {
	err := req.ParseForm()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
	execID := req.FormValue("exec_id")
	containerID := req.FormValue("container_id")

	webconsole := &WebSocketConfig{
		ExecID:      execID,
		ContainerID: containerID,
		Origin:      req.Header.Get("Origin"),
	}

	// handler container web console
	r.backend.StartExec(w, req, webconsole)
}

func (r *Router) resizeExec(w http.ResponseWriter, req *http.Request) {
	var resizeExecReq ResizeExecReq
	decoder := json.NewDecoder(req.Body)
	err := decoder.Decode(&resizeExecReq)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
	}

	if resizeExecReq.ExecID == "" {
		http.Error(w, "exec_id must be provided", http.StatusBadRequest)
		return
	}

	webconsole := &WebSocketConfig{
		ExecID: resizeExecReq.ExecID,
		Height: resizeExecReq.Height,
		Width:  resizeExecReq.Width,
	}

	r.backend.ResizeExec(w, req, webconsole)
}
