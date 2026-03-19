//go:build !windows

package create

// CheckOnlyProcess 检测是否为唯一进程
func CheckOnlyProcess(processType ProcessType) bool {
	return true
}

// ReleaseMutex 释放互斥体
func ReleaseMutex() {
}
