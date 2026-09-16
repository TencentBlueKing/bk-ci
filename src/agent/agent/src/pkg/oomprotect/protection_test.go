package oomprotect

import (
	"os"
	"path/filepath"
	"testing"
)

func TestStartupSettingPrecedenceAndRemoval(t *testing.T) {
	dir := t.TempDir()
	t.Setenv(EnvKey, "true")
	if enabled, err := Requested(dir); err != nil || !enabled {
		t.Fatalf("local setting: %v %v", enabled, err)
	}
	if _, err := PersistServerEnv(dir, map[string]string{EnvKey: "false", "SECRET": "not persisted"}); err != nil {
		t.Fatal(err)
	}
	if enabled, err := Requested(dir); err != nil || enabled {
		t.Fatalf("server precedence: %v %v", enabled, err)
	}
	data, _ := os.ReadFile(filepath.Join(dir, ".oom-protection-env.json"))
	if string(data) != "{\"DEVOPS_AGENT_OOM_PROTECT\":\"false\"}" {
		t.Fatalf("unexpected persisted keys: %s", data)
	}
	if changed, err := PersistServerEnv(dir, map[string]string{EnvKey: "false"}); err != nil || changed {
		t.Fatalf("unchanged config rewritten: %v %v", changed, err)
	}
	if _, err := PersistServerEnv(dir, map[string]string{}); err != nil {
		t.Fatal(err)
	}
	if enabled, err := Requested(dir); err != nil || !enabled {
		t.Fatalf("removed override must restore local setting: %v %v", enabled, err)
	}
}

func TestInvalidSettingFailsClosed(t *testing.T) {
	dir := t.TempDir()
	t.Setenv(EnvKey, "tru")
	if _, err := Requested(dir); err == nil {
		t.Fatal("invalid value accepted")
	}
	if err := os.WriteFile(filepath.Join(dir, ".oom-protection-env.json"), []byte("{"), 0600); err != nil {
		t.Fatal(err)
	}
	t.Setenv(EnvKey, "false")
	if _, err := Requested(dir); err == nil {
		t.Fatal("corrupt server config ignored")
	}
}

func TestPauseStopsAdmission(t *testing.T) {
	state.Lock()
	oldEnabled, oldErr := state.enabled, state.err
	state.enabled, state.err = true, nil
	state.Unlock()
	defer func() {
		state.Lock()
		state.enabled, state.err = oldEnabled, oldErr
		state.Unlock()
		pauseUntil.Store(0)
	}()
	Pause()
	if err := Ready(); err == nil {
		t.Fatal("resource failure did not stop admission")
	}
	pauseUntil.Store(0)
	if err := Ready(); err != nil {
		t.Fatal(err)
	}
}
