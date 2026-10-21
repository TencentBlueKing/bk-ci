package job

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/dockercli"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
)

func TestBuildDockerCreateArgs_DefaultNetworkAndEntrypoint(t *testing.T) {
	logs.UNTestDebugInit()
	envs.Init()
	oldAgentEnv := config.GAgentEnv
	t.Cleanup(func() { config.GAgentEnv = oldAgentEnv })
	config.GAgentEnv = &config.AgentEnv{}
	config.GAgentEnv.SetAgentIp("10.0.0.8")
	workDir := t.TempDir()
	oldWd, _ := os.Getwd()
	defer os.Chdir(oldWd)
	_ = os.Chdir(workDir)

	config.GAgentConfig = &config.AgentConfig{
		ProjectId:    "p1",
		Gateway:      "http://devops.example.com",
		JdkDirPath:   filepath.Join(workDir, "jdk"),
		Jdk17DirPath: "",
	}
	_ = os.MkdirAll(config.GAgentConfig.JdkDirPath, 0755)
	_ = os.WriteFile(filepath.Join(workDir, config.WorkAgentFile), []byte("x"), 0644)
	_ = os.WriteFile(filepath.Join(workDir, config.DockerInitFile), []byte("x"), 0644)

	buildInfo := &api.ThirdPartyBuildInfo{
		BuildId:    "b1",
		VmSeqId:    "vm1",
		PipelineId: "ppl1",
		DockerBuildInfo: &api.ThirdPartyDockerBuildInfo{
			AgentId:   "a1",
			SecretKey: "s1",
			Image:     "ubuntu:latest",
		},
	}

	args, err := buildDockerCreateArgs("c1", "ubuntu:latest", buildInfo)
	if err != nil {
		t.Fatal(err)
	}
	joined := strings.Join(args, " ")
	for _, mustContain := range []string{
		"--name c1",
		"--network bridge",
		"--entrypoint /bin/sh",
		"ubuntu:latest -c /data/init.sh",
		"devops_project_id=p1",
		"devops_agent_id=a1",
		"devops_agent_secret_key=s1",
		"agent_build_env=DOCKER",
		"-e BK_CI_AGENT_IP=10.0.0.8",
	} {
		if !strings.Contains(joined, mustContain) {
			t.Fatalf("expected args to contain %q, got: %s", mustContain, joined)
		}
	}
}

func TestBuildDockerCreateArgs_RespectsCustomNetwork(t *testing.T) {
	logs.UNTestDebugInit()
	envs.Init()
	oldAgentEnv := config.GAgentEnv
	t.Cleanup(func() { config.GAgentEnv = oldAgentEnv })
	config.GAgentEnv = &config.AgentEnv{}
	config.GAgentEnv.SetAgentIp("10.0.0.8")
	workDir := t.TempDir()
	oldWd, _ := os.Getwd()
	defer os.Chdir(oldWd)
	_ = os.Chdir(workDir)

	config.GAgentConfig = &config.AgentConfig{
		ProjectId:  "p1",
		Gateway:    "http://devops.example.com",
		JdkDirPath: filepath.Join(workDir, "jdk"),
	}
	_ = os.MkdirAll(config.GAgentConfig.JdkDirPath, 0755)
	_ = os.WriteFile(filepath.Join(workDir, config.WorkAgentFile), []byte("x"), 0644)
	_ = os.WriteFile(filepath.Join(workDir, config.DockerInitFile), []byte("x"), 0644)

	buildInfo := &api.ThirdPartyBuildInfo{
		BuildId:    "b1",
		VmSeqId:    "vm1",
		PipelineId: "ppl1",
		DockerBuildInfo: &api.ThirdPartyDockerBuildInfo{
			AgentId:   "a1",
			SecretKey: "s1",
			Image:     "ubuntu:latest",
			Options: api.DockerOptions{
				Network: []string{"customnet"},
			},
		},
	}

	args, err := buildDockerCreateArgs("c1", "ubuntu:latest", buildInfo)
	if err != nil {
		t.Fatal(err)
	}
	joined := strings.Join(args, " ")
	if strings.Contains(joined, "--network bridge") {
		t.Fatalf("did not expect default bridge when custom network exists: %s", joined)
	}
	if !strings.Contains(joined, "--network customnet") {
		t.Fatalf("expected custom network in args: %s", joined)
	}
}

func TestParseContainerEnv_AgentIP(t *testing.T) {
	oldConfig, oldAgentEnv, oldAPIEnvs := config.GAgentConfig, config.GAgentEnv, envs.GApiEnvVars
	t.Cleanup(func() {
		config.GAgentConfig, config.GAgentEnv, envs.GApiEnvVars = oldConfig, oldAgentEnv, oldAPIEnvs
	})
	config.GAgentConfig = &config.AgentConfig{ProjectId: "p1", Gateway: "http://devops.example.com"}
	for _, tt := range []struct {
		name    string
		ip      string
		apiEnvs map[string]string
	}{
		{name: "ipv4", ip: "10.0.0.8"},
		{name: "next_build_uses_current_ip", ip: "10.0.0.9"},
		{name: "ipv6", ip: "2001:db8::8"},
		{name: "loopback", ip: "127.0.0.1"},
		{name: "empty_ip_is_not_redetected"},
		{name: "overrides_configured_value", ip: "10.0.0.8", apiEnvs: map[string]string{
			"BK_CI_AGENT_IP": "192.0.2.1", "CUSTOM_VAR": "preserved",
		}},
	} {
		t.Run(tt.name, func(t *testing.T) {
			config.GAgentEnv = &config.AgentEnv{}
			config.GAgentEnv.SetAgentIp(tt.ip)
			envs.GApiEnvVars = nil
			if tt.apiEnvs != nil {
				envs.Init()
				envs.GApiEnvVars.SetEnvs(tt.apiEnvs)
			}
			vars := parseContainerEnv(&api.ThirdPartyDockerBuildInfo{AgentId: "a1", SecretKey: "s1"})
			if got, want := vars[len(vars)-1], "BK_CI_AGENT_IP="+tt.ip; got != want {
				t.Fatalf("last container env=%q, want %q", got, want)
			}
			if tt.apiEnvs != nil {
				if !strings.Contains(strings.Join(vars, "\n"), "CUSTOM_VAR=preserved") {
					t.Fatal("unrelated configured environment variable was lost")
				}
				if got, _ := envs.GApiEnvVars.Get("BK_CI_AGENT_IP"); got != tt.apiEnvs["BK_CI_AGENT_IP"] {
					t.Fatal("configured environment was modified")
				}
			}
		})
	}
}

func TestMapDockerRunnerLogLevel(t *testing.T) {
	tests := []struct {
		name string
		in   dockercli.LogLevel
		want api.LogType
	}{
		{"debug", dockercli.LogLevelDebug, api.LogtypeDebug},
		{"info", dockercli.LogLevelInfo, api.LogtypeDebug},
		{"warn", dockercli.LogLevelWarn, api.LogtypeWarn},
		{"error", dockercli.LogLevelError, api.LogtypeError},
		{"unknown_defaults_to_log", dockercli.LogLevel("OTHER"), api.LogtypeDebug},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := mapDockerRunnerLogLevel(tt.in); got != tt.want {
				t.Errorf("mapDockerRunnerLogLevel(%q) = %q, want %q", tt.in, got, tt.want)
			}
		})
	}
}

func TestRouteLogByType(t *testing.T) {
	tests := []struct {
		name    string
		red     bool
		logType api.LogType
		want    logRoute
	}{
		{"explicit_red_wins", true, api.LogtypeLog, logRouteRed},
		{"error_routes_red", false, api.LogtypeError, logRouteRed},
		{"warn_routes_yellow", false, api.LogtypeWarn, logRouteYellow},
		{"debug_routes_normal", false, api.LogtypeDebug, logRouteNormal},
		{"log_routes_normal", false, api.LogtypeLog, logRouteNormal},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := routeLogByType(tt.red, tt.logType); got != tt.want {
				t.Errorf("routeLogByType(%v, %q) = %q, want %q", tt.red, tt.logType, got, tt.want)
			}
		})
	}
}
