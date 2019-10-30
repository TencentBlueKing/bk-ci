rm -rf ./ci
mkdir -p ./ci/gateway
cp -r ./src/gateway/* ./ci/gateway
cd ./scripts/
sh ./render_dev -m ci ../support-files/templates/*
cd ./ci/gateway
cp ./lua/auth_user_oa.lua ./lua/auth_user.lua
cp ./lua/oauth_util_oa.lua ./lua/oauth_util.lua
