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

func (f *fakeManager) StartExec(w http.ResponseWriter, r *http.Request, conf *WebSocketConfig) {
	f.lastConf = conf
	_ = ResponseJSON(w, http.StatusOK, nil)
}

func (f *fakeManager) CreateExec(w http.ResponseWriter, r *http.Request, conf *WebSocketConfig) {
	f.lastConf = conf
	_ = ResponseJSON(w, http.StatusOK, &ExecRef{ID: "exec-1"})
}

func (f *fakeManager) ResizeExec(w http.ResponseWriter, r *http.Request, conf *WebSocketConfig) {
	f.lastConf = conf
	_ = ResponseJSON(w, http.StatusOK, nil)
}

func TestCreateExecHandler_Disabled(t *testing.T) {
	fm := &fakeManager{}
	router := &Router{backend: fm, conf: &ConsoleProxyConfig{Cmd: []string{"/bin/bash"}}}
	body, _ := json.Marshal(CreateExecReq{ContainerID: "c1"})
	req := httptest.NewRequest(http.MethodPost, "/create_exec", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	router.createExec(rec, req)

	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected 404, got %d body=%s", rec.Code, rec.Body.String())
	}
	if fm.lastConf != nil {
		t.Fatalf("expected backend CreateExec not to be called, got %#v", fm.lastConf)
	}
}

func TestAuthHandler_MissingToken(t *testing.T) {
	fm := &fakeManager{}
	router := &Router{backend: fm, conf: &ConsoleProxyConfig{IsAuth: true, AuthToken: "secret"}}
	req := httptest.NewRequest(http.MethodGet, "/start_exec?exec_id=exec-1&container_id=c1", nil)
	rec := httptest.NewRecorder()

	router.auth(router.startExec)(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401, got %d", rec.Code)
	}
	if fm.lastConf != nil {
		t.Fatalf("expected backend not to be called, got %#v", fm.lastConf)
	}
}

func TestStartExecHandler_ValidQueryToken(t *testing.T) {
	fm := &fakeManager{}
	router := &Router{backend: fm, conf: &ConsoleProxyConfig{IsAuth: true, AuthToken: "secret"}}
	req := httptest.NewRequest(http.MethodGet, "/start_exec?exec_id=exec-1&container_id=c1&token=secret", nil)
	rec := httptest.NewRecorder()

	router.auth(router.startExec)(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d body=%s", rec.Code, rec.Body.String())
	}
	if fm.lastConf == nil || fm.lastConf.ExecID != "exec-1" || fm.lastConf.ContainerID != "c1" {
		t.Fatalf("unexpected start conf: %#v", fm.lastConf)
	}
}

func TestStartExecHandler_MissingParams(t *testing.T) {
	router := &Router{backend: &fakeManager{}, conf: &ConsoleProxyConfig{}}
	req := httptest.NewRequest(http.MethodGet, "/start_exec?exec_id=exec-1", nil)
	rec := httptest.NewRecorder()

	router.startExec(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected 400, got %d", rec.Code)
	}
}

func TestResizeExecHandler_ValidHeaderToken(t *testing.T) {
	fm := &fakeManager{}
	router := &Router{backend: fm, conf: &ConsoleProxyConfig{IsAuth: true, AuthToken: "secret"}}
	body, _ := json.Marshal(ResizeExecReq{ExecID: "exec-1", ContainerID: "c1", Width: 100, Height: 40})
	req := httptest.NewRequest(http.MethodPost, "/resize_exec", bytes.NewReader(body))
	req.Header.Set(debugTokenHeader, "secret")
	rec := httptest.NewRecorder()

	router.auth(router.resizeExec)(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rec.Code)
	}
	if fm.lastConf == nil || fm.lastConf.ExecID != "exec-1" || fm.lastConf.ContainerID != "c1" || fm.lastConf.Width != 100 || fm.lastConf.Height != 40 {
		t.Fatalf("unexpected resize conf: %#v", fm.lastConf)
	}
}

func TestResizeExecHandler_MissingContainerID(t *testing.T) {
	router := &Router{backend: &fakeManager{}, conf: &ConsoleProxyConfig{}}
	body, _ := json.Marshal(ResizeExecReq{ExecID: "exec-1", Width: 100, Height: 40})
	req := httptest.NewRequest(http.MethodPost, "/resize_exec", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	router.resizeExec(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected 400, got %d", rec.Code)
	}
}

func TestResizeExecHandler_ContainerIDFromQuery(t *testing.T) {
	fm := &fakeManager{}
	router := &Router{backend: fm, conf: &ConsoleProxyConfig{}}
	body, _ := json.Marshal(ResizeExecReq{ExecID: "exec-1", Width: 100, Height: 40})
	req := httptest.NewRequest(http.MethodPost, "/resize_exec?container_id=c1", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	router.resizeExec(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rec.Code)
	}
	if fm.lastConf == nil || fm.lastConf.ContainerID != "c1" {
		t.Fatalf("unexpected resize conf: %#v", fm.lastConf)
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
