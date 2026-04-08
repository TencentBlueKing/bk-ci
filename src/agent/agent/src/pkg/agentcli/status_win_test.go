//go:build windows
// +build windows

package agentcli

import (
	"testing"
)

func TestParseServiceStartType(t *testing.T) {
	tests := []struct {
		name   string
		output string
		want   string
	}{
		{
			name: "auto_start",
			output: `[SC] QueryServiceConfig SUCCESS

SERVICE_NAME: devops_agent_test
        TYPE               : 10  WIN32_OWN_PROCESS
        START_TYPE         : 2   AUTO_START
        ERROR_CONTROL      : 1   NORMAL
        BINARY_PATH_NAME   : C:\agent\devopsDaemon.exe
        LOAD_ORDER_GROUP   :
        TAG                : 0
        DISPLAY_NAME       : devops_agent_test
        DEPENDENCIES       :
        SERVICE_START_NAME : LocalSystem`,
			want: "auto ✓",
		},
		{
			name: "demand_start",
			output: `[SC] QueryServiceConfig SUCCESS

SERVICE_NAME: devops_agent_test
        TYPE               : 10  WIN32_OWN_PROCESS
        START_TYPE         : 3   DEMAND_START
        ERROR_CONTROL      : 1   NORMAL`,
			want: "manual",
		},
		{
			name: "disabled",
			output: `[SC] QueryServiceConfig SUCCESS

SERVICE_NAME: devops_agent_test
        START_TYPE         : 4   DISABLED`,
			want: "disabled ✗",
		},
		{
			name:   "empty_output",
			output: "",
			want:   "unknown",
		},
		{
			name:   "no_start_type_line",
			output: "SERVICE_NAME: test\nTYPE: 10\n",
			want:   "unknown",
		},
	}

	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := parseServiceStartType(tt.output)
			if got != tt.want {
				t.Errorf("parseServiceStartType() = %q, want %q", got, tt.want)
			}
		})
	}
}
