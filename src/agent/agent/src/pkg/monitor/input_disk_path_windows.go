//go:build windows
// +build windows

package monitor

import "strings"

func normalizeWindowsDriveLetter(path string) string {
	if len(path) == 0 {
		return path
	}

	trimmed := strings.TrimRight(path, "\\\\/")
	trimmed = strings.TrimLeft(trimmed, "\\\\/")
	if len(trimmed) >= 2 && trimmed[1] == ':' {
		return strings.ToUpper(trimmed[:1]) + trimmed[1:]
	}
	return path
}

func normalizeDiskPathTag(path string) string {
	if drive := normalizeWindowsDriveLetter(path); drive != path {
		return "\\" + drive
	}
	if len(path) >= 2 && path[1] == ':' {
		return "\\" + path
	}
	return path
}
