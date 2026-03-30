package dockercli

import (
	"bytes"
	"context"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
)

type LogFunc func(format string, args ...interface{})

type LogLevel string

const (
	LogLevelDebug LogLevel = "DEBUG"
	LogLevelInfo  LogLevel = "INFO"
	LogLevelWarn  LogLevel = "WARN"
	LogLevelError LogLevel = "ERROR"
)

type LogEntry struct {
	Level   LogLevel
	Message string
}

type EventLogFunc func(LogEntry)

type Runner struct {
	workDir string
	binary  string
	logf    LogFunc
	eventf  EventLogFunc
}

type ContainerInfo struct {
	ID        string
	Name      string
	CreatedAt string
	State     string
	Status    string
}

func RuntimeBinary() string {
	if envs.GApiEnvVars != nil {
		if v, ok := envs.FetchEnv(constant.DevopsAgentContainerRuntime); ok && strings.TrimSpace(v) != "" {
			return strings.TrimSpace(v)
		}
	}
	if v := strings.TrimSpace(os.Getenv(constant.DevopsAgentContainerRuntime)); v != "" {
		return v
	}
	return "docker"
}

func NewRunner(workDir string, logf LogFunc) *Runner {
	return &Runner{
		workDir: workDir,
		binary:  RuntimeBinary(),
		logf:    logf,
	}
}

func NewRunnerWithEvent(workDir string, eventf EventLogFunc) *Runner {
	return &Runner{
		workDir: workDir,
		binary:  RuntimeBinary(),
		eventf:  eventf,
	}
}

func (r *Runner) Binary() string {
	return r.binary
}

func (r *Runner) ServerOS(ctx context.Context) (string, error) {
	stdout, _, err := r.run(ctx, nil, "version", "--format", "{{.Server.Os}}")
	return strings.TrimSpace(stdout), err
}

// ImageExists checks whether a container image exists locally by running
// "image inspect". It relies solely on the exit code (0 = exists) rather
// than parsing stderr text, which varies across Docker/Podman versions.
// If the daemon is unreachable, this returns (false, nil) and the
// subsequent pull or create will surface the real error.
func (r *Runner) ImageExists(ctx context.Context, image string) (bool, error) {
	_, _, err := r.run(ctx, nil, "image", "inspect", image)
	return err == nil, nil
}

func (r *Runner) PullImage(ctx context.Context, image, user, password string) (string, error) {
	if user == "" || password == "" {
		stdout, stderr, err := r.run(ctx, nil, "pull", image)
		return stdout + stderr, err
	}

	cfgDir, err := os.MkdirTemp("", "bkci-docker-config-*")
	if err != nil {
		return "", err
	}
	defer os.RemoveAll(cfgDir)

	registry := registryFromImage(image)
	loginArgs := []string{"--config", cfgDir, "login", "-u", user, "--password-stdin"}
	if registry != "" {
		loginArgs = append(loginArgs, registry)
	}
	if _, _, err := r.run(ctx, []byte(password), loginArgs...); err != nil {
		return "", err
	}

	pullArgs := []string{"--config", cfgDir, "pull", image}
	stdout, stderr, err := r.run(ctx, nil, pullArgs...)
	return stdout + stderr, err
}

func (r *Runner) CreateContainer(ctx context.Context, args []string) (string, error) {
	stdout, _, err := r.run(ctx, nil, append([]string{"create"}, args...)...)
	if err != nil {
		return "", err
	}
	lines := strings.Split(strings.TrimSpace(stdout), "\n")
	if len(lines) == 0 || strings.TrimSpace(lines[len(lines)-1]) == "" {
		return "", fmt.Errorf("empty container id returned from %s create", r.binary)
	}
	return strings.TrimSpace(lines[len(lines)-1]), nil
}

func (r *Runner) StartContainer(ctx context.Context, containerID string) error {
	_, _, err := r.run(ctx, nil, "start", containerID)
	return err
}

func (r *Runner) StopContainer(ctx context.Context, containerID string) error {
	_, _, err := r.run(ctx, nil, "stop", "-t", "0", containerID)
	return err
}

func (r *Runner) RemoveContainer(ctx context.Context, containerID string) error {
	_, _, err := r.run(ctx, nil, "rm", "-f", containerID)
	return err
}

func (r *Runner) WaitContainer(ctx context.Context, containerID string) (int64, error) {
	stdout, _, err := r.run(ctx, nil, "wait", containerID)
	if err != nil {
		return 0, err
	}
	code, parseErr := strconv.ParseInt(strings.TrimSpace(stdout), 10, 64)
	if parseErr != nil {
		return 0, parseErr
	}
	return code, nil
}

func (r *Runner) ContainerLogs(ctx context.Context, containerID string) (string, error) {
	stdout, stderr, err := r.run(ctx, nil, "logs", containerID)
	return stdout + stderr, err
}

func (r *Runner) ListContainers(ctx context.Context, all bool) ([]ContainerInfo, error) {
	args := []string{"ps", "--format", "{{.ID}}\t{{.Names}}\t{{.CreatedAt}}\t{{.State}}\t{{.Status}}"}
	if all {
		args = append(args, "-a")
	}
	stdout, _, err := r.run(ctx, nil, args...)
	if err != nil {
		return nil, err
	}
	var result []ContainerInfo
	for _, line := range strings.Split(strings.TrimSpace(stdout), "\n") {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		parts := strings.SplitN(line, "\t", 5)
		if len(parts) < 5 {
			continue
		}
		result = append(result, ContainerInfo{
			ID:        parts[0],
			Name:      parts[1],
			CreatedAt: parts[2],
			State:     parts[3],
			Status:    parts[4],
		})
	}
	return result, nil
}

func (r *Runner) InspectContainerMountSource(ctx context.Context, containerID, target string) (string, error) {
	stdout, _, err := r.run(ctx, nil,
		"inspect", "-f",
		fmt.Sprintf("{{range .Mounts}}{{if eq .Destination %q}}{{.Source}}{{end}}{{end}}", target),
		containerID,
	)
	return strings.TrimSpace(stdout), err
}

func (r *Runner) run(ctx context.Context, stdin []byte, args ...string) (string, string, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	r.log(classifyCommandLevel(args), "%s", formatCommand(r.binary, args))
	cmd := exec.CommandContext(ctx, r.binary, args...)
	cmd.Dir = r.workDir
	cmd.Env = envs.Envs()
	var stdoutBuf bytes.Buffer
	var stderrBuf bytes.Buffer
	cmd.Stdout = &stdoutBuf
	cmd.Stderr = &stderrBuf
	if stdin != nil {
		cmd.Stdin = bytes.NewReader(stdin)
	}
	var err error
	err = cmd.Run()
	stdout := stdoutBuf.String()
	stderr := stderrBuf.String()
	if strings.TrimSpace(stdout) != "" {
		r.log(classifyStreamLevel(false, err, stdout), "[stdout]\n%s", strings.TrimSpace(stdout))
	}
	if strings.TrimSpace(stderr) != "" {
		r.log(classifyStreamLevel(true, err, stderr), "[stderr]\n%s", strings.TrimSpace(stderr))
	}
	if err != nil {
		return stdout, stderr, fmt.Errorf("%s failed: %w", formatCommand(r.binary, args), err)
	}
	return stdout, stderr, nil
}

func (r *Runner) log(level LogLevel, format string, args ...interface{}) {
	if r.eventf != nil {
		r.eventf(LogEntry{
			Level:   level,
			Message: fmt.Sprintf(format, args...),
		})
		return
	}
	if r.logf != nil {
		r.logf(format, args...)
	}
}

func formatCommand(binary string, args []string) string {
	parts := []string{binary}
	for _, arg := range args {
		if strings.ContainsAny(arg, " \t\n\"'") {
			parts = append(parts, strconv.Quote(arg))
		} else {
			parts = append(parts, arg)
		}
	}
	return strings.Join(parts, " ")
}

func registryFromImage(image string) string {
	s := strings.TrimSpace(strings.TrimPrefix(strings.TrimPrefix(image, "http://"), "https://"))
	first, _, ok := strings.Cut(s, "/")
	if !ok {
		return ""
	}
	if strings.Contains(first, ".") || strings.Contains(first, ":") || first == "localhost" {
		return first
	}
	return ""
}

func classifyCommandLevel(args []string) LogLevel {
	if len(args) >= 2 && args[0] == "image" && args[1] == "inspect" {
		return LogLevelDebug
	}
	return LogLevelInfo
}

func classifyStreamLevel(isStderr bool, runErr error, output string) LogLevel {
	if runErr != nil {
		if isStderr {
			return LogLevelError
		}
		return LogLevelWarn
	}
	if isStderr && looksLikeWarning(output) {
		return LogLevelWarn
	}
	return LogLevelInfo
}

func looksLikeWarning(output string) bool {
	s := strings.ToLower(output)
	return strings.Contains(s, "warning") ||
		strings.Contains(s, "warn:") ||
		strings.Contains(s, "deprecated")
}

func DebugContainerCreatedLongAgo(createdAt string, maxAge time.Duration) bool {
	for _, layout := range []string{
		"2006-01-02 15:04:05 -0700 MST",
		time.RFC3339,
		time.RFC3339Nano,
	} {
		if t, err := time.Parse(layout, createdAt); err == nil {
			return time.Since(t) > maxAge
		}
	}
	return false
}

func RuntimeSocketFromBinary(binary string) string {
	if host := strings.TrimSpace(os.Getenv("DOCKER_HOST")); host != "" {
		return host
	}
	if strings.Contains(strings.ToLower(filepath.Base(binary)), "podman") {
		if runtime.GOOS == "windows" {
			return "npipe:////./pipe/podman-machine-default"
		}
		return "unix:///run/podman/podman.sock"
	}
	if runtime.GOOS == "windows" {
		return "npipe:////./pipe/docker_engine"
	}
	return "unix:///var/run/docker.sock"
}
