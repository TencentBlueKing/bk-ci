/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package analyser

import (
	"testing"
)

func TestIndex(t *testing.T) {
	cache := NewFileCache()

	a := cache.GetRelativeFile("hello-world")
	b := cache.GetRelativeFile("hello-world")
	c := cache.GetRelativeFile("foo-bar")

	if idx := a.Index(); idx != 0 {
		t.Errorf("cache index error: index = %d, want 0", idx)
	}

	if idx := b.Index(); idx != 0 {
		t.Errorf("cache index error: index = %d, want 0", idx)
	}

	if idx := c.Index(); idx != 1 {
		t.Errorf("cache index error: index = %d, want 1", idx)
	}
}

func TestString(t *testing.T) {
	cache := NewFileCache()

	a := cache.GetRelativeFile("hello-world")
	b := cache.GetRelativeFile("hello-world")
	c := cache.GetRelativeFile("foo-bar")

	if idx := a.String(); idx != "hello-world" {
		t.Errorf("cache index error: index = %s, want hello-world", idx)
	}

	if idx := b.String(); idx != "hello-world" {
		t.Errorf("cache index error: index = %s, want hello-world", idx)
	}

	if idx := c.String(); idx != "foo-bar" {
		t.Errorf("cache index error: index = %s, want foo-bar", idx)
	}
}
