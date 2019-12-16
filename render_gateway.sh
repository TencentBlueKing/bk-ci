WORKSPACE=`pwd`
rm -rf ${WORKSPACE}/ci
mkdir -p ${WORKSPACE}/ci/gateway
cp -r ${WORKSPACE}/src/gateway/* ${WORKSPACE}/ci/gateway
cd ${WORKSPACE}/scripts/
# cp -rf ci_env_local.properties ci_env.properties
cp -rf ci_env_local.properties ci_env.properties
sh ./render_ci -m ci ../support-files/templates/*
cd ${WORKSPACE}/ci/gateway
cp -rf ./static-tencent/* ./static
cp ./lua/auth/auth_user_tencent.lua ./lua/auth/auth_user.lua
cp ./lua/util/oauth_util_tencent.lua ./lua/util/oauth_util.lua
