package monitor

import "testing"

func TestShouldSkipNetInterface(t *testing.T) {
	cases := []struct {
		name string
		skip bool
	}{
		// Linux
		{"eth0", false},
		{"eth1", false},
		{"ens33", false},
		{"enp0s3", false},
		{"em1", false},
		{"lo", true},
		{"docker0", true},
		{"br-d89c829c1e0b", true},
		{"veth123abc", true},
		{"cali12345", true},
		{"flannel.1", true},
		{"weave", true},
		{"cni0", true},
		{"virbr0", true},
		// macOS
		{"en0", false},
		{"en1", false},
		{"lo0", true},
		{"gif0", true},
		{"stf0", true},
		{"anpi0", true},
		{"ap1", true},
		{"awdl0", true},
		{"llw0", true},
		{"bridge0", true},
		{"utun0", true},
		{"utun7", true},
		// Windows（FriendlyName 大小写不敏感）
		{"Loopback Pseudo-Interface 1", true},
		{"loopback pseudo-interface 1", true},
		{"vEthernet (WSL (Hyper-V firewall))", true},
		{"vEthernet (Default Switch)", true},
		{"isatap.{GUID}", true},
		{"Teredo Tunneling Pseudo-Interface", true},
		{"Intel[R] Ethernet Connection [17] I219-LM", false},
		{"以太网 3", false}, // 非 ASCII 前缀不会被误伤
		{"以太网 5", false},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			if got := shouldSkipNetInterface(tc.name); got != tc.skip {
				t.Errorf("shouldSkipNetInterface(%q) = %v, want %v", tc.name, got, tc.skip)
			}
		})
	}
}
