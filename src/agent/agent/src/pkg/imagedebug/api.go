package imagedebug

import (
	"context"
	"crypto/subtle"
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

const debugTokenHeader = "X-DEVOPS-DEBUG-TOKEN"

// CreateExecReq is createExec request struct
type CreateExecReq struct {
	ContainerID string   `json:"container_id,omitempty"`
	Cmd         []string `json:"cmd,omitempty"`
	User        string   `json:"user,omitempty"`
}

// ResizeExecReq is resizeExec request struct
type ResizeExecReq struct {
	ExecID      string `json:"exec_id,omitempty"`
	ContainerID string `json:"container_id,omitempty"`
	Width       int    `json:"width,omitempty"`
	Height      int    `json:"height,omitempty"`
}

// InitRouter return api server
func InitRouter(ctx context.Context, b Manager, conf *ConsoleProxyConfig, errorChan chan error) *http.Server {
	r := &Router{
		backend: b,
		conf:    conf,
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/start_exec", r.auth(r.startExec))
	mux.HandleFunc("/resize_exec", r.auth(r.resizeExec))
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

func (r *Router) auth(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, req *http.Request) {
		if r.conf == nil || !r.conf.IsAuth {
			next(w, req)
			return
		}

		token := req.URL.Query().Get("token")
		if token == "" {
			token = req.Header.Get(debugTokenHeader)
		}
		if r.conf.AuthToken == "" || token == "" || subtle.ConstantTimeCompare([]byte(token), []byte(r.conf.AuthToken)) != 1 {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		next(w, req)
	}
}

func (r *Router) createExec(w http.ResponseWriter, req *http.Request) {
	http.NotFound(w, req)
}

func (r *Router) startExec(w http.ResponseWriter, req *http.Request) {
	err := req.ParseForm()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	execID := req.FormValue("exec_id")
	containerID := req.FormValue("container_id")
	if execID == "" || containerID == "" {
		http.Error(w, "exec_id and container_id must be provided", http.StatusBadRequest)
		return
	}

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
		return
	}

	if resizeExecReq.ContainerID == "" {
		resizeExecReq.ContainerID = req.URL.Query().Get("container_id")
	}
	if resizeExecReq.ExecID == "" || resizeExecReq.ContainerID == "" {
		http.Error(w, "exec_id and container_id must be provided", http.StatusBadRequest)
		return
	}

	webconsole := &WebSocketConfig{
		ExecID:      resizeExecReq.ExecID,
		ContainerID: resizeExecReq.ContainerID,
		Height:      resizeExecReq.Height,
		Width:       resizeExecReq.Width,
	}

	r.backend.ResizeExec(w, req, webconsole)
}
