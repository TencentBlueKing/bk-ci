#!/bin/bash
# generate all micro-service template boot.sh

# make sure workdir is WORKSPACE/scripts
cd `dirname $0`

tpl_dir="../support-files/codecc/templates"
tpl_tpl="$tpl_dir/boot-codecc-service.sh"

echo "generate boot-codecc-service.sh from template: $tpl_tpl"
if [ ! -f "$tpl_tpl" ];then
  echo " template not found: $tpl_tpl"
  exit 1
fi

for service_name in `awk -F':boot-' '/:boot-/ && !/assembly"/ {print $2}' ../src/backend/codecc/settings.gradle | tr -d '"'`;do
  # convert lower to upper
  SERVICE_NAME=${service_name^^}

  tpl_name="$service_name#boot-$service_name.sh"
  tpl_file="$tpl_dir/$tpl_name"
  if [ -s "$tpl_file" ];then
    echo "  SKIP: target file exist: $tpl_file"
	continue
  fi
  echo "  generating $tpl_file"
  sed -e "s/bkcodeccservice/$service_name/g" \
      -e "s/BKCODECCSERVICE/$SERVICE_NAME/g" \
        "$tpl_tpl" > "$tpl_file"
done

# assembly ASSEMBLY
tpl_name="assembly#boot-assembly.sh"
tpl_file="$tpl_dir/$tpl_name"

if [ -s "$tpl_file" ];then
  echo "  SKIP: target file exist: $tpl_file"
else
  echo "  generating $tpl_file"
  sed -e "s/bkcodeccservice/assembly/g" -e "s/BKCODECCSERVICE/ASSEMBLY/g" "$tpl_tpl" > "$tpl_file"
fi