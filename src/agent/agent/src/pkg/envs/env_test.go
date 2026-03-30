package envs

import (
	"testing"
)

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

func TestFetchEnvAndCheck(t *testing.T) {
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
