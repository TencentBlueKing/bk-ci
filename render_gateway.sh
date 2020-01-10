WORKSPACE=`pwd`
rm -rf ${WORKSPACE}/ci
mkdir -p ${WORKSPACE}/ci/gateway
cp -r ${WORKSPACE}/src/gateway/* ${WORKSPACE}/ci/gateway
cd ${WORKSPACE}/scripts/
sh ./render_local -m ci ../support-files/templates/*
cd ${WORKSPACE}/ci/gateway
cp ./lua/auth/auth_user_oa.lua ./lua/auth/auth_user.lua
cp ./lua/auth/oauth_util_oa.lua ./lua/auth/oauth_util.lua
