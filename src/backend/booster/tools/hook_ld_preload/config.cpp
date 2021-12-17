
#include "config.h"
#include "prints.h"
#include "json/CJsonObject.hpp"
#include "util.h"

#include <fnmatch.h>
#include <stdint.h>
#include <stdlib.h>
// #include <curl/curl.h>

//#include <iostream>
#include <string>
//#include <sstream>
#include <fstream>

const std::string key_hook = "hooks";
const std::string key_syscall = "syscall";
const std::string key_src_command = "src_command";
const std::string key_target_command = "target_command";
const std::string key_delay_counter = "delay_counter";
const std::string key_depend_files = "depend_files";
const std::string key_controller_resp_result = "result";
const std::string key_controller_resp_message = "message";
const std::string key_controller_resp_data = "data";
const std::string key_controller_resp_preload = "preload";
const std::string key_split = "|";

satisfyCondition::satisfyCondition(int32_t counter, std::vector<std::string> files)
{
	if (counter > 0)
	{
		delayCounter = counter;
	}

	if (!files.empty())
	{
		dependFiles = files;
	}
}

// 判断是否满足hook条件
bool satisfyCondition::isSatisfy()
{
	if ((--delayCounter) >= 0)
	{
		bk_log(INFO, "isSatisfy not satisfy for delayCounter[%d]\n", delayCounter);
		return false;
	}

	if (!dependFiles.empty())
	{
		std::vector<std::string>::iterator ite = dependFiles.begin();
		for (; ite != dependFiles.end();)
		{
			// 判断目标文件是否存在
			if (!isFileExist(ite->c_str()))
			{
				bk_log(INFO, "isSatisfy not satisfy for [%s] not existed\n", ite->c_str());
				return false;
			}
			else
			{
				// 如果已经存在，则删除掉，提高下次判断效率
				ite = dependFiles.erase(ite);
			}
		}
	}

	return true;
}

config::config()
{
	preloadPath = "";
}

config::~config()
{
	// TODO : free resources
}

size_t writefunc(void *ptr, size_t size, size_t nmemb, std::string *s)
{
	s->append(static_cast<char *>(ptr), size * nmemb);
	return size * nmemb;
}

// bool config::parseFromController(neb::CJsonObject **root)
// {
// 	const char *work_id = getenv(key_controller_work_id.c_str());
// 	const char *controller_scheme = getenv(key_controller_scheme.c_str());
// 	const char *controller_ip = getenv(key_controller_ip.c_str());
// 	const char *controller_port = getenv(key_controller_port.c_str());

// 	if (!controller_scheme)
// 		controller_scheme = "http";
// 	if (!controller_ip)
// 		controller_ip = "127.0.0.1";
// 	if (!controller_port)
// 		controller_port = "30117";

// 	if (!work_id)
// 	{
// 		bk_log(DEBUG, "Try to get settings from controller failed, work_id no found\n");
// 		return false;
// 	}

// 	std::string uri = std::string(controller_scheme) + "://" + std::string(controller_ip) + ":" + std::string(controller_port) + "/api/v1/dist/work/" + std::string(work_id) + "/settings";

// 	bk_log(DEBUG, "Try to get settings from controller with uri(%s)\n", uri.c_str());

// 	CURL *curl = curl_easy_init();
// 	if (!curl)
// 	{
// 		bk_log(DEBUG, "Try to get settings from controller with work_id(%s), get curl failed\n", work_id);
// 		return false;
// 	}

// 	std::string s;
// 	curl_easy_setopt(curl, CURLOPT_URL, uri.c_str());
// 	curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writefunc);
// 	curl_easy_setopt(curl, CURLOPT_WRITEDATA, &s);

// 	CURLcode res = curl_easy_perform(curl);
// 	if (res != CURLE_OK)
// 	{
// 		bk_log(DEBUG, "Try to get settings from controller with work_id(%s), request failed\n", work_id);
// 		return false;
// 	}
// 	bk_log(DEBUG, "Get settings from controller: %s\n", s.c_str());

// 	// decode json to map
// 	neb::CJsonObject data(s.c_str());
// 	if (!data.Parse(s))
// 	{
// 		bk_log(DEBUG, "failed to parse json for uri: %s\n", uri.c_str());
// 		return false;
// 	}

// 	bool oJsonResult;
// 	if (!data.Get(key_controller_resp_result, oJsonResult))
// 	{
// 		bk_log(DEBUG, "failed to get result from resp json for uri: %s\n", uri.c_str());
// 		return false;
// 	}

// 	std::string oJsonMessage;
// 	if (!data.Get(key_controller_resp_message, oJsonMessage))
// 	{
// 		bk_log(DEBUG, "failed to get message from resp json for uri: %s\n", uri.c_str());
// 		return false;
// 	}

// 	if (!oJsonResult)
// 	{
// 		bk_log(DEBUG, "failed to get settings from controller, get result false, message: %s\n", oJsonMessage.c_str());
// 		return false;
// 	}

// 	neb::CJsonObject oJsonData;
// 	if (!data.Get(key_controller_resp_data, oJsonData))
// 	{
// 		bk_log(DEBUG, "failed to get data from resp json for uri: %s\n", uri.c_str());
// 		return false;
// 	}

// 	neb::CJsonObject *oJsonPreload = new neb::CJsonObject;
// 	if (!oJsonData.Get(key_controller_resp_preload, *oJsonPreload))
// 	{
// 		bk_log(DEBUG, "failed to get preload data from resp json for uri: %s\n", uri.c_str());
// 		return false;
// 	}

// 	bk_log(DEBUG, "success to get settings from controller: %s\n", oJsonPreload->ToString().c_str());
// 	*root = oJsonPreload;
// 	return true;
// }

bool config::parseFromFile(neb::CJsonObject **root, const char *filename)
{
	bk_log(DEBUG, "Parse for file[%s]\n", filename);

	// !!! 不能用fstream的方式读文件，会在构造函数 std::basic_ifstream 的std::locale::locale() 函数出现异常，具体原因未知 !!!
	std::string str;
	char buffer[1024], *ret;
	FILE *fp = fopen(filename, "r");
	if (!fp)
	{
		bk_log(ERROR, "config file[%s] not existed\n", filename);
		return false;
	}
	while (true)
	{
		ret = fgets(buffer, 1024, fp);
		if (!ret)
		{
			break;
		}
		str.append(buffer);
	}
	fclose(fp);

	// decode json to map
	neb::CJsonObject *data = new neb::CJsonObject(str.c_str());
	if (!data->Parse(str))
	{
		bk_log(ERROR, "failed to parse json for file [%s]\n", filename);
		return false;
	}

	bk_log(DEBUG, "json[%s]\n", data->ToString().c_str());

	*root = data;
	bk_log(DEBUG, "success to get settings from file: %s\n", filename);
	return true;
}

bool config::parseFromEnv(neb::CJsonObject **root)
{
	bk_log(DEBUG, "Parse for env[%s]\n", key_env_hook_preload_config_content.c_str());

	const char *str = getenv(key_env_hook_preload_config_content.c_str());
	if (str == NULL)
	{
		bk_log(ERROR, "failed to get json data for env [%s], errno %d\n", key_env_hook_preload_config_content.c_str(), errno);
		return false;
	}

	// decode json to map
	neb::CJsonObject *data = new neb::CJsonObject(str);
	if (!data->Parse(str))
	{
		bk_log(ERROR, "failed to parse json for env [%s] content[%s],err[%s]\n", key_env_hook_preload_config_content.c_str(), str, data->GetErrMsg().c_str());
		return false;
	}

	*root = data;
	bk_log(DEBUG, "success to get settings [%s] from env: %s\n", data->ToString().c_str(), key_env_hook_preload_config_content.c_str());
	return true;
}

bool config::Parse(const char *filename)
{
	neb::CJsonObject *root;
	if (!parseFromEnv(&root))
	{
		// if (!this->parseFromController(&root))
		// {
		if (filename == NULL || !this->parseFromFile(&root, filename))
		{
			return false;
		}
		// }
	}

	bk_log(DEBUG, "success to get settings: %s\n", root->ToString().c_str());

	// parse hooks
	neb::CJsonObject oJsonObjectHook;
	if (!root->Get(key_hook, oJsonObjectHook))
	{
		bk_log(ERROR, "failed to get hook object for file [%s]\n", filename);
		return false;
	}

	if (!oJsonObjectHook.IsArray())
	{
		bk_log(ERROR, "hook object is not array for file [%s]\n", filename);
		return false;
	}

	bk_log(DEBUG, "oJsonObjectHook size[%d]\n", oJsonObjectHook.GetArraySize());
	std::string syscall;
	std::string src_command;
	std::string target_command;
	std::string depend_file_string;
	std::vector<std::string> depend_files;
	for (int i = 0; i < oJsonObjectHook.GetArraySize(); ++i)
	{
		neb::CJsonObject oJsonObjectCommand = oJsonObjectHook[(unsigned int)i];
		if (!oJsonObjectCommand.Get(key_src_command, src_command))
		{
			bk_log(ERROR, "hook object not found src_command for file [%s]\n", filename);
			return false;
		}
		if (!oJsonObjectCommand.Get(key_target_command, target_command))
		{
			bk_log(ERROR, "hook object not found target_command for file [%s]\n", filename);
			return false;
		}

		bk_log(DEBUG, "ready add key[%s] value[%s]\n", src_command.c_str(), target_command.c_str());
		if (fnMatchPatternHasWildcards(src_command.c_str()))
		{
			commandGlobHash.insert(std::make_pair(src_command, target_command));
		}
		else
		{
			commandNormalHash.insert(std::make_pair(src_command, target_command));
		}

		satisfyCondition *condition = NULL;
		int32_t delay_times = 0;
		if (oJsonObjectCommand.Get(key_delay_counter, delay_times))
		{
			bk_log(INFO, "got delay_counter [%d] for key[%s]\n", delay_times, src_command.c_str());
		}
		if (oJsonObjectCommand.Get(key_depend_files, depend_file_string))
		{
			bk_log(INFO, "got depend file [%s] for key[%s]\n", key_depend_files.c_str(), depend_file_string.c_str());
			split(depend_file_string, key_split.c_str(), depend_files);
		}
		if (delay_times > 0 || !depend_files.empty())
		{
			condition = new satisfyCondition(delay_times, depend_files);
			commandDelayHash.insert(std::make_pair(src_command, condition));
		}
	}

	return true;
}

bool config::satisfyInjectCondition(const std::string &key)
{
	bk_log(DEBUG, "satisfyInjectCondition with key[%s]\n", key.c_str());

	// 先查询普通map
	std::map<std::string, satisfyCondition *>::iterator ite = commandDelayHash.find(key);
	if (ite != commandDelayHash.end())
	{
		if (!ite->second->isSatisfy())
		{
			bk_log(INFO, "satisfyInjectCondition not satisfy with key[%s]\n", key.c_str());
			return false;
		}
		else
		{
			delete ite->second;
			commandDelayHash.erase(ite);
			return true;
		}
	}

	return true;
}

std::string config::GetPreloadPath()
{
	return preloadPath;
}

std::string config::GetConfig(const std::string key)
{
	bk_log(DEBUG, "GetConfig with key[%s]\n", key.c_str());

	if (!satisfyInjectCondition(key))
	{
		return std::string("");
	}

	// 先查询普通map
	std::map<std::string, std::string>::iterator ite = commandNormalHash.find(key);
	if (ite != commandNormalHash.end())
	{
		return ite->second;
	}

	// 再查询glob匹配map
	std::map<std::string, std::string>::iterator ite1 = commandGlobHash.begin();
	std::map<std::string, std::string>::iterator ite1_end = commandGlobHash.end();
	for (; ite1 != ite1_end; ++ite1)
	{
		if (fnmatch(ite1->first.c_str(), key.c_str(), 0) == 0)
		{
			return ite1->second;
		}
	}

	return std::string("");
}

void config::Dump()
{
	std::map<std::string, std::string>::iterator ite = commandNormalHash.begin();
	std::map<std::string, std::string>::iterator ite_end = commandNormalHash.end();
	for (; ite != ite_end; ++ite)
	{
		bk_log(DEBUG, "key[%s] value[%s]\n", ite->first.c_str(), ite->second.c_str());
	}

	std::map<std::string, std::string>::iterator ite1 = commandGlobHash.begin();
	std::map<std::string, std::string>::iterator ite1_end = commandGlobHash.end();
	for (; ite1 != ite1_end; ++ite1)
	{
		bk_log(DEBUG, "key[%s] value[%s]\n", ite1->first.c_str(), ite1->second.c_str());
	}
}

bool config::fnMatchPatternHasWildcards(const char *str)
{
	const uint32_t max_check_len = 10240;
	uint32_t index = 0;
	while (1)
	{
		switch (*str++)
		{
		case '?':
		case '*':
		case '[':
			return true;

		case '\0':
			return false;
		}
		++index;
		if (index > max_check_len)
		{
			return false;
		}
	}
}