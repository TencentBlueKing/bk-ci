#!/bin/bash
# bk-ci jre.zip 生成工具
# shellcheck disable=SC2155
set -eu
trap 'on_EXIT' EXIT ERR HUP
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}  # 匹配bcprov jar用
BK_CI_SRC_DIR="${BK_CI_SRC_DIR:-$BK_PKG_SRC_PATH/ci}"  # ci安装源

on_EXIT (){
  local ret=$?
  if [ -d "${temp_java_dir:-}" ]; then
    [ $ret -eq 0 ] || err "fail to create jre.zip"
    #log "clean temp dir: ${temp_java_dir}"
    rm -rf "${temp_java_dir}"
  fi
  return $ret
}
log (){
  echo "${LOG_LEVEL:-INFO}:" "$@"
}
err (){
  LOG_LEVEL=ERROR log "$@"
}

list_tar (){
  tar tf "$1"
}
list_zip (){
  unzip -Z1 "$1"
}
extract_tar (){
  ( cd "$2" && tar xf "$1" )
}
extract_zip (){
  ( cd "$2" && unzip -q "$1" )
}
gen_zip (){
  ( cd "$2" && zip -qr "$1" . ); return $?;
}

get_bcprov_from_dispatchjar (){  # fatjar.
  local bcprov_dst_dir="$1"
  local dispatchjar="$BK_CI_SRC_DIR/dispatch/boot-dispatch.jar"
  log "try to get bcprov.jar from $dispatchjar"
  if ! [ -f "$dispatchjar" ]; then
    err "no bk-ci dispatch.jar: $dispatchjar, run ./bin/prepare-bk-ci.sh first."
    return 2;
  fi
  local bcprov_jar_in_jar=$(list_zip "$dispatchjar" | grep -m 1 "^BOOT-INF/lib/bcprov-jdk.*[.]jar$")
  [ -z "$bcprov_jar_in_jar" ] && { err "bcprov jar not found in $dispatchjar."; return 1; }
  bcprov_jar_path="$bcprov_dst_dir/${bcprov_jar_in_jar##*/}"
  unzip -qc "$dispatchjar" "$bcprov_jar_in_jar" > "$bcprov_jar_path" || return 1
  log "bcprov comes from $dispatchjar"
}
get_bcprov_from_dispatch_lib_dir (){  # slim安装包.
  local bcprov_dst_dir="$1"
  local bcprov_glob="$BK_CI_SRC_DIR/dispatch/lib/bcprov-jdk*.jar"
  log "try to find bcprov in dispatch lib dir by using glob: $bcprov_glob"
  shopt -s nullglob
  declare -a bcprov_dispatch_lib=($bcprov_glob)
  shopt -u nullglob
  local bcprov_src="${bcprov_dispatch_lib[0]:-}"
  bcprov_jar_path="$bcprov_dst_dir/${bcprov_src##*/}"
  if [ -f "$bcprov_src" ]; then
    cp -v "$bcprov_src" "$bcprov_jar_path" || return 1
    log "bcprov comes from $bcprov_src"
  else
    log "no bcprov jar available in dispatch lib dir."
    return 1
  fi
}
find_local_bcprov (){  # 用户自备.
  local bcprov_dst_dir="$1"
  local bcprov_glob="$BK_PKG_SRC_PATH/bcprov-jdk*.jar $PWD/bcprov-jdk*.jar"
  log "try to find bcprov in local dir by using glob: $bcprov_glob"
  shopt -s nullglob
  declare -a bcprov_local=($bcprov_glob)
  shopt -u nullglob
  local bcprov_src="${bcprov_local[0]:-}"
  bcprov_jar_path="$bcprov_dst_dir/${bcprov_src##*/}"
  if [ -f "$bcprov_src" ]; then
    cp -v "$bcprov_src" "$bcprov_jar_path" || return 1
    log "bcprov comes from $bcprov_src"
  else
    log "no bcprov jar available in local."
    return 1
  fi
}

install_bcprov_jar (){
  local bcprov_dst_dir="$1" bcprov_jar_path
  find_local_bcprov "$bcprov_dst_dir" || \
  get_bcprov_from_dispatch_lib_dir "$bcprov_dst_dir" || \
  get_bcprov_from_dispatchjar "$bcprov_dst_dir" || {
    err "no bcprov jar available."
    return 1
  }
  if list_zip "$bcprov_jar_path" | grep -qxim 1 "META-INF/MANIFEST.MF"; then
    log "bcprov was placed in $bcprov_jar_path"
  else
    err "bcprov is not a valid jar file: $bcprov_jar_path"
    return 1
  fi
}

gen_ci_jrezip (){
  local java_dir="$1"
  local new_jrezip_path="$2"
  local jre_home_lib_ext_dir=$(find "$java_dir" -path "*/lib/ext/zipfs.jar" -printf "%h")
  [ -z "$jre_home_lib_ext_dir" ] && { err "cant determin jre_home in $java_dir, no such path: lib/ext/zipfs.jar."; return 1; }
  local jre_home=$(echo "$jre_home_lib_ext_dir" | sed -r 's@(Contents/Home/)?lib/ext/?$@@')
  log "jre_home is $jre_home"
  install_bcprov_jar "$jre_home_lib_ext_dir"
  log "generating jre.zip: $new_jrezip_path"
  gen_zip "$new_jrezip_path" "$jre_home"
  log "job done."
}

main (){
  local os="$1"
  local java_pkg="$2"
  local ci_jre_path="${3:-$BK_CI_SRC_DIR/agent-package/jre/$os/jre.zip}"
  if [ -f "$ci_jre_path" ]; then
    log "target file exist, do nothing: $ci_jre_path"
    return 0;
  fi
  if ! [ -r "$java_pkg" ]; then
    err "invalid java_pkg: no such file or no read permission: $java_pkg"
    return 2;
  fi
  local temp_java_dir_tpl="bk-ci-gen-jrezip-XXXX"
  log "try to create a temp_java_dir in ${TMP_DIR:-/dev/shm}."
  temp_java_dir=$(mktemp -d "${TMP_DIR:-/dev/shm}/$temp_java_dir_tpl" || mktemp -dt "$temp_java_dir_tpl")
  log "temp_java_dir is $temp_java_dir"
  log "extract $java_pkg"
  if grep -iEq "[.](tar[.][gx]z|t[gx]z)$" <<< "$java_pkg"; then
    extract_tar "$java_pkg" "$temp_java_dir"
  elif grep -iEq "[.](zip)$" <<< "$java_pkg"; then
    extract_zip "$java_pkg" "$temp_java_dir"
  else
    err "unsupported file: $java_pkg, filename must end with .tar.gz, .tar.xz or .zip"
    return 1;
  fi
  gen_ci_jrezip "$temp_java_dir" "$ci_jre_path"
}

if [ $# -ge 2 ]; then
  main "$@"
else
  echo "$0 OS JDK_OR_JRE_PKG [CI_JRE_FILE_PATH]  -- generate jre.zip for ci."
  echo "OS should be one of linux, windows or macos."
  echo "JDK_OR_JRE_PKG could be OpenJDK tarball. filename must end with .tar.gz, .tar.xz or .zip"
  echo "CI_JRE_FILE_PATH is generated ci jre.zip, default to BK_CI_SRC_DIR/agent-package/jre/OS/jre.zip."
fi
