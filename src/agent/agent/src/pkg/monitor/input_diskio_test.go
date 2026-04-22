//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v3/disk"
)

func TestDiskIO_Name(t *testing.T) {
	if n := NewDiskIO().Name(); n != MeasurementDiskIO {
		t.Errorf("Name() = %q", n)
	}
}

func TestDiskIO_Gather_AllDevices(t *testing.T) {
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			return map[string]disk.IOCountersStat{
				"sda": {Name: "sda", ReadCount: 10, WriteCount: 5, ReadBytes: 1024, WriteBytes: 2048},
				"sdb": {Name: "sdb", ReadCount: 1},
			}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := d.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 2 {
		t.Errorf("want 2 disks, got %d", len(metrics))
	}
	for _, m := range metrics {
		if m.Tags[TagName] == "" {
			t.Errorf("name tag missing: %+v", m.Tags)
		}
		for _, f := range []string{
			FieldReads, FieldWrites,
			FieldReadBytes, FieldWriteBytes,
		} {
			if _, ok := m.Fields[f]; !ok {
				t.Errorf("name %s missing field %s", m.Tags[TagName], f)
			}
		}
	}
}

func TestDiskIO_Gather_ErrorPropagates(t *testing.T) {
	sentinel := errors.New("boom")
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) { return nil, sentinel },
		nowFn:        time.Now,
	}
	_, err := d.Gather()
	if err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v", err)
	}
}

func TestDiskIO_Gather_EmptyMap(t *testing.T) {
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			return map[string]disk.IOCountersStat{}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := d.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 0 {
		t.Errorf("want 0 metrics, got %d", len(metrics))
	}
}
