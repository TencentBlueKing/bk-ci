//go:build !windows
// +build !windows

package monitor

func normalizeDiskPathTag(path string) string {
	return path
}
