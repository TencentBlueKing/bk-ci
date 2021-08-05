#!/bin/bash
# 生成bk-codecc安装包.
# 收集编译产物, 生成所需的安装包目录, 然后打包.
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

my_path="$(readlink -f "$0")"
my_dir=${my_path%/*}
[ -d "$my_dir" ] || { echo >&2 "ERROR: my_dir is NOT an existed dir: $my_dir."; return 3; }
cmd_collect_codecc_ms_name="$my_dir/bk-codecc-collect-ms-name.sh"
cmd_codecc_slim="$my_dir/../bk-ci-slim.sh"  # 复用ci-lism脚本.

collect_frontend (){
  echo "collect_frontend"
  mkdir -p "$codecc_pkg_dir/frontend" "$codecc_pkg_dir/support-files/templates"
  cp -a "$codecc_bin_frontend_dir/." "$codecc_pkg_dir/frontend"
  echo "collect page templates."
  find "$codecc_pkg_dir/frontend" -name "frontend#*" -exec mv -v {} "$codecc_pkg_dir/support-files/templates" \;
}

collect_backend (){
  echo "collect_backend"
  # 收集fatjar, slim化.
  svcs=$(ls "$codecc_bin_msjar_dir" | sed -n 's/boot-\(.*\).jar/\1/p')
  for ms in $svcs; do
    "$cmd_codecc_slim" "$ms" "$codecc_bin_msjar_dir/boot-$ms.jar" "$codecc_pkg_dir"
  done
  # 清理codecc_ms_wip
  codecc_template_dir="$codecc_pkg_dir/support-files/templates"
  shopt -s nullglob
  echo "remove ms wip: $codecc_ms_wip."
  for ms in ${codecc_ms_wip//,/ }; do
    echo " remove ms dir: ${codecc_pkg_dir:-codecc_pkg_dir}/${ms:-ms}/."
    if [ -d "${codecc_pkg_dir:-codecc_pkg_dir}/${ms:-ms}/" ]; then
      rm -rf "${codecc_pkg_dir:-codecc_pkg_dir}/${ms:-ms}/"
    fi
    echo " remove conf:"
    for f in "$codecc_template_dir/$ms#"* "$codecc_template_dir/"*"#$ms#"*; do
      rm -vf "$f"
    done
  done
  shopt -u nullglob
}

collect_dirs (){
  local e=0
  for d in "$@"; do
    if [ -d "$d" ]; then
      cp -r "$codecc_code_dir/$d" "$codecc_pkg_dir"
    else
      echo "WARING: dir does not exist: $d."
      let ++e
    fi
  done
  return "$e"
}

collect_gateway (){
  cp -r "$codecc_code_dir/src/gateway" "$codecc_pkg_dir"
}

prepare_reports (){
  echo "copy defect/ to report/."
  cp -r "$codecc_pkg_dir/defect" "$codecc_pkg_dir/report"
  echo "copy defect/ to asyncreport/."
  cp -r "$codecc_pkg_dir/defect" "$codecc_pkg_dir/asyncreport"
  # asyncreport 加载代码需要大量内存.
  sed -i 's/MEM_OPTS=.*/MEM_OPTS="-Xms512m -Xms2048m"/' "$codecc_pkg_dir/asyncreport/service.env"
}

packager_codecc (){
  mkdir -p "$codecc_pkg_dir"
  echo "codecc_code_dir is $codecc_code_dir."
  echo "codecc_pkg_dir is $codecc_pkg_dir."
  mkdir -p "$codecc_pkg_dir/scripts" "$codecc_pkg_dir/support-files"
  collect_dirs "scripts/deploy-codecc" "src/gateway"
  rsync -rav "scripts/deploy-codecc" "$codecc_pkg_dir/scripts/"
  rsync -rav "support-files/codecc/" "$codecc_pkg_dir/support-files/"
  rm -rf "$codecc_pkg_dir"/support-files/k8s
  collect_dirs "docs" || echo "skip docs."  # 可选.
  collect_backend
  collect_frontend
  prepare_reports
  echo "gen version:"
  echo "$VERSION" | tee "$codecc_pkg_dir/VERSION"
  # codecc.env version 也留着. 可能今后会用到.
  echo "BK_CODECC_VERSION=\"$VERSION\"" | tee -a "$codecc_pkg_dir/scripts/deploy-codecc/codecc.properties"
  echo "generate $codecc_pkg_path from $codecc_pkg_dir."
  (cd "$codecc_pkg_dir/.."; tar acf "$codecc_pkg_path" "$(basename "$codecc_pkg_dir")"; )
  ls -l "$codecc_pkg_path"
}

if [ $# -lt 2 ]; then
  echo "Usage: $0 VERSION CODECC_PKG_PATH CODECC_CODE_DIR  -- make install package"
  echo " VERSION      version string. example: v1.X.X-desc"
  echo " CODECC_PKG_PATH  generated install package path."
  echo " CODECC_CODE_DIR  source code with compiled binaries."
  echo "Example: $0 v1.5.0-RELEASE workspace/bkcodecc-slim.tar.gz workspace/bk-codecc-v1.5.0"
  echo "ENV:"
  echo " codecc_pkg_dir   temp dir contains codecc package files, should end with /codecc/."
  echo " codecc_ms_wip    ms still wip, should be removed if exist. comma separated."
  exit 0
else
  VERSION="$1"
  codecc_pkg_path="$(readlink -f "$2")"
  codecc_code_dir="${3:-${my_dir%/*}}"  # 默认为本脚本的上层目录.
  codecc_pkg_dir="${codecc_pkg_dir:-$codecc_code_dir/codecc}"  # 默认为代码目录下的codecc目录.
  codecc_ms_wip="${codecc_ms_wip:-}"  # 暂无
  # 编译后的目录, 其他目录为code_dir的相对路径, 不提供修改.
  codecc_bin_frontend_dir="${codecc_bin_frontend_dir:-$codecc_code_dir/src/frontend/devops-codecc/dist/}"
  codecc_bin_msjar_dir=${codecc_bin_msjar_dir:-$codecc_code_dir/src/backend/codecc/release/}
  packager_codecc
fi
