WORKSPACE=`pwd`
rm -rf ${WORKSPACE}/ci
mkdir -p ${WORKSPACE}/ci/gateway
cp -r ${WORKSPACE}/src/gateway/* ${WORKSPACE}/ci/gateway
cd ${WORKSPACE}/scripts/
sh ./render_dev -m ci ../support-files/templates/*
cd ${WORKSPACE}/ci/gateway
cp ./lua/auth_user_oa.lua ./lua/auth_user.lua
cp ./lua/oauth_util_oa.lua ./lua/oauth_util.lua