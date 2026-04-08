package envs

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

const (
	envFileName  = ".env"
	pathFileName = ".path"
)

// LoadEnvFiles reads .env and .path files from workDir and merges them into the
// current process environment. Missing files are silently skipped.
// This is called early in daemon startup, before InitEnvPolling.
func LoadEnvFiles(workDir string) {
	envPath := filepath.Join(workDir, envFileName)
	pathPath := filepath.Join(workDir, pathFileName)

	loaded := 0

	envVars, err := loadEnvFile(envPath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "[envfile] load %s error: %v\n", envPath, err)
	}
	for k, v := range envVars {
		os.Setenv(k, v)
		loaded++
	}

	savedPath, err := loadPathFile(pathPath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "[envfile] load %s error: %v\n", pathPath, err)
	}
	if savedPath != "" {
		merged := mergePath(savedPath, os.Getenv("PATH"))
		os.Setenv("PATH", merged)
		loaded++
	}

	if loaded > 0 {
		fmt.Printf("[envfile] loaded %d env entries from %s and %s\n", loaded, envFileName, pathFileName)
	}
}

// loadEnvFile parses a KEY=VALUE file (systemd EnvironmentFile compatible).
// Lines starting with '#' and blank lines are skipped.
func loadEnvFile(path string) (map[string]string, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, err
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
		key := strings.TrimSpace(parts[0])
		if key == "" {
			continue
		}
		result[key] = parts[1]
	}
	return result, nil
}

// loadPathFile reads a single-line PATH value from the given file.
func loadPathFile(path string) (string, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return "", nil
		}
		return "", err
	}
	return strings.TrimSpace(string(data)), nil
}

// mergePath combines savedPath (from .path file, higher priority) with
// currentPath (from process env), deduplicating entries while preserving order.
func mergePath(savedPath, currentPath string) string {
	seen := make(map[string]bool)
	var merged []string

	for _, p := range strings.Split(savedPath, ":") {
		p = strings.TrimSpace(p)
		if p == "" || seen[p] {
			continue
		}
		seen[p] = true
		merged = append(merged, p)
	}

	for _, p := range strings.Split(currentPath, ":") {
		p = strings.TrimSpace(p)
		if p == "" || seen[p] {
			continue
		}
		seen[p] = true
		merged = append(merged, p)
	}

	return strings.Join(merged, ":")
}
