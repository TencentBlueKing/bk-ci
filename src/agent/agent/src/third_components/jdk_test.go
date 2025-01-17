package third_components

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

	sep := "\r\n"
	jdkVersionString := "OpenJDK 64-Bit Server VM warning: Insufficient space for shared memory file:" + sep +
		"   9507" + sep +
		"Try using the -Djava.io.tmpdir= option to select an alternate temp location." + sep + sep +
		want[0] + sep +
		want[1] + sep +
		want[2] + sep +
		"Picked up _JAVA_OPTIONS: -Xmx8192m -Xms256m -Xss8m" + sep

	loopTest(t, "windows_un_normal", jdkVersionString, want)

	jdkVersionString = want[0] + sep + want[1] + sep + want[2] + sep

	loopTest(t, "windows_normal", jdkVersionString, want)

	sep = "\n"
	jdkVersionString = "OpenJDK 64-Bit Server VM warning: Insufficient space for shared memory file:" + sep +
		"   9507" + sep +
		"Try using the -Djava.io.tmpdir= option to select an alternate temp location." + sep + sep +
		want[0] + sep +
		want[1] + sep +
		want[2] + sep +
		"Picked up _JAVA_OPTIONS: -Xmx8192m -Xms256m -Xss8m" + sep

	loopTest(t, "linux_un_normal", jdkVersionString, want)

	jdkVersionString = want[0] + sep + want[1] + sep + want[2] + sep

	loopTest(t, "linux_normal", jdkVersionString, want)
}

func loopTest(t *testing.T, name string, jdkVersionString string, want []string) {
	tests := trimJdk8VersionList(jdkVersionString)
	t.Run(name+"|length=3", func(t *testing.T) {
		if len(tests) != len(want) {
			t.Fatalf("\nFail: len(%d), want(%d)", len(tests), len(want))
		}
	})

	for i := 0; i < len(tests); i++ {
		t.Run(name+"|"+tests[i], func(t *testing.T) {
			if !reflect.DeepEqual(tests[i], want[i]) {
				t.Fatalf("\nFail: %v \nWant: %v", tests[i], want[i])
			}
		})
	}
}

func Test_trimJdk17VersionList(t *testing.T) {
	type args struct {
		versionOutputString string
	}
	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			name: "normal",
			args: args{
				versionOutputString: "openjdk 17.0.11 2024-04-23 LTS\nOpenJDK Runtime Environment TencentKonaJDK (build 17.0.11+1-LTS)\nOpenJDK 64-Bit Server VM TencentKonaJDK (build 17.0.11+1-LTS, mixed mode, sharing)\n",
			},
			want: []string{
				"openjdk 17.0.11 2024-04-23 LTS",
				"OpenJDK Runtime Environment TencentKonaJDK (build 17.0.11+1-LTS)",
				"OpenJDK 64-Bit Server VM TencentKonaJDK (build 17.0.11+1-LTS, mixed mode, sharing)",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := trimJdk17VersionList(tt.args.versionOutputString); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("trimJdk17VersionList() = %v, want %v", got, tt.want)
			}
		})
	}
}
