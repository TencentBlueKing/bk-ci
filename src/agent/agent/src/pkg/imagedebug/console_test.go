package imagedebug

import "testing"

func TestCreateExecNoHttp(t *testing.T) {
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
	mgr := NewManager(&ConsoleProxyConfig{Tty: true}, NewOnceChan[struct{}]()).(*manager)
	if _, err := mgr.CreateExecNoHttp(&WebSocketConfig{}); err == nil {
		t.Fatal("expected error for missing container id")
	}
}
