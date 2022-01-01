
//#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
//#include <linux/user.h>
//#include <sys/reg.h>
//#include <sys/user.h>
//#include <sys/syscall.h>   /* For SYS_write etc */
#include <sys/stat.h>

#include <stdint.h>
#include <paths.h>
#include <stdio.h>
#include <locale.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <getopt.h>
#include <limits.h>

#include <string>
//#include <vector>

//#include "defs.h"
#include "prints.h"
//#include "sysent.h"
//#include "hook.h"
#include "config.h"
#include "util.h"
//#include "trace_event.h"

const std::string key_ld_preload = "LD_PRELOAD";
//const std::string key_hook_config_path = "BK_HOOK_CONFIG_PATH";

void freeArgv(char** argv, int size) {
	for (int i = 0; i< size; ++i) {
		delete[] argv[i];
	}
	delete[] argv;
}

bool debug_flag = false;
// 默认不输出日志
int debug_level = -1;
config globalConfig;
std::string clientCommand = "";

int main(int argc, char *argv[]) {
	setlocale(LC_ALL, "");

	// TODO : do parse argv here
	static const char optstring[] = "+d:l:";
	int lopt;
	static const struct option longopts[] = {
		{ "help", no_argument, NULL, 'h' },
		{ "version", no_argument, NULL, 'V' },
		{ "command", required_argument, &lopt, 1 },
		{ "config", required_argument, &lopt, 2 },
		{ 0, 0, 0, 0 }
	};
	int c;
	const char* configfile = NULL;
	while ((c = getopt_long(argc, argv, optstring, longopts, NULL)) != EOF) {
		switch (c) {
		case 0: {
			switch (lopt) {
			case 1: { // command
				clientCommand = optarg;
				break;
			}
			case 2: // config
				configfile = optarg;
				break;
			default:
				// unknow long opt
				break;
			}
			break;
		}
		case 'd':
			debug_flag = true;
			break;
		case 'l':
			debug_level = atoi(optarg);
			break;
		case 'h':
			// TODO : show usage here
			break;
		case 'V':
			// TODO : show version here
			break;
		default:
			break;
		}
	}

	//printf("ready set log level to [%d]\n", debug_level);
	set_log_level(debug_level);
	char debug_level_str[32];
	sprintf(debug_level_str, "%d", debug_level);
	if (setenv(key_log_level.c_str(), debug_level_str, 1) != 0 ) {
		int temperr = errno;
		bk_log(FATAL, "failed to set print devel errno[%d]\n", temperr);
		errno = temperr;
		exit(1);
	}

	bk_log(INFO, "get client command[%s]\n", clientCommand.c_str());

	if (configfile == NULL || !globalConfig.Parse(configfile)) {
		bk_log(FATAL, "failed to parse json file [%s],please check it\n", configfile);
		fprintf(stderr, "failed to parse json file [%s],please check it\n", configfile);
		exit(1);
	}
	globalConfig.Dump();

	if (setenv(key_hook_config_path.c_str(), configfile, 1) != 0 ) {
		int temperr = errno;
		bk_log(FATAL, "failed to set config path errno[%d]\n", temperr);
		errno = temperr;
		exit(1);
	}

	// TODO : 如果 配置文件中没有预加载so的路径，可以考虑在可执行路径下查找
	bk_log(DEBUG, "ready set env LD_PRELOAD to [%s]\n", globalConfig.GetPreloadPath().c_str());
	if (setenv(key_ld_preload.c_str(), globalConfig.GetPreloadPath().c_str(), 1) != 0 ) {
		int temperr = errno;
		bk_log(ERROR, "failed to setenv errno[%d]\n", temperr);
		errno = temperr;
	}

    /*
	std::vector<std::string> result_array;
	int size = split(clientCommand, " ", result_array);
	if (size <= 0) {
		bk_log(ERROR, "failed to split client command [%s]\n", clientCommand.c_str());
		exit(1);
	}
	char** clientArgv = stringvec2argv(result_array);
	if (clientArgv == NULL ) {
		bk_log(ERROR, "failed to convert client command [%s] to argv\n", clientCommand.c_str());
		exit(1);
	}
	std::string path = get_path(clientArgv);

	execv(path.c_str(), clientArgv);

	return 0;
    */
    // ++ by tomtian 2020-02-13
    // 对命令参数的处理不彻底，改为直接用system命令
    return system(clientCommand.c_str());
    // --
}