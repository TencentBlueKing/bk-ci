//go:build darwin && !cgo
// +build darwin,!cgo

package diskutil

// AvailableCapacity is a no-op stub when CGO is disabled.
// The caller should fall back to the statfs / gopsutil value.
func AvailableCapacity(_ string) (uint64, bool) {
	return 0, false
}
