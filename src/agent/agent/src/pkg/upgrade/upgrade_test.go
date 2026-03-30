package upgrade

import "testing"

func TestUpgradeItems_NoChange(t *testing.T) {
	tests := []struct {
		name string
		item upgradeItems
		want bool
	}{
		{"all_false", upgradeItems{}, true},
		{"agent_true", upgradeItems{Agent: true}, false},
		{"worker_true", upgradeItems{Worker: true}, false},
		{"jdk_true", upgradeItems{Jdk: true}, false},
		{"docker_init_true", upgradeItems{DockerInitFile: true}, false},
		{"all_true", upgradeItems{Agent: true, Worker: true, Jdk: true, DockerInitFile: true}, false},
		{"mixed", upgradeItems{Agent: false, Worker: true, Jdk: false, DockerInitFile: false}, false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.item.NoChange(); got != tt.want {
				t.Errorf("NoChange() = %v, want %v", got, tt.want)
			}
		})
	}
}
