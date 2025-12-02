package fileutil

import (
	"archive/zip"
	"os"
	"path/filepath"
	"testing"
)

func TestExists(t *testing.T) {
	type args struct {
		file string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "测试判断文件是否存在",
			args: args{
				file: "testdata/existsFile",
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := Exists(tt.args.file); got != tt.want {
				t.Errorf("Exists() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetFileMd5(t *testing.T) {
	type args struct {
		file string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "测试获取文件Md5",
			args: args{
				file: "testdata/md5File",
			},
			want:    "ecd4b77ab9b4b1101d1e4ed2cacad6db",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetFileMd5(tt.args.file)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetFileMd5() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("GetFileMd5() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetString(t *testing.T) {
	type args struct {
		file string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "测试获取文件字符串内容",
			args: args{
				file: "testdata/stringAndPidFile",
			},
			want:    "9527",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetString(tt.args.file)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetString() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("GetString() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetPid(t *testing.T) {
	type args struct {
		file string
	}
	tests := []struct {
		name    string
		args    args
		want    int
		wantErr bool
	}{
		{
			name: "测试获取文件数字内容",
			args: args{
				file: "testdata/stringAndPidFile",
			},
			want:    9527,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetPid(tt.args.file)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetPid() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("GetPid() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestWriteString(t *testing.T) {
	type args struct {
		file string
		str  string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "测试写入文件内容",
			args: args{
				file: "testdata/writeFile_temp",
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := WriteString(tt.args.file, tt.args.str); (err != nil) != tt.wantErr {
				t.Errorf("WriteString() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
	// 删除文件测试
	if err := TryRemoveFile("testdata/writeFile_temp"); err != nil {
		t.Errorf("TryRemoveFile() error = %v, wantErr %v", err, false)
	}
}

// createTestZip 创建一个测试用的 zip 文件
func createTestZip(zipPath string, files map[string]string) error {
	// 确保目录存在
	if err := os.MkdirAll(filepath.Dir(zipPath), os.ModePerm); err != nil {
		return err
	}

	zipFile, err := os.Create(zipPath)
	if err != nil {
		return err
	}
	defer zipFile.Close()

	zipWriter := zip.NewWriter(zipFile)
	defer zipWriter.Close()

	for name, content := range files {
		writer, err := zipWriter.Create(name)
		if err != nil {
			return err
		}
		_, err = writer.Write([]byte(content))
		if err != nil {
			return err
		}
	}

	return nil
}

func TestUnzip(t *testing.T) {
	// 创建临时目录
	tempDir := t.TempDir()

	tests := []struct {
		name     string
		files    map[string]string
		wantErr  bool
		errMsg   string
		validate func(t *testing.T, targetDir string)
	}{
		{
			name: "正常解压文件",
			files: map[string]string{
				"test.txt":         "hello world",
				"subdir/file.txt":  "content in subdir",
				"another/test.txt": "another file",
			},
			wantErr: false,
			validate: func(t *testing.T, targetDir string) {
				// 验证文件是否正确解压
				testFile := filepath.Join(targetDir, "test.txt")
				if !Exists(testFile) {
					t.Errorf("test.txt should exist at %s", testFile)
				}
				subdirFile := filepath.Join(targetDir, "subdir", "file.txt")
				if !Exists(subdirFile) {
					t.Errorf("subdir/file.txt should exist at %s", subdirFile)
				}
				anotherFile := filepath.Join(targetDir, "another", "test.txt")
				if !Exists(anotherFile) {
					t.Errorf("another/test.txt should exist at %s", anotherFile)
				}

				// 验证内容
				content, err := GetString(testFile)
				if err != nil {
					t.Errorf("failed to read test.txt: %v", err)
				}
				if content != "hello world" {
					t.Errorf("content = %v, want %v", content, "hello world")
				}
			},
		},
		{
			name: "正常的相对路径",
			files: map[string]string{
				"normal.txt":       "normal file",
				"subdir/file.txt":  "normal subdir file",
			},
			wantErr: false,
			validate: func(t *testing.T, targetDir string) {
				if !Exists(filepath.Join(targetDir, "normal.txt")) {
					t.Error("normal.txt should exist")
				}
				if !Exists(filepath.Join(targetDir, "subdir", "file.txt")) {
					t.Error("subdir/file.txt should exist")
				}
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// 为每个测试创建独立的 zip 文件和目标目录
			zipPath := filepath.Join(tempDir, tt.name+".zip")
			targetDir := filepath.Join(tempDir, tt.name+"_extracted")

			// 创建测试 zip 文件
			if err := createTestZip(zipPath, tt.files); err != nil {
				t.Fatalf("Failed to create test zip: %v", err)
			}

			// 执行解压
			err := Unzip(zipPath, targetDir)

			// 检查错误
			if (err != nil) != tt.wantErr {
				t.Errorf("Unzip() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			if tt.wantErr && err != nil && tt.errMsg != "" {
				if err.Error() != tt.errMsg && !contains(err.Error(), tt.errMsg) {
					t.Errorf("Unzip() error message = %v, want to contain %v", err.Error(), tt.errMsg)
				}
			}

			// 执行自定义验证
			if !tt.wantErr && tt.validate != nil {
				tt.validate(t, targetDir)
			}

			// 清理
			os.RemoveAll(targetDir)
			os.Remove(zipPath)
		})
	}
}

// TestUnzipZipSlip 专门测试 Zip Slip 攻击防护
func TestUnzipZipSlip(t *testing.T) {
	tempDir := t.TempDir()

	tests := []struct {
		name         string
		maliciousName string
		wantErr      bool
	}{
		{
			name:         "防止路径遍历攻击-三层上级目录",
			maliciousName: "../../../etc/passwd",
			wantErr:      true,
		},
		{
			name:         "防止路径遍历攻击-子目录中的上级遍历",
			maliciousName: "subdir/../../outside.txt",
			wantErr:      true,
		},
		{
			name:         "防止路径遍历攻击-单层上级目录",
			maliciousName: "../outside.txt",
			wantErr:      true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			zipPath := filepath.Join(tempDir, "malicious.zip")
			targetDir := filepath.Join(tempDir, "extracted")

			// 手动创建包含恶意路径的 zip 文件
			if err := createMaliciousZip(zipPath, tt.maliciousName, "malicious content"); err != nil {
				t.Fatalf("Failed to create malicious zip: %v", err)
			}

			// 尝试解压
			err := Unzip(zipPath, targetDir)

			// 应该返回错误
			if (err != nil) != tt.wantErr {
				t.Errorf("Unzip() error = %v, wantErr %v", err, tt.wantErr)
			}

			if err != nil && !contains(err.Error(), "illegal file path") {
				t.Errorf("Expected 'illegal file path' error, got: %v", err)
			}

			// 清理
			os.RemoveAll(targetDir)
			os.Remove(zipPath)
		})
	}
}

// createMaliciousZip 创建包含恶意路径的 zip 文件
func createMaliciousZip(zipPath, maliciousName, content string) error {
	if err := os.MkdirAll(filepath.Dir(zipPath), os.ModePerm); err != nil {
		return err
	}

	zipFile, err := os.Create(zipPath)
	if err != nil {
		return err
	}
	defer zipFile.Close()

	zipWriter := zip.NewWriter(zipFile)
	defer zipWriter.Close()

	// 直接使用恶意文件名创建 zip entry
	header := &zip.FileHeader{
		Name:   maliciousName,
		Method: zip.Deflate,
	}
	writer, err := zipWriter.CreateHeader(header)
	if err != nil {
		return err
	}
	_, err = writer.Write([]byte(content))
	return err
}

// contains 检查字符串是否包含子串
func contains(s, substr string) bool {
	return len(s) >= len(substr) && (s == substr || len(s) > len(substr) && stringContains(s, substr))
}

func stringContains(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}
