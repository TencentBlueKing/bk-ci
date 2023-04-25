WORKSPACE=`pwd`
CONFIGDIR="/data/src/git/bkdevops/devops-config"

cp -rf ${CONFIGDIR}/gateway_frontend/* ${WORKSPACE}/
rm -rf ${WORKSPACE}/ci
mkdir -p ${WORKSPACE}/ci/gateway
cp -r ${WORKSPACE}/src/gateway/* ${WORKSPACE}/ci/gateway
cd ${WORKSPACE}/scripts/
# cp -rf ci_env_local.properties ci_env.properties
cp -rf ci_env_local.properties ci_env.properties
sh ./render_ci -m ci ../support-files/templates/*
cp -rf ${WORKSPACE}/ci/gateway/tencent/* ${WORKSPACE}/ci/gateway/core/
