//go:build !windows
// +build !windows

package agentcli

import "fmt"

func handleConfigureSession(_ string, _ []string) error {
	return fmt.Errorf("configure-session is only supported on Windows")
}
