#!/bin/bash
# fat-jar展开合并.
set -eu

# 根据jar文件名确定是private还是public.
patt_jar_private_filename="${patt_jar_private_filename:-^(common|model|biz|api|iam|plugin)-}"
# 一些目录名
# public 目录为公网发行的jar文件, 版本变动频率较低
jar_public_dirname=jars-public
# private 目录为本项目内部使用的, 随项目版本变动而变.
jar_private_dirname=jars-private
# local 目录为项目内各组件私有的java -cp目录. 符号链接到 public 及 private 目录.
jar_local_dirname=lib

trap 'on_EXIT' EXIT

on_EXIT (){
  local ret=$?
  if [ -d "${temp_jar_dir:-}" ]; then
    if [ "${keep_temp_jar_dir:-0}" = "1" ]; then
      echo "keep dir: ${temp_jar_dir}"
    else
      echo "clean temp dir: ${temp_jar_dir}"
      rm -rf "${temp_jar_dir}"
    fi
  fi
  return $ret
}

[ $# -eq 3 ] && [ -n "$1" ] || {
  echo "Usage: $0 service /path/to/fat-jar /path/to/slim-ci-home/ -- slim spring boot fat jars to dest dir."
  echo "example: $0 misc src/backend/ci/release/boot-misc.jar ./slimed-ci/"
  echo ""
  echo "Env vars:"
  echo " * temp_jar_dir=/path/to/dir: a temp dir during slim, will create it but not clean it."
  echo " * keep_temp_jar_dir=1: force keep temp_jar_dir. you should delete it yourself."
  exit 0
}
ms=$1
fat_jar=$2
slim_dir=$3

tip_file_exist (){
  local m="file exist" e=0
  [ -f "$1" ] || { m="file not exist"; e=1; }
  echo "$1: $m."
  return $e
}

tip_dir_exist (){
  local m="dir exist" e=0
  [ -d "$1" ] || { m="dir not exist"; e=1; }
  echo "$1: $m."
  return $e
}

extract_zip (){
  ( cd "$2" && unzip -oq "$1" )
}

jar_hash_md5 (){
  local file="$1" md5
  md5=$(md5sum "$file" | awk '{print $1}')
  [ ${#md5} -eq 32 ] || { echo >&2 "Abort. unable to calculate md5sum for $jar_name: $md5."; exit 1; }
  echo "$md5"
}

# env jar_share_dir
# env jar_local_dir
# env jar_suffix=".jar".
# env jar_hash_func=jar_hash_md5
# 收集jar文件到jar_share_dir, 并在jar_local_dir里创建符号链接, 使用相对路径指向jar_share_dir.
gathering_jar_lib (){
  local jar_filename jar_name jar_file_hash jar_hashed_filename
  local jar_suffix=${jar_suffix:-.jar}
  local jar_hash_func=${jar_hash_func:-jar_hash_md5}
  local jar_src_path jar_dst_path jar_local_path
  [ -z "$jar_public_dir" ] && { echo "env jar_public_dir is not set."; return 1; }
  [ -z "$jar_private_dir" ] && { echo "env jar_private_dir is not set."; return 1; }
  [ -z "$jar_local_dir" ] && { echo "env jar_local_dir is not set."; return 1; }
  [ -d "$jar_public_dir" ] || { echo "$jar_public_dir is not a directory."; return 1; }
  [ -d "$jar_private_dir" ] || { echo "$jar_private_dir is not a directory."; return 1; }
  [ -d "$jar_local_dir" ] || { echo "$jar_local_dir is not a directory."; return 1; }
  #echo "gathering_jar_lib args: $*."
  for jar_src_path in "$@"; do
    [ -f "$jar_src_path" ] || { echo "SKIP file not exist: $jar_src_path."; continue; }
    jar_filename=${jar_src_path##*/}
    jar_name=${jar_filename%$jar_suffix}
    [ "$jar_name$jar_suffix" = "$jar_filename" ] || {
      echo "SKIP bad filename: $jar_filename, it should be suffixed with $jar_suffix.";
      continue;
    }
    jar_file_hash=$($jar_hash_func "$jar_src_path")
    [ -n "$jar_file_hash" ] || {
      echo "EXIT unable to get file hash: $jar_src_path: $jar_file_hash."
      exit 1
    }
    jar_hashed_filename="$jar_name--$jar_file_hash$jar_suffix"
    # 判断包名为public或private.
    if [[ "$jar_name" =~ $patt_jar_private_filename ]]; then
      jar_dst_path="$jar_private_dir/$jar_hashed_filename"
      [ -f "$jar_dst_path" ] || echo "new private jar: $jar_dst_path."
    else
      jar_dst_path="$jar_public_dir/$jar_hashed_filename"
    fi
    jar_local_path="$jar_local_dir/$jar_filename"
    # 共享路径是带hash的名称, 所以自动跳过已经存在的.
    cp -nv "$jar_src_path" "$jar_dst_path"
    # 如果链接已经是最新, 则无需处理.
    if [ -n "$(readlink -f "$jar_dst_path" "$jar_local_path"| uniq -d)" ]; then
      : echo "SKIP symlink $jar_local_path to $jar_dst_path."
    else
      ln -svnrf "$jar_dst_path" "$jar_local_path"
    fi
  done
}

echo "check..."
tip_file_exist "$fat_jar" || exit 1
tip_dir_exist "$slim_dir" || exit 1

ms_dir="$slim_dir/$ms"
jar_public_dir="$slim_dir/$jar_public_dirname"
jar_private_dir="$slim_dir/$jar_private_dirname"
jar_local_dir="$ms_dir/$jar_local_dirname"

temp_jar_dir_tpl="bkci-jar-slim-XXXX"
# 环境变量使用永久存储, 则不删除.
if [ -n "${temp_jar_dir:-}" ]; then
  echo "temp_jar_dir defined: $temp_jar_dir, will not clean it."
  keep_temp_jar_dir=1
else
  echo "try to create a temp_jar_dir in ${TMP_DIR:-/dev/shm}."
  temp_jar_dir=$(mktemp -d "${TMP_DIR:-/dev/shm}/$temp_jar_dir_tpl" || mktemp -dt "$temp_jar_dir_tpl")
fi
mkdir -p "$temp_jar_dir" || exit 1
echo "temp_jar_dir is $temp_jar_dir"

echo "creating dirs: $ms_dir $jar_public_dir $jar_private_dir $jar_local_dir"
mkdir -p "$ms_dir" "$jar_public_dir" "$jar_private_dir" "$jar_local_dir" || exit 1

echo "extract_zip: $fat_jar into $temp_jar_dir"
if ! extract_zip "$(readlink -f "$fat_jar")" "$(readlink -f "$temp_jar_dir")"; then
  echo "ERROR: failed to extract zip file: $fat_jar."
  exit 1
fi
springboot_classes_dir="$temp_jar_dir/BOOT-INF/classes"
tip_dir_exist "$springboot_classes_dir" || exit 1

springboot_lib_dir="$temp_jar_dir/BOOT-INF/lib"
tip_dir_exist "$springboot_lib_dir" || exit 1

#start_class=$(grep -Po '(?<=Start-Class: )\S*' "$temp_jar_dir/META-INF/MANIFEST.MF")
# MANIFEST.MF单行限制72字节. 去除\r\n. 剩余70字节, 续行以空格开头.
start_class=$(awk -v RS="[\r\n]+" '/Start-Class: /{ v=$2; if(length($0)>=70){
  while(getline){ if($0~/^ /){v=v substr($0,2); } else {break; }; }; };
  print v}' "$temp_jar_dir/META-INF/MANIFEST.MF")
if [ -z "$start_class" ]; then
  echo "failed to get springboot start_class."
  exit 1
fi
echo "springboot start_class is $start_class."
echo "generate default config:"
tee "$ms_dir/service.env" <<EOF
MAIN_CLASS="$start_class"
MEM_OPTS="-Xms256m -Xmx512m"
CLASSPATH=".:lib/*"
SPRING_CONFIG_LOCATION="file:./application.yml"
SPRING_CLOUD_CONFIG_ENABLED=false
JAVA_TOOL_OPTIONS="-Djava.security.egd=file:/dev/urandom -Dcertificate.file= -Dservice.log.dir=./logs/ -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=utf8 -XX:NewRatio=1 -XX:SurvivorRatio=8 -XX:+UseConcMarkSweepGC"
EOF

echo "copy classes:"
rsync -cav "$springboot_classes_dir/" "$ms_dir"
echo "gathering jar lib. patt_jar_private_filename is $patt_jar_private_filename."
gathering_jar_lib "$springboot_lib_dir/"*.jar || exit 1

echo "generate manifest."
mkdir -p "$ms_dir/META-INF/"
LF=$'\r'
# 仅为flag文件, 不是给java用的, 故无需兼容72字节的限制.
tee "$ms_dir/META-INF/MANIFEST.MF" <<EOF
Manifest-Version: 1.0$LF
Main-Class: $start_class$LF
Created-By: bk-ci-slim.sh$LF
EOF

echo "job done."
