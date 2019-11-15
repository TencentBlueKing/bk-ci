WORKSPACE=`pwd`
rm -rf ${WORKSPACE}/ci
mkdir -p ${WORKSPACE}/ci/gateway
cp -r ${WORKSPACE}/src/gateway/* ${WORKSPACE}/ci/gateway
cd ${WORKSPACE}/scripts/
# cp -rf ci_env_local.properties ci_env.properties
cp -rf bkenv.properties ci_env.properties
sh ./render_ci -m ci ../support-files/templates/*
cd ${WORKSPACE}/ci/gateway
rm -rf ./static-tencent
rm -rf ./vhosts
rm -rf  ./lua/auth/auth_user_tencent.lua 
rm -rf  ./lua/util/oauth_util_tencent.lua