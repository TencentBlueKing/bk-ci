//go:build windows
// +build windows

package main

import (
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func init() {
	logs.UNTestDebugInit()
}

func TestGetActiveSessionID(t *testing.T) {
	sessionID, err := GetActiveSessionID()
	if err != nil {
		t.Skipf("GetActiveSessionID returned error (expected on headless/CI): %v", err)
	}
	if sessionID > 65535 {
		t.Errorf("sessionID %d seems unreasonably large", sessionID)
	}
	t.Logf("Active session ID: %d", sessionID)
}

func TestSessionProcessInfo_Close(t *testing.T) {
	t.Run("zero_handles", func(t *testing.T) {
		info := &SessionProcessInfo{
			PID:           0,
			ProcessHandle: 0,
			ThreadHandle:  0,
		}
		info.Close()
		if info.ThreadHandle != 0 {
			t.Error("ThreadHandle should be 0 after Close")
		}
	})

	t.Run("idempotent", func(t *testing.T) {
		info := &SessionProcessInfo{
			PID:           1234,
			ProcessHandle: 0,
			ThreadHandle:  0,
		}
		info.Close()
		info.Close() // should not panic
	})
}

func TestEnumerateSessions(t *testing.T) {
	sessions, err := enumerateSessions()
	if err != nil {
		t.Skipf("enumerateSessions failed (may require specific privileges): %v", err)
	}

	if len(sessions) == 0 {
		t.Skip("no sessions found")
	}

	for _, s := range sessions {
		t.Logf("Session ID=%d, State=%d", s.SessionID, s.State)
	}
}

func TestDuplicateUserToken(t *testing.T) {
	sessionID, err := GetActiveSessionID()
	if err != nil {
		t.Skipf("no active session: %v", err)
	}

	token, err := duplicateUserToken(sessionID)
	if err != nil {
		// This commonly fails without SYSTEM or admin privileges
		t.Skipf("duplicateUserToken failed (may need elevated privileges): %v", err)
	}
	defer token.Close()

	if token == 0 {
		t.Error("token should not be zero on success")
	}
	t.Logf("Successfully duplicated token for session %d", sessionID)
}
