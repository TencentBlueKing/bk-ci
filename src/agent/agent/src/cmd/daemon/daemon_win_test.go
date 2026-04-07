//go:build windows
// +build windows

package main

import (
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/gofrs/flock"
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

func TestDoWaitBeforeRestart(t *testing.T) {
	logs.UNTestDebugInit()

	origDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	tmpDir := t.TempDir()
	if err := os.Chdir(tmpDir); err != nil {
		t.Fatal(err)
	}
	defer os.Chdir(origDir)

	t.Run("no_lock_returns_quickly", func(t *testing.T) {
		start := time.Now()
		doWaitBeforeRestart(100*time.Millisecond, 5*time.Second, 50*time.Millisecond)
		elapsed := time.Since(start)

		if elapsed < 100*time.Millisecond {
			t.Errorf("returned too fast (%s), expected at least base delay", elapsed)
		}
		if elapsed > 2*time.Second {
			t.Errorf("took too long (%s) with no lock held, expected ~150ms", elapsed)
		}
	})

	t.Run("waits_for_lock_release", func(t *testing.T) {
		lockPath := fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock)
		fl := flock.New(lockPath)
		if err := fl.Lock(); err != nil {
			t.Fatal(err)
		}

		done := make(chan time.Duration, 1)
		start := time.Now()
		go func() {
			doWaitBeforeRestart(50*time.Millisecond, 5*time.Second, 50*time.Millisecond)
			done <- time.Since(start)
		}()

		time.Sleep(300 * time.Millisecond)
		_ = fl.Unlock()

		select {
		case elapsed := <-done:
			if elapsed < 300*time.Millisecond {
				t.Errorf("returned before lock release (%s)", elapsed)
			}
			if elapsed > 2*time.Second {
				t.Errorf("took too long after lock release (%s)", elapsed)
			}
		case <-time.After(5 * time.Second):
			t.Error("doWaitBeforeRestart did not return after lock was released")
		}
	})

	t.Run("timeout_when_lock_held", func(t *testing.T) {
		lockPath := fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock)
		fl := flock.New(lockPath)
		if err := fl.Lock(); err != nil {
			t.Fatal(err)
		}
		defer fl.Unlock()

		start := time.Now()
		doWaitBeforeRestart(50*time.Millisecond, 500*time.Millisecond, 50*time.Millisecond)
		elapsed := time.Since(start)

		if elapsed < 450*time.Millisecond {
			t.Errorf("returned before timeout (%s), expected ~500ms", elapsed)
		}
		if elapsed > 2*time.Second {
			t.Errorf("took too long (%s), expected ~500ms", elapsed)
		}
	})
}

func TestCheckDaemonUpgradeSignal(t *testing.T) {
	origDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	tmpDir := t.TempDir()
	if err := os.Chdir(tmpDir); err != nil {
		t.Fatal(err)
	}
	defer os.Chdir(origDir)

	t.Run("no_signal_file", func(t *testing.T) {
		if checkDaemonUpgradeSignal() {
			t.Error("should return false when no signal file exists")
		}
	})

	t.Run("signal_file_present", func(t *testing.T) {
		signalPath := fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), daemonUpgradeFile)
		if err := os.WriteFile(signalPath, []byte("upgrade"), 0644); err != nil {
			t.Fatal(err)
		}

		if !checkDaemonUpgradeSignal() {
			t.Error("should return true when signal file exists")
		}

		if _, err := os.Stat(signalPath); !os.IsNotExist(err) {
			t.Error("signal file should be removed after detection")
		}
	})

	t.Run("second_call_returns_false", func(t *testing.T) {
		signalPath := fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), daemonUpgradeFile)
		if err := os.WriteFile(signalPath, []byte("upgrade"), 0644); err != nil {
			t.Fatal(err)
		}
		checkDaemonUpgradeSignal()

		if checkDaemonUpgradeSignal() {
			t.Error("second call should return false after signal consumed")
		}
	})
}

func TestCleanupOldDaemonBinary(t *testing.T) {
	tmpDir := t.TempDir()

	t.Run("no_old_file", func(t *testing.T) {
		cleanupOldDaemonBinary(tmpDir)
	})

	t.Run("old_file_removed", func(t *testing.T) {
		oldPath := fmt.Sprintf("%s/devopsDaemon.exe.old", tmpDir)
		if err := os.WriteFile(oldPath, []byte("old binary"), 0644); err != nil {
			t.Fatal(err)
		}

		cleanupOldDaemonBinary(tmpDir)

		if _, err := os.Stat(oldPath); !os.IsNotExist(err) {
			t.Error(".old file should be removed after cleanup")
		}
	})
}

func TestWaitForUpgradeFinish(t *testing.T) {
	logs.UNTestDebugInit()

	origDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	tmpDir := t.TempDir()
	if err := os.Chdir(tmpDir); err != nil {
		t.Fatal(err)
	}
	defer os.Chdir(origDir)

	t.Run("no_lock_held", func(t *testing.T) {
		got := waitForUpgradeFinish()
		if !got {
			t.Error("waitForUpgradeFinish() = false, want true")
		}
	})

	t.Run("blocks_until_lock_released", func(t *testing.T) {
		lockPath := fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock)
		fl := flock.New(lockPath)
		if err := fl.Lock(); err != nil {
			t.Fatal(err)
		}

		done := make(chan bool, 1)
		go func() {
			done <- waitForUpgradeFinish()
		}()

		select {
		case <-done:
			t.Error("waitForUpgradeFinish returned while lock was held")
		case <-time.After(500 * time.Millisecond):
		}

		_ = fl.Unlock()

		select {
		case result := <-done:
			if !result {
				t.Error("waitForUpgradeFinish() = false after lock released, want true")
			}
		case <-time.After(5 * time.Second):
			t.Error("waitForUpgradeFinish did not return after lock was released")
		}
	})

	t.Run("consecutive_calls", func(t *testing.T) {
		for i := 0; i < 3; i++ {
			if got := waitForUpgradeFinish(); !got {
				t.Errorf("call %d: waitForUpgradeFinish() = false, want true", i)
			}
		}
	})
}
