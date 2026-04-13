package constant

import "testing"

func TestConstantValues(t *testing.T) {
	if DaemonExitCode != 88 {
		t.Errorf("DaemonExitCode = %d, want 88", DaemonExitCode)
	}

	if WinCommandNewConsole != 0x00000010 {
		t.Errorf("WinCommandNewConsole = 0x%x, want 0x10", WinCommandNewConsole)
	}

	if CommonFileModePerm != 0644 {
		t.Errorf("CommonFileModePerm = %o, want 644", CommonFileModePerm)
	}
}

func TestEnvVarNames(t *testing.T) {
	envVars := map[string]string{
		"DevopsAgentEnableNewConsole": DevopsAgentEnableNewConsole,
		"DevopsAgentEnableExitGroup":  DevopsAgentEnableExitGroup,
		"DevopsAgentDockerCapAdd":     DevopsAgentDockerCapAdd,
		"DevopsAgentTimeoutExitTime":  DevopsAgentTimeoutExitTime,
		"DevopsAgentEnableMCP":        DevopsAgentEnableMCP,
		"DevopsAgentNoInheritHandles": DevopsAgentNoInheritHandles,
	}

	for name, val := range envVars {
		if val == "" {
			t.Errorf("%s should not be empty", name)
		}
	}
}
