package exitcode

import (
	"errors"
	"fmt"
	"syscall"
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

func TestAddAndResetExitError(t *testing.T) {
	// clean state
	exitError.Store(nil)

	t.Run("initially_nil", func(t *testing.T) {
		got := GetAndResetExitError()
		if got != nil {
			t.Errorf("expected nil, got %+v", got)
		}
	})

	t.Run("add_then_get", func(t *testing.T) {
		AddExitError(ExitJdkError, "jdk broken")
		got := GetAndResetExitError()
		if got == nil {
			t.Fatal("expected non-nil")
		}
		if got.ErrorEnum != ExitJdkError {
			t.Errorf("ErrorEnum = %q, want %q", got.ErrorEnum, ExitJdkError)
		}
		if got.Message != "jdk broken" {
			t.Errorf("Message = %q, want %q", got.Message, "jdk broken")
		}
	})

	t.Run("reset_clears", func(t *testing.T) {
		AddExitError(ExitWorkerError, "worker crash")
		GetAndResetExitError()

		got := GetAndResetExitError()
		if got != nil {
			t.Errorf("after reset, expected nil, got %+v", got)
		}
	})

	t.Run("last_write_wins", func(t *testing.T) {
		AddExitError(ExitJdkError, "first")
		AddExitError(ExitWorkerError, "second")
		got := GetAndResetExitError()
		if got.ErrorEnum != ExitWorkerError {
			t.Errorf("expected last write ExitWorkerError, got %q", got.ErrorEnum)
		}
	})
}

func TestCheckOsIoError(t *testing.T) {
	exitError.Store(nil)

	t.Run("nil_error_no_effect", func(t *testing.T) {
		exitError.Store(nil)
		CheckOsIoError("/tmp/test", nil)
		if got := GetAndResetExitError(); got != nil {
			t.Errorf("expected nil after nil error, got %+v", got)
		}
	})

	t.Run("enospc", func(t *testing.T) {
		exitError.Store(nil)
		CheckOsIoError("/disk/full", syscall.ENOSPC)
		got := GetAndResetExitError()
		if got == nil {
			t.Fatal("expected exit error for ENOSPC")
		}
		if got.ErrorEnum != ExitLeftDevice {
			t.Errorf("ErrorEnum = %q, want %q", got.ErrorEnum, ExitLeftDevice)
		}
	})

	t.Run("eacces", func(t *testing.T) {
		exitError.Store(nil)
		CheckOsIoError("/no/perm", syscall.EACCES)
		got := GetAndResetExitError()
		if got == nil {
			t.Fatal("expected exit error for EACCES")
		}
		if got.ErrorEnum != ExitNoPermissionDenied {
			t.Errorf("ErrorEnum = %q, want %q", got.ErrorEnum, ExitNoPermissionDenied)
		}
	})

	t.Run("other_error_no_exit", func(t *testing.T) {
		exitError.Store(nil)
		CheckOsIoError("/some/path", fmt.Errorf("random error"))
		got := GetAndResetExitError()
		if got != nil {
			t.Errorf("expected nil for non-ENOSPC/EACCES error, got %+v", got)
		}
	})
}

func TestCheckSignalJdkError2(t *testing.T) {
	exitError.Store(nil)
	jdkSignFlag.Store(0)

	t.Run("nil_keeps_counter_at_zero", func(t *testing.T) {
		CheckSignalJdkError(nil)
		if jdkSignFlag.Load() != 0 {
			t.Errorf("counter = %d, want 0", jdkSignFlag.Load())
		}
	})

	t.Run("non_killed_signal_no_increment", func(t *testing.T) {
		jdkSignFlag.Store(0)
		CheckSignalJdkError(fmt.Errorf("some other error"))
		if jdkSignFlag.Load() != 0 {
			t.Errorf("counter = %d, want 0", jdkSignFlag.Load())
		}
	})

	t.Run("killed_signal_increments", func(t *testing.T) {
		jdkSignFlag.Store(0)
		exitError.Store(nil)
		for i := int32(1); i <= 9; i++ {
			CheckSignalJdkError(fmt.Errorf("signal: killed"))
			if jdkSignFlag.Load() != i {
				t.Errorf("after %d kills, counter = %d", i, jdkSignFlag.Load())
			}
		}
		if got := GetAndResetExitError(); got != nil {
			t.Error("should not trigger exit before 10 consecutive kills")
		}
	})

	t.Run("10th_kill_triggers_exit", func(t *testing.T) {
		jdkSignFlag.Store(9)
		exitError.Store(nil)
		CheckSignalJdkError(fmt.Errorf("signal: killed"))
		got := GetAndResetExitError()
		if got == nil {
			t.Fatal("expected exit error after 10 kills")
		}
		if got.ErrorEnum != ExitJdkError {
			t.Errorf("ErrorEnum = %q, want %q", got.ErrorEnum, ExitJdkError)
		}
	})

	t.Run("nil_decrements", func(t *testing.T) {
		jdkSignFlag.Store(5)
		CheckSignalJdkError(nil)
		if jdkSignFlag.Load() != 4 {
			t.Errorf("counter = %d, want 4", jdkSignFlag.Load())
		}
	})
}

func TestCheckSignalWorkerError2(t *testing.T) {
	exitError.Store(nil)
	workerSignFlag.Store(0)

	t.Run("10th_kill_triggers_exit", func(t *testing.T) {
		workerSignFlag.Store(9)
		exitError.Store(nil)
		CheckSignalWorkerError(fmt.Errorf("signal: killed"))
		got := GetAndResetExitError()
		if got == nil {
			t.Fatal("expected exit error after 10 kills")
		}
		if got.ErrorEnum != ExitWorkerError {
			t.Errorf("ErrorEnum = %q, want %q", got.ErrorEnum, ExitWorkerError)
		}
	})

	t.Run("nil_decrements", func(t *testing.T) {
		workerSignFlag.Store(3)
		CheckSignalWorkerError(nil)
		if workerSignFlag.Load() != 2 {
			t.Errorf("counter = %d, want 2", workerSignFlag.Load())
		}
	})
}

func TestCheckTimeoutError2(t *testing.T) {
	exitError.Store(nil)
	timeoutSignFlag.Store(0)

	t.Run("nil_decrements", func(t *testing.T) {
		timeoutSignFlag.Store(5)
		CheckTimeoutError(nil, 10)
		if timeoutSignFlag.Load() != 4 {
			t.Errorf("counter = %d, want 4", timeoutSignFlag.Load())
		}
	})

	t.Run("error_increments", func(t *testing.T) {
		timeoutSignFlag.Store(0)
		CheckTimeoutError(fmt.Errorf("timeout"), 10)
		if timeoutSignFlag.Load() != 1 {
			t.Errorf("counter = %d, want 1", timeoutSignFlag.Load())
		}
	})

	t.Run("threshold_triggers_exit", func(t *testing.T) {
		timeoutSignFlag.Store(4)
		exitError.Store(nil)
		CheckTimeoutError(fmt.Errorf("timeout"), 5)
		got := GetAndResetExitError()
		if got == nil {
			t.Fatal("expected exit error at threshold")
		}
		if got.ErrorEnum != ExitTimeOutError {
			t.Errorf("ErrorEnum = %q, want %q", got.ErrorEnum, ExitTimeOutError)
		}
	})

	t.Run("below_threshold_no_exit", func(t *testing.T) {
		timeoutSignFlag.Store(0)
		exitError.Store(nil)
		CheckTimeoutError(fmt.Errorf("timeout"), 100)
		got := GetAndResetExitError()
		if got != nil {
			t.Errorf("expected nil below threshold, got %+v", got)
		}
	})
}

func TestWriteFileWithCheck2(t *testing.T) {
	exitError.Store(nil)
	tmpDir := t.TempDir()

	t.Run("success", func(t *testing.T) {
		exitError.Store(nil)
		err := WriteFileWithCheck(tmpDir+"/test.txt", []byte("hello"), 0644)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
	})

	t.Run("permission_error_dir", func(t *testing.T) {
		exitError.Store(nil)
		err := WriteFileWithCheck("/proc/nonexistent/test.txt", []byte("data"), 0644)
		if err == nil {
			t.Error("expected error writing to invalid path")
		}
	})
}
