package mssql

import (
	"context"
	"testing"
)

func TestBadOpen(t *testing.T) {
	drv := driverWithProcess(t)
	_, err := drv.open(context.Background(), "port=bad")
	if err == nil {
		t.Fail()
	}
}

func TestIsProc(t *testing.T) {
	list := []struct {
		s  string
		is bool
	}{
		{"proc", true},
		{"select 1;", false},
		{"select 1", false},
		{"[proc 1]", true},
		{"[proc\n1]", false},
		{"schema.name", true},
		{"[schema].[name]", true},
		{"schema.[name]", true},
		{"[schema].name", true},
		{"schema.[proc name]", true},
	}

	for _, item := range list {
		got := isProc(item.s)
		if got != item.is {
			t.Errorf("for %q, got %t want %t", item.s, got, item.is)
		}
	}
}
