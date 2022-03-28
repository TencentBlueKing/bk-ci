
#ifndef CONFIG_H
#define CONFIG_H

#include "json/CJsonObject.hpp"
//#include <ext/hash_map>
#include <stdint.h>

#include <map>
#include <string>
#include <vector>

const std::string key_basic = "BK_DIST_";
const std::string key_hook_config_path = key_basic + "HOOK_CONFIG_PATH";
const std::string key_controller_scheme = key_basic + "CONTROLLER_SCHEME";
const std::string key_controller_ip = key_basic + "CONTROLLER_IP";
const std::string key_controller_port = key_basic + "CONTROLLER_PORT";
const std::string key_controller_work_id = key_basic + "CONTROLLER_WORK_ID";
const std::string key_env_hook_preload_config_content = key_basic + "HOOK_CONFIG_CONTENT";

/*
namespace __gnu_cxx {
    template<> struct hash< std::string > {
        size_t operator()( const std::string& x ) const {
            return hash< const char* >()( x.c_str() );
        }
    };

    template<> struct hash<long long> {
        size_t operator()(long long x) const {
            return x;
        }
    };
}
*/

class satisfyCondition
{
public:
    satisfyCondition(int32_t counter, std::vector<std::string> files);

public:
    bool isSatisfy();

private:
    int32_t delayCounter;
    std::vector<std::string> dependFiles;
};

class config
{
public:
    config();
    ~config();
    bool Parse(const char *filename);

    std::string GetPreloadPath();
    std::string GetConfig(const std::string key);
    void Dump();

private:
    bool fnMatchPatternHasWildcards(const char *str);
    bool satisfyInjectCondition(const std::string &key);
    // bool parseFromController(neb::CJsonObject **root);
    bool parseFromFile(neb::CJsonObject **root, const char *filename);
    bool parseFromEnv(neb::CJsonObject **root);

private:
    // key为普通字符串
    //__gnu_cxx::hash_map<std::string, std::string> commandNormalHash;
    std::map<std::string, std::string> commandNormalHash;
    // key为glob匹配格式
    //__gnu_cxx::hash_map<std::string, std::string> commandGlobHash;
    std::map<std::string, std::string> commandGlobHash;

    // 记录key设置的延迟生效次数
    //__gnu_cxx::hash_map<std::string, satisfyCondition*> commandDelayHash;
    std::map<std::string, satisfyCondition *> commandDelayHash;

    std::string preloadPath;
};

#endif