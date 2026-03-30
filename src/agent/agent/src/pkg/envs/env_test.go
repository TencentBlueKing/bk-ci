package envs

import (
	"os"
	"testing"
)

func init() {
	Init()
}

func TestFetchEnvAndCheck(t *testing.T) {
	tests := []struct {
		name       string
		envKey     string
		envVal     string
		setEnv     bool
		checkValue string
		want       bool
	}{
		{"missing_check_empty", "TEST_BKCI_MISSING_KEY", "", false, "", true},
		{"missing_check_nonempty", "TEST_BKCI_MISSING_KEY2", "", false, "true", false},
		{"set_true_check_true", "TEST_BKCI_SET_1", "true", true, "true", true},
		{"set_false_check_true", "TEST_BKCI_SET_2", "false", true, "true", false},
		{"set_empty_check_empty", "TEST_BKCI_SET_3", "", true, "", true},
		{"exact_match", "TEST_BKCI_SET_4", "hello", true, "hello", true},
		{"case_sensitive", "TEST_BKCI_SET_5", "True", true, "true", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if tt.setEnv {
				os.Setenv(tt.envKey, tt.envVal)
				defer os.Unsetenv(tt.envKey)
			} else {
				os.Unsetenv(tt.envKey)
			}

			got := FetchEnvAndCheck(tt.envKey, tt.checkValue)
			if got != tt.want {
				t.Errorf("FetchEnvAndCheck(%q, %q) = %v, want %v", tt.envKey, tt.checkValue, got, tt.want)
			}
		})
	}
}

func TestFetchEnv(t *testing.T) {
	t.Run("from_os_env", func(t *testing.T) {
		os.Setenv("TEST_BKCI_FETCH", "myval")
		defer os.Unsetenv("TEST_BKCI_FETCH")

		val, ok := FetchEnv("TEST_BKCI_FETCH")
		if !ok {
			t.Error("expected to find env var")
		}
		if val != "myval" {
			t.Errorf("val = %q, want %q", val, "myval")
		}
	})

	t.Run("from_api_vars_priority", func(t *testing.T) {
		os.Setenv("TEST_BKCI_PRIO", "os_value")
		defer os.Unsetenv("TEST_BKCI_PRIO")

		GApiEnvVars.SetEnvs(map[string]string{"TEST_BKCI_PRIO": "api_value"})
		defer GApiEnvVars.SetEnvs(map[string]string{})

		val, ok := FetchEnv("TEST_BKCI_PRIO")
		if !ok {
			t.Error("expected to find env var")
		}
		if val != "api_value" {
			t.Errorf("API vars should have priority, got %q", val)
		}
	})

	t.Run("not_found", func(t *testing.T) {
		os.Unsetenv("TEST_BKCI_NOTEXIST")
		_, ok := FetchEnv("TEST_BKCI_NOTEXIST")
		if ok {
			t.Error("should not find nonexistent env var")
		}
	})
}

func TestGEnvVarsT(t *testing.T) {
	e := &GEnvVarsT{envs: make(map[string]string)}

	t.Run("empty_size", func(t *testing.T) {
		if e.Size() != 0 {
			t.Errorf("Size() = %d, want 0", e.Size())
		}
	})

	t.Run("set_and_get", func(t *testing.T) {
		e.SetEnvs(map[string]string{"K1": "V1", "K2": "V2"})
		v, ok := e.Get("K1")
		if !ok || v != "V1" {
			t.Errorf("Get(K1) = (%q, %v), want (V1, true)", v, ok)
		}
		if e.Size() != 2 {
			t.Errorf("Size() = %d, want 2", e.Size())
		}
	})

	t.Run("get_all", func(t *testing.T) {
		e.SetEnvs(map[string]string{"A": "1"})
		all := e.GetAll()
		if len(all) != 1 || all["A"] != "1" {
			t.Errorf("GetAll() = %v, want map[A:1]", all)
		}
	})

	t.Run("range_do", func(t *testing.T) {
		e.SetEnvs(map[string]string{"X": "1", "Y": "2"})
		count := 0
		e.RangeDo(func(k, v string) bool {
			count++
			return true
		})
		if count != 2 {
			t.Errorf("RangeDo visited %d items, want 2", count)
		}
	})

	t.Run("range_do_early_stop", func(t *testing.T) {
		e.SetEnvs(map[string]string{"A": "1", "B": "2", "C": "3"})
		count := 0
		e.RangeDo(func(k, v string) bool {
			count++
			return false // stop after first
		})
		if count != 1 {
			t.Errorf("RangeDo should stop after 1 iteration, visited %d", count)
		}
	})
}
