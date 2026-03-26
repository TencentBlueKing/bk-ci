//go:build windows
// +build windows

package agentcli

import "testing"

func TestSplitUserDomain(t *testing.T) {
	tests := []struct {
		account    string
		wantUser   string
		wantDomain string
	}{
		{"user", "user", "."},
		{"DOMAIN\\user", "user", "DOMAIN"},
		{"user@domain.com", "user", "domain.com"},
		{"CORP\\admin", "admin", "CORP"},
		{"me@company.local", "me", "company.local"},
		{".\\localuser", "localuser", "."},
	}
	for _, tt := range tests {
		t.Run(tt.account, func(t *testing.T) {
			user, domain := splitUserDomain(tt.account)
			if user != tt.wantUser || domain != tt.wantDomain {
				t.Errorf("splitUserDomain(%q) = (%q, %q), want (%q, %q)",
					tt.account, user, domain, tt.wantUser, tt.wantDomain)
			}
		})
	}
}

func TestSplitUserDomain_BackslashPriority(t *testing.T) {
	// Backslash should take priority over @
	user, domain := splitUserDomain("DOMAIN\\user@mail.com")
	if user != "user@mail.com" {
		t.Errorf("user = %q, want %q", user, "user@mail.com")
	}
	if domain != "DOMAIN" {
		t.Errorf("domain = %q, want %q", domain, "DOMAIN")
	}
}

func TestReadInstallTypeFile(t *testing.T) {
	t.Run("no_file", func(t *testing.T) {
		old := useChinese
		useChinese = false
		defer func() { useChinese = old }()

		dir := t.TempDir()
		got := readInstallTypeFile(dir)
		if got == "" {
			t.Error("readInstallTypeFile should return default when file missing")
		}
	})
}
