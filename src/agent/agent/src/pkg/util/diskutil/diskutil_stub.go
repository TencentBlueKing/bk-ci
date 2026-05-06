//go:build !darwin
// +build !darwin

package diskutil

// AvailableCapacity is a no-op stub on non-darwin platforms.
func AvailableCapacity(_ string) (uint64, bool) {
	return 0, false
}
