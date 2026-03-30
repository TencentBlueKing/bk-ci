package config

import (
	"testing"
)

func TestGetGateWay(t *testing.T) {
	oldConfig := GAgentConfig
	defer func() { GAgentConfig = oldConfig }()

	GAgentConfig = &AgentConfig{}

	tests := []struct {
		name    string
		gateway string
		want    string
	}{
		{"with_http", "http://devops.example.com", "http://devops.example.com"},
		{"with_https", "https://devops.example.com", "https://devops.example.com"},
		{"without_scheme", "devops.example.com", "http://devops.example.com"},
		{"bare_ip", "10.0.0.1:8080", "http://10.0.0.1:8080"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			GAgentConfig.Gateway = tt.gateway
			got := GetGateWay()
			if got != tt.want {
				t.Errorf("GetGateWay() = %q, want %q", got, tt.want)
			}
		})
	}
}

func TestGetAuthHeaderMap(t *testing.T) {
	cfg := &AgentConfig{
		BuildType: "AGENT",
		ProjectId: "proj-123",
		AgentId:   "agent-456",
		SecretKey: "secret-789",
	}

	headers := cfg.GetAuthHeaderMap()

	expected := map[string]string{
		AuthHeaderBuildType: "AGENT",
		AuthHeaderProjectId: "proj-123",
		AuthHeaderAgentId:   "agent-456",
		AuthHeaderSecretKey: "secret-789",
	}

	for k, want := range expected {
		got, ok := headers[k]
		if !ok {
			t.Errorf("missing header %q", k)
		} else if got != want {
			t.Errorf("header[%q] = %q, want %q", k, got, want)
		}
	}

	if len(headers) != len(expected) {
		t.Errorf("header count = %d, want %d", len(headers), len(expected))
	}
}

func TestGetPersistedProxyEnvs(t *testing.T) {
	tests := []struct {
		name  string
		cfg   AgentConfig
		count int
	}{
		{"all_empty", AgentConfig{}, 0},
		{"http_only", AgentConfig{HTTPProxy: "http://proxy:8080"}, 1},
		{"all_set", AgentConfig{
			HTTPProxy:  "http://proxy:8080",
			HTTPSProxy: "https://proxy:8443",
			NOProxy:    "localhost,127.0.0.1",
		}, 3},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			envs := tt.cfg.GetPersistedProxyEnvs()
			if len(envs) != tt.count {
				t.Errorf("proxy env count = %d, want %d, envs=%v", len(envs), tt.count, envs)
			}
		})
	}
}

func TestSyncPersistedProxyEnvs(t *testing.T) {
	tests := []struct {
		name        string
		initial     AgentConfig
		envMap      map[string]string
		wantChanged bool
		wantHTTP    string
		wantHTTPS   string
		wantNO      string
	}{
		{
			name:        "set_new_proxy",
			initial:     AgentConfig{},
			envMap:      map[string]string{"HTTP_PROXY": "http://new:8080"},
			wantChanged: true,
			wantHTTP:    "http://new:8080",
		},
		{
			name:        "no_change",
			initial:     AgentConfig{HTTPProxy: "http://same:8080"},
			envMap:      map[string]string{"HTTP_PROXY": "http://same:8080"},
			wantChanged: false,
			wantHTTP:    "http://same:8080",
		},
		{
			name:        "clear_proxy",
			initial:     AgentConfig{HTTPProxy: "http://old:8080"},
			envMap:      map[string]string{},
			wantChanged: true,
			wantHTTP:    "",
		},
		{
			name:        "lowercase_keys",
			initial:     AgentConfig{},
			envMap:      map[string]string{"http_proxy": "http://lower:8080", "https_proxy": "https://lower:443"},
			wantChanged: true,
			wantHTTP:    "http://lower:8080",
			wantHTTPS:   "https://lower:443",
		},
		{
			name:        "trim_spaces",
			initial:     AgentConfig{},
			envMap:      map[string]string{"HTTP_PROXY": "  http://spaced:8080  "},
			wantChanged: true,
			wantHTTP:    "http://spaced:8080",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cfg := tt.initial
			changed := cfg.SyncPersistedProxyEnvs(tt.envMap)
			if changed != tt.wantChanged {
				t.Errorf("changed = %v, want %v", changed, tt.wantChanged)
			}
			if cfg.HTTPProxy != tt.wantHTTP {
				t.Errorf("HTTPProxy = %q, want %q", cfg.HTTPProxy, tt.wantHTTP)
			}
			if tt.wantHTTPS != "" && cfg.HTTPSProxy != tt.wantHTTPS {
				t.Errorf("HTTPSProxy = %q, want %q", cfg.HTTPSProxy, tt.wantHTTPS)
			}
			if tt.wantNO != "" && cfg.NOProxy != tt.wantNO {
				t.Errorf("NOProxy = %q, want %q", cfg.NOProxy, tt.wantNO)
			}
		})
	}
}
