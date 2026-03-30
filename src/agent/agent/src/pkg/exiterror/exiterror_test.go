package exitcode

import (
	"errors"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func init() {
	logs.UNTestDebugInit()
}

func TestAddExitError_GetAndReset(t *testing.T) {
	t.Run("no_error", func(t *testing.T) {
		got := GetAndResetExitError()
		if got != nil {
			t.Errorf("expected nil, got %+v", got)
		}
	})

	t.Run("add_then_get", func(t *testing.T) {
		AddExitError(ExitLeftDevice, "disk full")
		got := GetAndResetExitError()
		if got == nil {
			t.Fatal("expected non-nil error")
		}
		if got.ErrorEnum != ExitLeftDevice {
			t.Errorf("ErrorEnum = %q, want %q", got.ErrorEnum, ExitLeftDevice)
		}
		if got.Message != "disk full" {
			t.Errorf("Message = %q, want %q", got.Message, "disk full")
		}
	})

	t.Run("get_clears_error", func(t *testing.T) {
		AddExitError(ExitJdkError, "jdk broken")
		GetAndResetExitError()
		got := GetAndResetExitError()
		if got != nil {
			t.Errorf("second get should return nil, got %+v", got)
		}
	})

	t.Run("last_wins", func(t *testing.T) {
		AddExitError(ExitLeftDevice, "first")
		AddExitError(ExitWorkerError, "second")
		got := GetAndResetExitError()
		if got == nil {
			t.Fatal("expected non-nil")
		}
		if got.ErrorEnum != ExitWorkerError {
			t.Errorf("last AddExitError should win, got %q", got.ErrorEnum)
		}
	})
}

func TestCheckSignalJdkError(t *testing.T) {
	jdkSignFlag.Store(0)
	defer jdkSignFlag.Store(0)

	t.Run("nil_error_no_effect", func(t *testing.T) {
		CheckSignalJdkError(nil)
		if jdkSignFlag.Load() != 0 {
			t.Errorf("flag should be 0, got %d", jdkSignFlag.Load())
		}
	})

	t.Run("non_signal_error", func(t *testing.T) {
		CheckSignalJdkError(errors.New("some other error"))
		if jdkSignFlag.Load() != 0 {
			t.Errorf("non-signal error should not increment, got %d", jdkSignFlag.Load())
		}
	})

	t.Run("signal_killed_increments", func(t *testing.T) {
		jdkSignFlag.Store(0)
		CheckSignalJdkError(errors.New("signal: killed"))
		if jdkSignFlag.Load() != 1 {
			t.Errorf("flag should be 1, got %d", jdkSignFlag.Load())
		}
	})

	t.Run("triggers_exit_at_10", func(t *testing.T) {
		jdkSignFlag.Store(0)
		GetAndResetExitError()

		for i := 0; i < 10; i++ {
			CheckSignalJdkError(errors.New("signal: killed"))
		}

		exitErr := GetAndResetExitError()
		if exitErr == nil {
			t.Fatal("should have triggered exit error at 10")
		}
		if exitErr.ErrorEnum != ExitJdkError {
			t.Errorf("ErrorEnum = %q, want %q", exitErr.ErrorEnum, ExitJdkError)
		}
	})
}

func TestCheckSignalWorkerError(t *testing.T) {
	workerSignFlag.Store(0)
	defer workerSignFlag.Store(0)

	t.Run("signal_killed_increments", func(t *testing.T) {
		workerSignFlag.Store(0)
		CheckSignalWorkerError(errors.New("signal: killed"))
		if workerSignFlag.Load() != 1 {
			t.Errorf("flag should be 1, got %d", workerSignFlag.Load())
		}
	})

	t.Run("nil_decrements", func(t *testing.T) {
		workerSignFlag.Store(3)
		CheckSignalWorkerError(nil)
		if workerSignFlag.Load() != 2 {
			t.Errorf("flag should be 2, got %d", workerSignFlag.Load())
		}
	})
}

func TestCheckTimeoutError(t *testing.T) {
	timeoutSignFlag.Store(0)
	defer timeoutSignFlag.Store(0)

	t.Run("nil_decrements", func(t *testing.T) {
		timeoutSignFlag.Store(5)
		CheckTimeoutError(nil, 10)
		if timeoutSignFlag.Load() != 4 {
			t.Errorf("flag should be 4, got %d", timeoutSignFlag.Load())
		}
	})

	t.Run("triggers_at_threshold", func(t *testing.T) {
		timeoutSignFlag.Store(0)
		GetAndResetExitError()

		for i := int32(0); i < 5; i++ {
			CheckTimeoutError(errors.New("timeout"), 5)
		}

		exitErr := GetAndResetExitError()
		if exitErr == nil {
			t.Fatal("should have triggered exit error")
		}
		if exitErr.ErrorEnum != ExitTimeOutError {
			t.Errorf("ErrorEnum = %q, want %q", exitErr.ErrorEnum, ExitTimeOutError)
		}
	})
}

func TestWriteFileWithCheck(t *testing.T) {
	tmpDir := t.TempDir()

	t.Run("success", func(t *testing.T) {
		err := WriteFileWithCheck(tmpDir+"/test.txt", []byte("hello"), 0644)
		if err != nil {
			t.Errorf("WriteFileWithCheck failed: %v", err)
		}
	})

	t.Run("invalid_path", func(t *testing.T) {
		err := WriteFileWithCheck(tmpDir+"/nonexistent/dir/file.txt", []byte("x"), 0644)
		if err == nil {
			t.Error("expected error for invalid path")
		}
	})
}
