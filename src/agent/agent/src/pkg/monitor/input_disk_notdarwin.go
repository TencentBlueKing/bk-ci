//go:build !darwin
// +build !darwin

package monitor

// darwinAvailableCapacity is a no-op stub on non-darwin platforms.
func darwinAvailableCapacity(_ string) (uint64, bool) {
	return 0, false
}
