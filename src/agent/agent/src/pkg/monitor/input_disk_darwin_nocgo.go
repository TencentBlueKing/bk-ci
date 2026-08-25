//go:build darwin && !cgo
// +build darwin,!cgo

package monitor

// darwinAvailableCapacity is a no-op stub when CGO is disabled.
// The caller falls back to the gopsutil value.
func darwinAvailableCapacity(_ string) (uint64, bool) {
	return 0, false
}
