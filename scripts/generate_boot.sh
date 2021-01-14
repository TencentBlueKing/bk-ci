#!/bin/bash
# generate all micro-service template boot.sh

# make sure workdir is WORKSPACE/scripts
cd `dirname $0`

tpl_dir="../support-files/templates"
tpl_tpl="$tpl_dir/boot-service.sh"

echo "generate boot-service.sh from template: $tpl_tpl"
if [ ! -f "$tpl_tpl" ];then
  echo " template not found: $tpl_tpl"
  exit 1
fi

for service_name in `awk -F':boot-' '/:boot-/ && !/assembly"/ {print $2}' ../src/backend/ci/settings.gradle | tr -d '"' | sed '$aagentless'`;do
  # convert lower to upper
  SERVICE_NAME=${service_name^^}

  tpl_name="$service_name#boot-$service_name.sh"
  tpl_file="$tpl_dir/$tpl_name"
  if [ -s "$tpl_file" ];then
    echo "  SKIP: target file exist: $tpl_file"
	continue
  fi
  echo "  generating $tpl_file"
  sed -e "s/bkciservice/$service_name/g" \
      -e "s/BKCISERVICE/$SERVICE_NAME/g" \
        "$tpl_tpl" > "$tpl_file"
done

# assembly ASSEMBLY
tpl_name="assembly#boot-assembly.sh"
tpl_file="$tpl_dir/$tpl_name"

if [ -s "$tpl_file" ];then
  echo "  SKIP: target file exist: $tpl_file"
else
  echo "  generating $tpl_file"
  sed -e "s/bkciservice/assembly/g" -e "s/BKCISERVICE/ASSEMBLY/g" "$tpl_tpl" > "$tpl_file"
fi