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

func TestGEnvVarsT_GetSet(t *testing.T) {
	e := &GEnvVarsT{envs: make(map[string]string)}

	t.Run("get_missing_key", func(t *testing.T) {
		_, ok := e.Get("missing")
		if ok {
			t.Error("Get on missing key should return false")
		}
	})

	t.Run("set_and_get", func(t *testing.T) {
		e.SetEnvs(map[string]string{"KEY1": "val1", "KEY2": "val2"})

		v, ok := e.Get("KEY1")
		if !ok || v != "val1" {
			t.Errorf("Get(KEY1) = (%q, %v), want (val1, true)", v, ok)
		}

		v, ok = e.Get("KEY2")
		if !ok || v != "val2" {
			t.Errorf("Get(KEY2) = (%q, %v), want (val2, true)", v, ok)
		}
	})

	t.Run("overwrite", func(t *testing.T) {
		e.SetEnvs(map[string]string{"KEY1": "new_val"})

		v, ok := e.Get("KEY1")
		if !ok || v != "new_val" {
			t.Errorf("Get(KEY1) = (%q, %v), want (new_val, true)", v, ok)
		}

		// old key should be gone after SetEnvs replaces the map
		_, ok = e.Get("KEY2")
		if ok {
			t.Error("KEY2 should not exist after SetEnvs replaced the map")
		}
	})
}

func TestGEnvVarsT_Size(t *testing.T) {
	e := &GEnvVarsT{envs: make(map[string]string)}

	if e.Size() != 0 {
		t.Errorf("Size() = %d, want 0", e.Size())
	}

	e.SetEnvs(map[string]string{"A": "1", "B": "2", "C": "3"})
	if e.Size() != 3 {
		t.Errorf("Size() = %d, want 3", e.Size())
	}
}

func TestGEnvVarsT_GetAll(t *testing.T) {
	e := &GEnvVarsT{envs: make(map[string]string)}
	e.SetEnvs(map[string]string{"X": "10", "Y": "20"})

	all := e.GetAll()
	if len(all) != 2 {
		t.Fatalf("GetAll() len = %d, want 2", len(all))
	}
	if all["X"] != "10" || all["Y"] != "20" {
		t.Errorf("GetAll() = %v, want map[X:10 Y:20]", all)
	}
}

func TestGEnvVarsT_RangeDo(t *testing.T) {
	e := &GEnvVarsT{envs: make(map[string]string)}
	e.SetEnvs(map[string]string{"A": "1", "B": "2", "C": "3"})

	t.Run("collect_all", func(t *testing.T) {
		collected := make(map[string]string)
		e.RangeDo(func(k, v string) bool {
			collected[k] = v
			return true
		})
		if len(collected) != 3 {
			t.Errorf("collected %d items, want 3", len(collected))
		}
	})

	t.Run("early_stop", func(t *testing.T) {
		count := 0
		e.RangeDo(func(k, v string) bool {
			count++
			return false
		})
		if count != 1 {
			t.Errorf("early stop collected %d items, want 1", count)
		}
	})
}

func TestFetchEnvAndCheck2(t *testing.T) {
	old := GApiEnvVars
	GApiEnvVars = &GEnvVarsT{envs: make(map[string]string)}
	defer func() { GApiEnvVars = old }()

	GApiEnvVars.SetEnvs(map[string]string{"MY_KEY": "hello"})

	tests := []struct {
		name       string
		key        string
		checkValue string
		want       bool
	}{
		{"match", "MY_KEY", "hello", true},
		{"mismatch", "MY_KEY", "world", false},
		{"missing_check_empty", "NO_KEY", "", true},
		{"missing_check_nonempty", "NO_KEY", "something", false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := FetchEnvAndCheck(tt.key, tt.checkValue); got != tt.want {
				t.Errorf("FetchEnvAndCheck(%q, %q) = %v, want %v", tt.key, tt.checkValue, got, tt.want)
			}
		})
	}
}
