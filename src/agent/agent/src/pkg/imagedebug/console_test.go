package imagedebug

import (
	"net/http/httptest"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func TestManagerStart(t *testing.T) {
	logs.UNTestDebugInit()
	Init()
	mgr := NewManager(&ConsoleProxyConfig{Tty: true}, NewOnceChan[struct{}]()).(*manager)
	if err := mgr.Start(); err != nil {
		t.Fatal(err)
	}
	if mgr.runner == nil {
		t.Fatal("expected runner initialized")
	}
}

func TestCreateExecNoHttp(t *testing.T) {
	logs.UNTestDebugInit()
	Init()
	mgr := NewManager(&ConsoleProxyConfig{Tty: true}, NewOnceChan[struct{}]()).(*manager)
	ref, err := mgr.CreateExecNoHttp(&WebSocketConfig{
		ContainerID: "abc123",
		User:        "root",
		Cmd:         []string{"/bin/bash"},
	})
	if err != nil {
		t.Fatal(err)
	}
	if ref == nil || ref.ID == "" {
		t.Fatal("expected non-empty exec ref")
	}
	if _, ok := mgr.execSessions[ref.ID]; !ok {
		t.Fatal("expected session to be stored")
	}
}

func TestCreateExecNoHttp_Invalid(t *testing.T) {
	logs.UNTestDebugInit()
	Init()
	mgr := NewManager(&ConsoleProxyConfig{Tty: true}, NewOnceChan[struct{}]()).(*manager)
	if _, err := mgr.CreateExecNoHttp(&WebSocketConfig{}); err == nil {
		t.Fatal("expected error for missing container id")
	}
}

func TestGetExecSession_ContainerMismatch(t *testing.T) {
	logs.UNTestDebugInit()
	Init()
	mgr := NewManager(&ConsoleProxyConfig{Tty: true}, NewOnceChan[struct{}]()).(*manager)
	ref, err := mgr.CreateExecNoHttp(&WebSocketConfig{ContainerID: "c1"})
	if err != nil {
		t.Fatal(err)
	}

	if _, err := mgr.getExecSession(&WebSocketConfig{ExecID: ref.ID, ContainerID: "c2"}); err == nil {
		t.Fatal("expected error for container mismatch")
	}
	if _, err := mgr.getExecSession(&WebSocketConfig{ExecID: ref.ID, ContainerID: "c1"}); err != nil {
		t.Fatalf("expected matching container to pass: %v", err)
	}
}

func TestResponseJSON(t *testing.T) {
	logs.UNTestDebugInit()
	Init()
	rec := httptest.NewRecorder()
	if err := ResponseJSON(rec, 200, map[string]string{"ok": "true"}); err != nil {
		t.Fatal(err)
	}
	if rec.Code != 200 {
		t.Fatalf("unexpected status %d", rec.Code)
	}
	if ct := rec.Header().Get("Content-Type"); ct != "application/json" {
		t.Fatalf("unexpected content type %q", ct)
	}
}
