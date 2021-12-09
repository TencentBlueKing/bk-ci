package sharedlibrary

// #cgo LDFLAGS: -L. -lhandler
//
// #include <handler.h>
import (
	"C"
)
