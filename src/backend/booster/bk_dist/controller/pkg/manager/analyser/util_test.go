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

func TestParseMacroFunctionArgs(t *testing.T) {
	r, pos := parseMacroFunctionArgs("hello_world", 11)
	if r != nil || pos != 11 {
		t.Errorf("parse macro func-args failed: r(%v) pos(%d), want r(nil) pos(11)", r, pos)
	}

	r, pos = parseMacroFunctionArgs("hello_world(alpha, bravo, charlie) something bla bla", 11)
	if len(r) != 3 || r[0] != "alpha" || r[1] != "bravo" || r[2] != "charlie" || pos != 34 {
		t.Errorf("parse macro func-args failed: r(%v) pos(%d), want r([alpha, bravo, charlie]) pos(34)", r, pos)
	}

	r, pos = parseMacroFunctionArgs("hello_world(a, b(c(d), e), f)", 11)
	if len(r) != 3 || r[0] != "a" || r[1] != "b(c(d), e)" || r[2] != "f" || pos != 29 {
		t.Errorf("parse macro func-args failed: r(%v) pos(%d), want r([a, b(c(d), e), f]) pos(29)", r, pos)
	}

	r, pos = parseMacroFunctionArgs("hello_world(a, \"b(c(d), e)\")", 11)
	if len(r) != 2 || r[0] != "a" || r[1] != "\"b(c(d), e)\"" || pos != 28 {
		t.Errorf("parse macro func-args failed: r(%v) pos(%d), want r([a, \"b(c(d), e)\"]) pos(28)", r, pos)
	}

	r, pos = parseMacroFunctionArgs("hello_world(a, (), (f)", 11)
	if r != nil || pos != 11 {
		t.Errorf("parse macro func-args failed: r(%v) pos(%d), want r(nil) pos(11)", r, pos)
	}
}

func TestGetFirstWorld(t *testing.T) {
	text := "BOOST_USER_CONFIG"
	r := getFirstWorld(text)
	s := ReMacroSymbol.FindStringIndex(text)
	if len(r) != len(s) || (len(r) > 0 && r[0] != s[0] || r[1] != s[1]) {
		t.Errorf("get first ascii world failed: base %s, get(%v), want(%v)", text, r, s)
	}

	text = "<boost/config/user.hpp>"
	r = getFirstWorld(text)
	s = ReMacroSymbol.FindStringIndex(text)
	if len(r) != len(s) || (len(r) > 0 && r[0] != s[0] || r[1] != s[1]) {
		t.Errorf("get first ascii world failed: base %s, get(%v), want(%v)", text, r, s)
	}
}
