//go:build !windows
// +build !windows

package agentcli

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"
)

var snapshotVars = []string{
	"LANG", "LC_ALL",
	"JAVA_HOME", "JRE_HOME",
	"ANT_HOME", "M2_HOME", "MAVEN_HOME",
	"ANDROID_HOME", "ANDROID_SDK_ROOT",
	"GRADLE_HOME",
	"NVM_DIR", "NVM_BIN",
	"LD_LIBRARY_PATH",
	"GOPATH", "GOROOT",
	"PYENV_ROOT", "CARGO_HOME", "RUSTUP_HOME",
	"NODE_PATH", "PYTHONPATH",
}

// snapshotEnvFiles captures the current shell PATH and selected environment
// variables into .path and .env files under workDir. Called during install and
// start so that the daemon process (which may run under systemd/launchd with a
// minimal environment) can restore the user's interactive shell environment.
func snapshotEnvFiles(workDir string) {
	writePathSnapshot(workDir)
	writeEnvSnapshot(workDir)
}

func writePathSnapshot(workDir string) {
	p := os.Getenv("PATH")
	if p == "" {
		return
	}
	dest := filepath.Join(workDir, ".path")
	if err := os.WriteFile(dest, []byte(p+"\n"), 0644); err != nil {
		printWarn(msgf("failed to write .path: %v", "写入 .path 失败: %v", err))
		return
	}
	printStep(msg("Captured PATH to .path", "已采集 PATH 到 .path"))
}

func writeEnvSnapshot(workDir string) {
	var lines []string
	lines = append(lines, "# BK-CI Agent Environment Snapshot")
	lines = append(lines, fmt.Sprintf("# Updated: %s", time.Now().Format(time.RFC3339)))
	lines = append(lines, "# Edit this file to add custom variables; changes take effect on next restart.")

	count := 0
	for _, key := range snapshotVars {
		val := os.Getenv(key)
		if val == "" {
			continue
		}
		lines = append(lines, fmt.Sprintf("%s=%s", key, val))
		count++
	}

	dest := filepath.Join(workDir, ".env")

	existing := loadExistingUserVars(dest)
	for k, v := range existing {
		if isSnapshotVar(k) {
			continue
		}
		lines = append(lines, fmt.Sprintf("%s=%s", k, v))
		count++
	}

	content := strings.Join(lines, "\n") + "\n"
	if err := os.WriteFile(dest, []byte(content), 0644); err != nil {
		printWarn(msgf("failed to write .env: %v", "写入 .env 失败: %v", err))
		return
	}
	printStep(msgf("Captured %d env vars to .env", "已采集 %d 个环境变量到 .env", count))
}

// loadExistingUserVars reads the current .env file to preserve user-added
// variables that are not in the snapshotVars list.
func loadExistingUserVars(path string) map[string]string {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil
	}
	result := make(map[string]string)
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		parts := strings.SplitN(line, "=", 2)
		if len(parts) != 2 {
			continue
		}
		result[strings.TrimSpace(parts[0])] = parts[1]
	}
	return result
}

func isSnapshotVar(key string) bool {
	for _, v := range snapshotVars {
		if v == key {
			return true
		}
	}
	return false
}
