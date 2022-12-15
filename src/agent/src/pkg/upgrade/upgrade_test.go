package upgrade

import (
	"reflect"
	"testing"
)

func Test_trimJdkVersionList(t *testing.T) {
	want := []string{
		"openjdk version \"1.8.0_352\"",
		"OpenJDK Runtime Environment (Tencent Kona 8.0.12) (build 1.8.0_352-b1)",
		"OpenJDK 64-Bit Server VM (Tencent Kona 8.0.12) (build 25.352-b1, mixed mode)",
	}
	jdkVersionString := "OpenJDK 64-Bit Server VM warning: Insufficient space for shared memory file:\n" +
		"   9507\n" +
		"Try using the -Djava.io.tmpdir= option to select an alternate temp location.\n\n" +
		want[0] + "\n" +
		want[1] + "\n" +
		want[2]

	tests := trimJdkVersionList(jdkVersionString)

	t.Run("length", func(t *testing.T) {
		if len(tests) != len(want) {
			t.Fatalf("Fail: len(%d), want(%d)", len(tests), len(want))
		}
	})

	for i := 0; i < len(tests); i++ {
		t.Run(tests[i], func(t *testing.T) {
			if !reflect.DeepEqual(tests[i], want[i]) {
				t.Fatalf("Fail: %v want %v", tests[i], want[i])
			}
		})
	}

	jdkVersionString = want[0] + "\n" + want[1] + "\n" + want[2]

	tests = trimJdkVersionList(jdkVersionString)

	t.Run("3length", func(t *testing.T) {
		if len(tests) != len(want) {
			t.Fatalf("Fail: len(%d), want(%d)", len(tests), len(want))
		}
	})

	for i := 0; i < len(tests); i++ {
		t.Run(tests[i], func(t *testing.T) {
			if !reflect.DeepEqual(tests[i], want[i]) {
				t.Fatalf("Fail: %v want %v", tests[i], want[i])
			}
		})
	}

}
