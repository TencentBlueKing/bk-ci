//go:build windows

package create

import (
	"errors"
	"fmt"
	"runtime"

	"golang.org/x/sys/windows"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
)

var mutexHandle windows.Handle

// CheckOnlyProcess 检测是否为唯一进程
func CheckOnlyProcess(processType ProcessType) bool {
	mutexName := fmt.Sprintf("Global\\BKCI_AGENT_%s_Mutex", processType)

	namePtr, err := windows.UTF16PtrFromString(mutexName)
	if err != nil {
		logs.WithError(err).Error("mutex name to UTF16 failed")
		return false
	}

	handle, err := windows.CreateMutex(nil, false, namePtr)
	if err != nil {
		logs.WithError(err).Error("create mutex failed")
		return false
	}

	if errors.Is(windows.GetLastError(), windows.ERROR_ALREADY_EXISTS) {
		_ = windows.CloseHandle(handle)
		logs.Error("process already exists")
		return false
	}

	mutexHandle = handle
	logs.Info("create mutex success")
	return true
}

// ReleaseMutex 释放互斥体
func ReleaseMutex() {
	keepMutexAlive()
	if mutexHandle != 0 {
		_ = windows.CloseHandle(mutexHandle)
	}
}

// keepMutexAlive 保持互斥体存活
func keepMutexAlive() {
	if mutexHandle != 0 {
		// Mutex 会在进程退出时自动释放
		// 这里只是为了防止被 GC
		runtime.KeepAlive(mutexHandle)
	}
}
