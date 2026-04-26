//go:build windows
// +build windows

package agentcli

import "golang.org/x/sys/windows"

var (
	modkernel32Win               = windows.NewLazySystemDLL("kernel32.dll")
	procSetConsoleOutputCP       = modkernel32Win.NewProc("SetConsoleOutputCP")
	procSetConsoleCP             = modkernel32Win.NewProc("SetConsoleCP")
	procGetUserDefaultUILanguage = modkernel32Win.NewProc("GetUserDefaultUILanguage")
)

func init() {
	procSetConsoleOutputCP.Call(65001) // UTF-8
	procSetConsoleCP.Call(65001)
}

func detectPlatformLang() string {
	langID, _, _ := procGetUserDefaultUILanguage.Call()
	primaryLang := langID & 0x3FF
	if primaryLang == 0x04 { // LANG_CHINESE
		return "zh_CN"
	}
	return ""
}
