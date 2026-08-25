//go:build darwin
// +build darwin

package agentcli

import (
	"testing"
)

func TestParseLaunchdList(t *testing.T) {
	tests := []struct {
		name        string
		output      string
		wantLoaded  bool
		wantRunning bool
		wantPID     int
		wantExit    int
		wantHasExit bool
	}{
		{
			name: "running_with_pid",
			output: `{
	"LimitLoadToSessionType" = "Background";
	"Label" = "devops_agent_abc123";
	"PID" = 1234;
	"LastExitStatus" = 0;
};`,
			wantLoaded:  true,
			wantRunning: true,
			wantPID:     1234,
			wantExit:    0,
			wantHasExit: true,
		},
		{
			name: "loaded_not_running",
			output: `{
	"LimitLoadToSessionType" = "Background";
	"Label" = "devops_agent_abc123";
	"LastExitStatus" = 256;
};`,
			wantLoaded:  true,
			wantRunning: false,
			wantPID:     0,
			wantExit:    256,
			wantHasExit: true,
		},
		{
			name: "loaded_no_exit_status",
			output: `{
	"Label" = "devops_agent_abc123";
};`,
			wantLoaded:  true,
			wantRunning: false,
			wantPID:     0,
			wantHasExit: false,
		},
		{
			name:        "empty_output",
			output:      "",
			wantLoaded:  true,
			wantRunning: false,
			wantPID:     0,
			wantHasExit: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := parseLaunchdList(tt.output)
			if got.loaded != tt.wantLoaded {
				t.Errorf("loaded = %v, want %v", got.loaded, tt.wantLoaded)
			}
			if got.running != tt.wantRunning {
				t.Errorf("running = %v, want %v", got.running, tt.wantRunning)
			}
			if got.pid != tt.wantPID {
				t.Errorf("pid = %d, want %d", got.pid, tt.wantPID)
			}
			if got.hasExit != tt.wantHasExit {
				t.Errorf("hasExit = %v, want %v", got.hasExit, tt.wantHasExit)
			}
			if got.hasExit && got.lastExit != tt.wantExit {
				t.Errorf("lastExit = %d, want %d", got.lastExit, tt.wantExit)
			}
		})
	}
}

func TestFormatLaunchdStatus(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	tests := []struct {
		name        string
		status      launchdStatus
		plistExists bool
		wantContain string
	}{
		{
			name:        "no_plist",
			status:      launchdStatus{},
			plistExists: false,
			wantContain: "not registered",
		},
		{
			name:        "plist_not_loaded",
			status:      launchdStatus{loaded: false},
			plistExists: true,
			wantContain: "not loaded",
		},
		{
			name:        "loaded_running",
			status:      launchdStatus{loaded: true, running: true, pid: 5678},
			plistExists: true,
			wantContain: "running (PID 5678)",
		},
		{
			name:        "loaded_not_running_with_exit",
			status:      launchdStatus{loaded: true, running: false, hasExit: true, lastExit: 1},
			plistExists: true,
			wantContain: "not running",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := formatLaunchdStatus(tt.status, tt.plistExists)
			if len(tt.wantContain) > 0 {
				found := false
				for _, s := range []string{tt.wantContain} {
					if contains(got, s) {
						found = true
					}
				}
				if !found {
					t.Errorf("formatLaunchdStatus() = %q, want to contain %q", got, tt.wantContain)
				}
			}
		})
	}
}

func contains(s, substr string) bool {
	return len(s) >= len(substr) && (s == substr || len(substr) == 0 ||
		(len(s) > 0 && len(substr) > 0 && searchSubstring(s, substr)))
}

func searchSubstring(s, sub string) bool {
	for i := 0; i <= len(s)-len(sub); i++ {
		if s[i:i+len(sub)] == sub {
			return true
		}
	}
	return false
}
