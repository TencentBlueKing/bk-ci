//go:build windows

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
