package agentcli

import (
	"os"
	"path/filepath"
	"testing"
)

func TestMsg(t *testing.T) {
	tests := []struct {
		name    string
		chinese bool
		en, zh  string
		want    string
	}{
		{"english", false, "hello", "你好", "hello"},
		{"chinese", true, "hello", "你好", "你好"},
		{"empty_en", false, "", "空", ""},
		{"empty_zh", true, "empty", "", ""},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			old := useChinese
			useChinese = tt.chinese
			defer func() { useChinese = old }()

			if got := msg(tt.en, tt.zh); got != tt.want {
				t.Errorf("msg(%q, %q) = %q, want %q", tt.en, tt.zh, got, tt.want)
			}
		})
	}
}

func TestMsgf(t *testing.T) {
	tests := []struct {
		name    string
		chinese bool
		en, zh  string
		args    []interface{}
		want    string
	}{
		{"english_format", false, "hello %s", "你好 %s", []interface{}{"world"}, "hello world"},
		{"chinese_format", true, "hello %s", "你好 %s", []interface{}{"world"}, "你好 world"},
		{"multiple_args", false, "%s has %d items", "%s 有 %d 项", []interface{}{"list", 3}, "list has 3 items"},
		{"int_format", true, "PID %d", "进程号 %d", []interface{}{1234}, "进程号 1234"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			old := useChinese
			useChinese = tt.chinese
			defer func() { useChinese = old }()

			if got := msgf(tt.en, tt.zh, tt.args...); got != tt.want {
				t.Errorf("msgf() = %q, want %q", got, tt.want)
			}
		})
	}
}

func TestInitLang_EnvVars(t *testing.T) {
	origLang := os.Getenv("LANG")
	origLcAll := os.Getenv("LC_ALL")
	origLanguage := os.Getenv("LANGUAGE")
	defer func() {
		setOrUnset("LANG", origLang)
		setOrUnset("LC_ALL", origLcAll)
		setOrUnset("LANGUAGE", origLanguage)
	}()

	dir := t.TempDir()

	tests := []struct {
		name   string
		envKey string
		envVal string
		wantZh bool
	}{
		{"LANG_zh_CN", "LANG", "zh_CN.UTF-8", true},
		{"LANG_en_US", "LANG", "en_US.UTF-8", false},
		{"LC_ALL_zh_TW", "LC_ALL", "zh_TW", true},
		{"LANGUAGE_zh", "LANGUAGE", "zh", true},
		{"LANG_ja_JP", "LANG", "ja_JP.UTF-8", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			os.Unsetenv("LANG")
			os.Unsetenv("LC_ALL")
			os.Unsetenv("LANGUAGE")

			os.Setenv(tt.envKey, tt.envVal)

			initLang(dir)
			if useChinese != tt.wantZh {
				t.Errorf("initLang with %s=%q: useChinese = %v, want %v",
					tt.envKey, tt.envVal, useChinese, tt.wantZh)
			}
		})
	}
}

func TestInitLang_PropertiesFile(t *testing.T) {
	origLang := os.Getenv("LANG")
	origLcAll := os.Getenv("LC_ALL")
	origLanguage := os.Getenv("LANGUAGE")
	defer func() {
		setOrUnset("LANG", origLang)
		setOrUnset("LC_ALL", origLcAll)
		setOrUnset("LANGUAGE", origLanguage)
	}()

	os.Unsetenv("LANG")
	os.Unsetenv("LC_ALL")
	os.Unsetenv("LANGUAGE")

	dir := t.TempDir()
	os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte("devops.language=zh_CN\n"), 0644)

	initLang(dir)
	if !useChinese {
		t.Error("initLang with devops.language=zh_CN should set useChinese = true")
	}
}

func TestInitLang_FallbackToEmpty(t *testing.T) {
	origLang := os.Getenv("LANG")
	origLcAll := os.Getenv("LC_ALL")
	origLanguage := os.Getenv("LANGUAGE")
	defer func() {
		setOrUnset("LANG", origLang)
		setOrUnset("LC_ALL", origLcAll)
		setOrUnset("LANGUAGE", origLanguage)
	}()

	os.Unsetenv("LANG")
	os.Unsetenv("LC_ALL")
	os.Unsetenv("LANGUAGE")

	dir := t.TempDir()

	initLang(dir)
	// No env vars, no properties, detectPlatformLang returns "" on non-windows
	// useChinese should be false
	if useChinese {
		t.Error("initLang with no language hints should set useChinese = false")
	}
}

func TestTryReadLang(t *testing.T) {
	t.Run("no_file", func(t *testing.T) {
		dir := t.TempDir()
		if got := tryReadLang(dir); got != "" {
			t.Errorf("tryReadLang(no file) = %q, want empty", got)
		}
	})

	t.Run("with_language", func(t *testing.T) {
		dir := t.TempDir()
		os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte("devops.language=en_US\n"), 0644)
		if got := tryReadLang(dir); got != "en_US" {
			t.Errorf("tryReadLang() = %q, want %q", got, "en_US")
		}
	})

	t.Run("no_language_key", func(t *testing.T) {
		dir := t.TempDir()
		os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte("devops.agent.id=x\n"), 0644)
		if got := tryReadLang(dir); got != "" {
			t.Errorf("tryReadLang(no lang key) = %q, want empty", got)
		}
	})
}

func setOrUnset(key, val string) {
	if val != "" {
		os.Setenv(key, val)
	} else {
		os.Unsetenv(key)
	}
}
