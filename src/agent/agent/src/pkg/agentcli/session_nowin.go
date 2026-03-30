//go:build !windows
// +build !windows

package agentcli

import "fmt"

func handleConfigureSession(_ string, _ []string) error {
	return fmt.Errorf(msg("configure-session is only supported on Windows", "configure-session 仅支持 Windows"))
}
