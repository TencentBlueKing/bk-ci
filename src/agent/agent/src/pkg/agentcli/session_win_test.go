//go:build windows
// +build windows

package agentcli

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

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

func TestHandleInstall_ModeDispatch(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	dir := t.TempDir()

	tests := []struct {
		name    string
		args    []string
		wantErr bool
	}{
		{"default_mode", []string{}, true},
		{"service_mode", []string{"service"}, true},
		{"session_mode", []string{"session"}, true},
		{"task_mode", []string{"task"}, true},
		{"invalid_mode", []string{"invalid"}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := handleInstall(dir, tt.args)
			if tt.wantErr && err == nil {
				t.Error("expected error with temp dir (no .agent.properties)")
			}
			if tt.name == "invalid_mode" && err != nil {
				if got := err.Error(); got == "" {
					t.Error("invalid mode should return descriptive error")
				}
			}
		})
	}
}

func TestHandleInstall_SessionValidation(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	dir := t.TempDir()
	os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte("devops.agent.id=test1\n"), 0644)

	t.Run("autologon_no_args", func(t *testing.T) {
		err := handleInstall(dir, []string{"session", "--auto-logon"})
		if err == nil {
			t.Error("expected error: --auto-logon requires USER and PASSWORD")
		}
	})

	t.Run("autologon_only_user", func(t *testing.T) {
		err := handleInstall(dir, []string{"session", "--auto-logon", "admin"})
		if err == nil {
			t.Error("expected error: --auto-logon requires USER and PASSWORD")
		}
	})
}

func TestConfigureSessionSummaryLines(t *testing.T) {
	old := useChinese
	defer func() { useChinese = old }()

	t.Run("english_no_autologon", func(t *testing.T) {
		useChinese = false
		lines := configureSessionSummaryLines("", false)
		if len(lines) != 3 {
			t.Fatalf("len(lines) = %d, want 3", len(lines))
		}
		if !strings.Contains(lines[0], "current session NOW") {
			t.Errorf("line[0] = %q", lines[0])
		}
		if !strings.Contains(lines[1], "waits until") {
			t.Errorf("line[1] = %q, want 'waits until'", lines[1])
		}
	})

	t.Run("english_auto_logon", func(t *testing.T) {
		useChinese = false
		lines := configureSessionSummaryLines("builduser", true)
		if len(lines) < 3 {
			t.Fatalf("len(lines) = %d, want >= 3", len(lines))
		}
		if !strings.Contains(lines[1], "auto-logs in") {
			t.Errorf("line[1] = %q", lines[1])
		}
		// Verify PIN warning is included
		found := false
		for _, l := range lines {
			if strings.Contains(l, "PIN") {
				found = true
				break
			}
		}
		if !found {
			t.Error("auto-logon summary should include PIN warning")
		}
	})

	t.Run("chinese_auto_logon", func(t *testing.T) {
		useChinese = true
		lines := configureSessionSummaryLines("构建用户", true)
		if len(lines) < 3 {
			t.Fatalf("len(lines) = %d, want >= 3", len(lines))
		}
		if !strings.Contains(lines[0], "当前桌面会话") {
			t.Errorf("line[0] = %q", lines[0])
		}
		if !strings.Contains(lines[1], "自动登录") {
			t.Errorf("line[1] = %q", lines[1])
		}
		// Verify PIN warning is included in Chinese
		found := false
		for _, l := range lines {
			if strings.Contains(l, "PIN") {
				found = true
				break
			}
		}
		if !found {
			t.Error("auto-logon summary should include PIN warning in Chinese")
		}
	})
}
