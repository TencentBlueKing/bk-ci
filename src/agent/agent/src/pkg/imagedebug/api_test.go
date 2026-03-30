package imagedebug

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

type fakeManager struct {
	lastConf *WebSocketConfig
}

func (f *fakeManager) Start() error { return nil }
func (f *fakeManager) CreateExecNoHttp(conf *WebSocketConfig) (*ExecRef, error) {
	f.lastConf = conf
	return &ExecRef{ID: "exec-1"}, nil
}
func (f *fakeManager) StartExec(http.ResponseWriter, *http.Request, *WebSocketConfig) {}
func (f *fakeManager) CreateExec(w http.ResponseWriter, r *http.Request, conf *WebSocketConfig) {
	f.lastConf = conf
	_ = ResponseJSON(w, http.StatusOK, &ExecRef{ID: "exec-1"})
}
func (f *fakeManager) ResizeExec(w http.ResponseWriter, r *http.Request, conf *WebSocketConfig) {
	f.lastConf = conf
	_ = ResponseJSON(w, http.StatusOK, nil)
}

func TestCreateExecHandler_Valid(t *testing.T) {
	fm := &fakeManager{}
	router := &Router{backend: fm, conf: &ConsoleProxyConfig{Cmd: []string{"/bin/bash"}}}
	body, _ := json.Marshal(CreateExecReq{
		ContainerID: "c1",
		User:        "",
		Cmd:         nil,
	})
	req := httptest.NewRequest(http.MethodPost, "/create_exec", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	router.createExec(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d body=%s", rec.Code, rec.Body.String())
	}
	if fm.lastConf == nil {
		t.Fatal("expected backend CreateExec to be called")
	}
	if fm.lastConf.ContainerID != "c1" {
		t.Fatalf("container id = %q", fm.lastConf.ContainerID)
	}
	if fm.lastConf.User != "root" {
		t.Fatalf("default user = %q, want root", fm.lastConf.User)
	}
	if len(fm.lastConf.Cmd) != 1 || fm.lastConf.Cmd[0] != "/bin/bash" {
		t.Fatalf("default cmd = %#v", fm.lastConf.Cmd)
	}
}

func TestCreateExecHandler_InvalidBody(t *testing.T) {
	router := &Router{backend: &fakeManager{}, conf: &ConsoleProxyConfig{}}
	req := httptest.NewRequest(http.MethodPost, "/create_exec", bytes.NewReader([]byte("{")))
	rec := httptest.NewRecorder()

	router.createExec(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected 400, got %d", rec.Code)
	}
}

func TestCreateExecHandler_MissingContainerID(t *testing.T) {
	router := &Router{backend: &fakeManager{}, conf: &ConsoleProxyConfig{}}
	body, _ := json.Marshal(CreateExecReq{})
	req := httptest.NewRequest(http.MethodPost, "/create_exec", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	router.createExec(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected 400, got %d", rec.Code)
	}
}

func TestResizeExecHandler_Valid(t *testing.T) {
	fm := &fakeManager{}
	router := &Router{backend: fm, conf: &ConsoleProxyConfig{}}
	body, _ := json.Marshal(ResizeExecReq{ExecID: "exec-1", Width: 100, Height: 40})
	req := httptest.NewRequest(http.MethodPost, "/resize_exec", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	router.resizeExec(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rec.Code)
	}
	if fm.lastConf == nil || fm.lastConf.ExecID != "exec-1" || fm.lastConf.Width != 100 || fm.lastConf.Height != 40 {
		t.Fatalf("unexpected resize conf: %#v", fm.lastConf)
	}
}

func TestResizeExecHandler_MissingExecID(t *testing.T) {
	router := &Router{backend: &fakeManager{}, conf: &ConsoleProxyConfig{}}
	body, _ := json.Marshal(ResizeExecReq{Width: 100, Height: 40})
	req := httptest.NewRequest(http.MethodPost, "/resize_exec", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	router.resizeExec(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected 400, got %d", rec.Code)
	}
}

func TestInitRouter(t *testing.T) {
	logs.UNTestDebugInit()
	Init()
	fm := &fakeManager{}
	errCh := make(chan error, 1)
	srv := InitRouter(context.Background(), fm, &ConsoleProxyConfig{Address: "127.0.0.1", Port: 0}, errCh)
	if srv == nil {
		t.Fatal("expected server")
	}
	_ = srv.Close()
}
