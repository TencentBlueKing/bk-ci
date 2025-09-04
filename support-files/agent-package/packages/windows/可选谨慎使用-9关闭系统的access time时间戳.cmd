REM 该功能可以降低因为回写access time降低磁盘IO速度，对提升IO很有效。
REM 注意：除非新增或者修改文件，否则access time不会更新，如果使用file access time，不应该使用该功能。

fsutil behavior set disablelastaccess 1