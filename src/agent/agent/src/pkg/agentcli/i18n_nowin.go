//go:build !windows
// +build !windows

package agentcli

func detectPlatformLang() string {
	return ""
}
