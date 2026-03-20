package utils

// IsStringSliceBlank 检查一个字符串切片是否是空字符
func IsStringSliceBlank(slice []string) bool {
	if len(slice) == 0 {
		return true
	}
	f := true
	for _, str := range slice {
		if str != "" {
			f = false
		}
	}
	return f
}
