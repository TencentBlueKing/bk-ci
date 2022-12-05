#include "../config.h"
#include "../rawparse.h"
#include "../prints.h"
#include "../util.h"

#include <assert.h>
#include <dlfcn.h>
#include <errno.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>

// environ 是在unistd.h中声明的全局变量，所以无需再声明
extern char **environ;

config *hookConfig = NULL;
static bool envInited = false;
static bool needHook = true;
static bool checkedHookFlag = false;

static bool rawparsed = false;

void printEnv();

// 该环境变量key，用于在当前进程hook成功后，设置标记为true，避免其子进程继续hook
const char *key_hooked_flag = "BK_PARENT_PROCESS_HOOKED";
const char *full_hooked_flag_env = "BK_PARENT_PROCESS_HOOKED=true";
//const char* default_config_file = "/etc/bkhook/config_file.json";

const char *key_src_cmd = "$SRC_CMD";

void initEnv()
{
    if (envInited)
    {
        bk_log(DEBUG, "envInited is true, do nothing now\n");
        return;
    }

    // TODO : init log level here?
    const char *log_level_str = getenv(key_log_level.c_str());
    if (log_level_str != NULL)
    {
        int log_level = atoi(log_level_str);
        set_log_level(log_level);
    }

    const char *configfile = getenv(key_hook_config_path.c_str());

    if (cParseFromEnv())
	{
		rawparsed = true;
	} 
	else 
	{
		hookConfig = new config();
		hookConfig->Parse(configfile);
	}
	
    envInited = true;
    bk_log(DEBUG, "finishend initEnv\n");

    return;
}

// do not call setenv, it's not thread-safe
// void setHookFlag()
// {
//     if (setenv(key_hooked_flag, "true", 1) != 0)
//     {
//         int temperr = errno;
//         bk_log(WARN, "failed to set env with key[%s] errno[%d]\n", key_hooked_flag, temperr);
//         errno = temperr;
//     }
//     else
//     {
//         bk_log(DEBUG, "succeed to set key[%s] to [true]...\n", key_hooked_flag);
//     }
// }

void checkHookFlag()
{
    if (checkedHookFlag)
    {
        return;
    }

    const char *hookflag = getenv(key_hooked_flag);
    checkedHookFlag = true;
    if (hookflag != NULL)
    {
        bk_log(DEBUG, "succeed to get key[%s] with value[%s]...\n", key_hooked_flag, hookflag);
        if (strncmp(hookflag, "true", 4) == 0)
        {
            bk_log(DEBUG, "succeed to get hooked key with [\"true\"]\n");
            needHook = false;
            
            // 如果发现上级进程已经设置过标记，则继续设置，往下传递
            // setHookFlag();
        }
    }
    else
    {
        bk_log(DEBUG, "failed to get key[%s] with parent id[%d]...\n", key_hooked_flag, getppid());
        //printEnv();
    }
}

bool endWith(const char *str, const char *end)
{
    bool result = false;

    if (str != NULL && end != NULL)
    {
        int l1 = strlen(str);
        int l2 = strlen(end);
        if (l1 >= l2)
        {
            if (strcmp(str + l1 - l2, end) == 0)
            {
                result = true;
            }
        }
    }

    return result;
}

bool isInBlack(const char *file)
{
    if (endWith(file, "sed") || endWith(file, "cat") || endWith(file, "grep") || endWith(file, "rm") || endWith(file, "mv") || endWith(file, "dirname") || endWith(file, "expr") || endWith(file, "mkdir") || endWith(file, "gawk"))
    {
        return true;
    }

    return false;
}

void init(const char *file)
{
    // // only for debug
    // if (isInBlack(file))
    // {
    //     bk_log(DEBUG, "%s in black list, do nothing...\n", file);
    //     return;
    // }
    // //

    // struct timeval now;
    // gettimeofday(&now, NULL);
    // uint64 usecs1 = (uint64)now.tv_sec * 1000 * 1000 + now.tv_usec;

    initEnv();

    // gettimeofday(&now, NULL);
    // uint64 usecs2 = (uint64)now.tv_sec * 1000 * 1000 + now.tv_usec;
    // bk_log(DEBUG, "init env spent [%lld] usec...\n", usecs2 - usecs1);

    checkHookFlag();

    // gettimeofday(&now, NULL);
    // uint64 usecs3 = (uint64)now.tv_sec * 1000 * 1000 + now.tv_usec;
    // bk_log(DEBUG, "check hook flag spent [%lld] usec...\n", usecs3 - usecs2);
}

bool getReplaceCmd(const char *src, std::string &target)
{
    bk_log(DEBUG, "ready to find target with src[%s]...\n", src);
    if (!needHook)
    {
        return false;
    }
	
	if (rawparsed)
	{
		const char *target_raw = cGetConfig(src);
		if (target_raw == NULL)
		{
			bk_log(WARN, "failed to get target from raw parse with src[%s]...\n", src);
			return false;
		}
		
		target = target_raw;
	}
	else 
	{
		if (hookConfig == NULL)
		{
			return false;
		}

		target = hookConfig->GetConfig(src);
		if (target.empty())
		{
			bk_log(WARN, "failed to get target with src[%s]...\n", src);
			return false;
		}
	}

    if (target.find(key_src_cmd) != std::string::npos)
    {
        replace_all(target, key_src_cmd, src);
    }

    // 替换命令前，设置hook标记
    // setHookFlag();

    bk_log(DEBUG, "succeed to get target[%s] with src[%s]...\n", target.c_str(), src);
    return true;
}

void printEnv()
{
    bk_log(DEBUG, "+++++++env start+++++++++++++++++++++++++++++\n");
    for (int i = 0; environ[i] != NULL; ++i)
    {
        bk_log(DEBUG, "    %s\n", environ[i]);
    }
    bk_log(DEBUG, "-------env end-------------------------------\n");
}

void printEnvArg(char *const envp[])
{
    bk_log(ERROR, "+++++++env arg start+++++++++++++++++++++++++++++\n");
    for (int i = 0; envp[i] != NULL; ++i)
    {
        bk_log(ERROR, "    %s\n", envp[i]);
    }
    bk_log(ERROR, "-------env arg end-------------------------------\n");
}

/*
#define DEFINE_WRAPPER(ret_type, name, params)  \
    typedef ret_type name##_func params;        \
    name##_func name;                           \
    ret_type name params
*/

#define DEFINE_WRAPPER(ret_type, name, params) \
    typedef ret_type name##_func params;       \
    ret_type name params

#if defined(__MACH__)
#define SILENT_CALL_REAL(name, ...)                                   \
    ({                                                                \
		name(__VA_ARGS__);                                            \
    })
#else   
#define SILENT_CALL_REAL(name, ...)                                   \
    ({                                                                \
        name##_func *real = (name##_func *)(dlsym(RTLD_NEXT, #name)); \
        real(__VA_ARGS__);                                            \
    })
#endif

static unsigned count_non_null_char_ptrs(va_list args)
{
    va_list args_copy;
    va_copy(args_copy, args);
    unsigned arg_count;
    for (arg_count = 0; va_arg(args_copy, const char *); arg_count++)
    {
        /* No need to do anything here... */
    }
    va_end(args_copy);
    return arg_count;
}

static char **malloc_argv_from(char *arg, va_list args)
{
    unsigned arg_count = count_non_null_char_ptrs(args) + 2; /*for first arg and last NULL*/
    char **argv = (char **)malloc(arg_count * sizeof(const char *));
    argv[0] = arg;
    unsigned i;
    for (i = 1; i < arg_count-1; ++i)
    {
        argv[i] = va_arg(args, char *);
    }
    argv[arg_count-1] = NULL;
    return argv;
}

// 计算envp数组长度
static int getEnvpLen(char *const envp[])
{
    int counter = 0;
    for (; envp[counter] != NULL; ++counter)
    {
    }
    // 最后一个参数是NULL
    ++counter;

    return counter;
}

// 在原有的环境变量基础上，增加新的设置
static char **getNewEnvp(char *const envp[], char *newHookFlagEnv)
{
    int counter = 0;
    for (; envp[counter] != NULL; ++counter)
    {
    }
    // 最后一个参数是NULL
    ++counter;

    int newcounter = counter + 1;
    char **newEnvp = (char **)malloc(newcounter * sizeof(const char *));
    for (int i = 0; i < counter - 1; ++i)
    {
        newEnvp[i] = envp[i];
    }
    newEnvp[counter - 1] = newHookFlagEnv;
    newEnvp[counter] = NULL;

    return newEnvp;
}

// 新的命令行参数为 arg_array + argv(去掉第一个参数(原来的命令)) + NULL
static char **getNewArgv(char *const argv[], std::vector<char *> &arg_array)
{
    size_t counter = 0;
    for (; argv[counter] != NULL; ++counter)
    {
    }
    // 最后一个参数是NULL
    //++counter;
    if (counter == 0)
    {
        counter = 1;
    }

    size_t newcounter = counter + arg_array.size();
    char **newArgv = (char **)malloc(newcounter * sizeof(const char *));
    for (size_t i = 0; i < arg_array.size(); ++i)
    {
        newArgv[i] = arg_array[i];
    }

    for (size_t i = arg_array.size(), j = 1;
         i < newcounter - 1 && j < counter;
         ++i, ++j)
    {
        newArgv[i] = argv[j];
    }
    newArgv[newcounter - 1] = NULL;

    return newArgv;
}

#if defined(__MACH__)
int hookexecve (const char *file, char *const argv[], char *const envp[]);
int hookexecvp (const char *file, char *const argv[]);
int hookexecv(const char *path, char *const argv[]);

int hookexecl(const char *path, const char *arg, ...)
{
    init(path);
    bk_log(DEBUG, "enter hook execl with path[%s]...\n", path);

    va_list args;
    va_start(args, arg);
    /* Need to cast away the constness, because execl*'s prototypes
     * are buggy -- taking ptr to const char whereas execv* take ptr
     * to const array of ptr to NON-CONST char */
    char **argv = malloc_argv_from((char *)arg, args);
    va_end(args);
    int rc = hookexecv(path, argv);
    free(argv);
    return rc;
}

int hookexeclp(const char *file, const char *arg, ...)
{
    init(file);
    bk_log(DEBUG, "enter hook execlp with file[%s]...\n", file);

    va_list args;
    va_start(args, arg);
    /* Need to cast away the constness, because execl*'s prototypes
     * are buggy -- taking ptr to const char whereas execv* take ptr
     * to const array of ptr to NON-CONST char */
    char **argv = malloc_argv_from((char *)arg, args);
    va_end(args);
    int rc = hookexecvp(file, argv);
    free(argv);
    return rc;
}

// 变参的最后一个参数是 char * const envp[]
int hookexecle(const char *path, const char *arg, ...)
{
    init(path);
    bk_log(DEBUG, "enter hook execle with path[%s]...\n", path);

    va_list args;
    va_start(args, arg);
    char **argv = malloc_argv_from((char *)arg, args);
    //ASSERT(NULL == va_arg(args, const char *));
    char *const *envp = va_arg(args, char *const *);
    va_end(args);
    int rc = hookexecve(path, argv, envp);
    free(argv);
    return rc;
}

int hookexecv(const char *path, char *const argv[])
{
    init(path);
    bk_log(DEBUG, "enter hook execv with path[%s]...\n", path);

    return hookexecve(path, argv, environ);
}

typedef int execvp_func (const char *file, char *const argv[]); 
int hookexecvp (const char *file, char *const argv[])
{
    init(file);
    bk_log(ERROR, "enter hook execvp with file[%s]...\n", file);

    printEnvArg(argv);
    std::string target = "";
    const char *src = file;
    if (getEnvpLen(argv) > 1)
    {
        src = argv[0];
    }

    if (getReplaceCmd(src, target))
    {
        //printEnvArg(newenvp);
        int rc = 0;
        std::vector<std::string> result_array;
        int size = split(target, " ", result_array);
        if (size > 1)
        {
            std::vector<char *> arg_array;
            for (size_t i = 0; i < result_array.size(); ++i)
            {
                size_t strlen = result_array[i].size();
                char *arg = new char[strlen + 1];
                strncpy(arg, result_array[i].c_str(), strlen);
                arg[strlen] = '\0';
                arg_array.push_back(arg);
            }
            char **newArgv = getNewArgv(argv, arg_array);

            bk_log(ERROR, "enter hook execve with new cmd[%s]...\n", result_array[0].c_str());
            printEnvArg(newArgv);

            // run cmd
            rc = SILENT_CALL_REAL(execvp, result_array[0].c_str(), newArgv);

            // free arg_array
            for (size_t i = 0; i < arg_array.size(); ++i)
            {
                delete[] arg_array[i];
                arg_array[i] = NULL;
            }

            // free newArgv
            free(newArgv);
        }
        else
        {
            bk_log(ERROR, "enter hook execve with new cmd[%s]...\n", result_array[0].c_str());
            rc = SILENT_CALL_REAL(execvp, target.c_str(), argv);
        }

        return rc;
    }
    else
    {
        return SILENT_CALL_REAL(execvp, file, argv);
    }
}

typedef int execve_func (const char *file, char *const argv[], char *const envp[]); 
int hookexecve (const char *file, char *const argv[], char *const envp[])
{
    init(file);
    bk_log(ERROR, "enter hook execve with file[%s]...\n", file);

    printEnvArg(argv);
    std::string target = "";
    const char *src = file;
    if (getEnvpLen(argv) > 1)
    {
        src = argv[0];
    }
    // if (getReplaceCmd(file, target))
    if (getReplaceCmd(src, target))
    {
        // printEnvArg(envp);
        size_t keyLen = strlen(full_hooked_flag_env);
        char *newHookFlagEnv = new char[keyLen + 1];
        strncpy(newHookFlagEnv, full_hooked_flag_env, keyLen);
        newHookFlagEnv[keyLen] = '\0';
        char **newenvp = getNewEnvp(envp, newHookFlagEnv);

        //printEnvArg(newenvp);
        int rc = 0;
        std::vector<std::string> result_array;
        int size = split(target, " ", result_array);
        if (size > 1)
        {
            std::vector<char *> arg_array;
            for (size_t i = 0; i < result_array.size(); ++i)
            {
                size_t strlen = result_array[i].size();
                char *arg = new char[strlen + 1];
                strncpy(arg, result_array[i].c_str(), strlen);
                arg[strlen] = '\0';
                arg_array.push_back(arg);
            }
            char **newArgv = getNewArgv(argv, arg_array);

            bk_log(ERROR, "enter hook execve with new cmd[%s]...\n", result_array[0].c_str());
            printEnvArg(newArgv);

            // run cmd
            rc = SILENT_CALL_REAL(execve, result_array[0].c_str(), newArgv, newenvp);

            // free arg_array
            for (size_t i = 0; i < arg_array.size(); ++i)
            {
                delete[] arg_array[i];
                arg_array[i] = NULL;
            }

            // free newArgv
            free(newArgv);
        }
        else
        {
            bk_log(ERROR, "enter hook execve with new cmd[%s]...\n", result_array[0].c_str());
            rc = SILENT_CALL_REAL(execve, target.c_str(), argv, newenvp);
        }

        free(newenvp);
        delete[] newHookFlagEnv;
        newHookFlagEnv = NULL;

        return rc;
    }
    else
    {
	bk_log(ERROR, "enter hook silent execve with file[%s]...\n", file);
        return SILENT_CALL_REAL(execve, file, argv, envp);
    }
}

__attribute__((used)) static struct {
    const void* replacment; 
    const void* replacee; 
} interposing_functions[] __attribute__ ((section ("__DATA,__interpose"))) = {
      {(const void*)(unsigned long)&hookexecl, (const void*)(unsigned long)&execl},
      {(const void*)(unsigned long)&hookexeclp, (const void*)(unsigned long)&execlp},
      {(const void*)(unsigned long)&hookexecle, (const void*)(unsigned long)&execle},
      {(const void*)(unsigned long)&hookexecv, (const void*)(unsigned long)&execv},
      {(const void*)(unsigned long)&hookexecvp, (const void*)(unsigned long)&execvp},
    {(const void*)(unsigned long)&hookexecve, (const void*)(unsigned long)&execve},
   };
   
#else

int execl(const char *path, const char *arg, ...)
{
    init(path);
    bk_log(DEBUG, "enter hook execl with path[%s]...\n", path);

    va_list args;
    va_start(args, arg);
    /* Need to cast away the constness, because execl*'s prototypes
     * are buggy -- taking ptr to const char whereas execv* take ptr
     * to const array of ptr to NON-CONST char */
    char **argv = malloc_argv_from((char *)arg, args);
    va_end(args);
    int rc = execv(path, argv);
    free(argv);
    return rc;
}

int execlp(const char *file, const char *arg, ...)
{
    init(file);
    bk_log(DEBUG, "enter hook execlp with file[%s]...\n", file);

    va_list args;
    va_start(args, arg);
    /* Need to cast away the constness, because execl*'s prototypes
     * are buggy -- taking ptr to const char whereas execv* take ptr
     * to const array of ptr to NON-CONST char */
    char **argv = malloc_argv_from((char *)arg, args);
    va_end(args);
    int rc = execvp(file, argv);
    free(argv);
    return rc;
}

// 变参的最后一个参数是 char * const envp[]
int execle(const char *path, const char *arg, ...)
{
    init(path);
    bk_log(DEBUG, "enter hook execle with path[%s]...\n", path);

    va_list args;
    va_start(args, arg);
    char **argv = malloc_argv_from((char *)arg, args);
    //ASSERT(NULL == va_arg(args, const char *));
    char *const *envp = va_arg(args, char *const *);
    va_end(args);
    int rc = execve(path, argv, envp);
    free(argv);
    return rc;
}

int execv(const char *path, char *const argv[])
{
    init(path);
    bk_log(DEBUG, "enter hook execv with path[%s]...\n", path);

    return execve(path, argv, environ);
}


int execvp(const char *file, char *const argv[])
{
    init(file);
    bk_log(DEBUG, "enter hook execvp with file[%s]...\n", file);
    return execvpe(file, argv, environ);
}

DEFINE_WRAPPER(int, execve, (const char *file, char *const argv[], char *const envp[]))
{
    init(file);
    bk_log(ERROR, "enter hook execve with file[%s]...\n", file);

    printEnvArg(argv);
    std::string target = "";
    const char *src = file;
    if (getEnvpLen(argv) > 1)
    {
        src = argv[0];
    }
    // if (getReplaceCmd(file, target))
    if (getReplaceCmd(src, target))
    {
        // printEnvArg(envp);
        size_t keyLen = strlen(full_hooked_flag_env);
        char *newHookFlagEnv = new char[keyLen + 1];
        strncpy(newHookFlagEnv, full_hooked_flag_env, keyLen);
        newHookFlagEnv[keyLen] = '\0';
        char **newenvp = getNewEnvp(envp, newHookFlagEnv);

        //printEnvArg(newenvp);
        int rc = 0;
        std::vector<std::string> result_array;
        int size = split(target, " ", result_array);
        if (size > 1)
        {
            std::vector<char *> arg_array;
            for (size_t i = 0; i < result_array.size(); ++i)
            {
                size_t strlen = result_array[i].size();
                char *arg = new char[strlen + 1];
                strncpy(arg, result_array[i].c_str(), strlen);
                arg[strlen] = '\0';
                arg_array.push_back(arg);
            }
            char **newArgv = getNewArgv(argv, arg_array);

            bk_log(ERROR, "enter hook execve with new cmd[%s]...\n", result_array[0].c_str());
            printEnvArg(newArgv);

            // run cmd
            rc = SILENT_CALL_REAL(execve, result_array[0].c_str(), newArgv, newenvp);

            // free arg_array
            for (size_t i = 0; i < arg_array.size(); ++i)
            {
                delete[] arg_array[i];
                arg_array[i] = NULL;
            }

            // free newArgv
            free(newArgv);
        }
        else
        {
            bk_log(ERROR, "enter hook execve with new cmd[%s]...\n", result_array[0].c_str());
            rc = SILENT_CALL_REAL(execve, target.c_str(), argv, newenvp);
        }

        free(newenvp);
        delete[] newHookFlagEnv;
        newHookFlagEnv = NULL;

        return rc;
    }
    else
    {
        return SILENT_CALL_REAL(execve, file, argv, envp);
    }
}

DEFINE_WRAPPER(int, execvpe, (const char *file, char *const argv[], char *const envp[]))
{
    init(file);
    bk_log(ERROR, "enter hook execvpe with file[%s]...\n", file);

    printEnvArg(argv);
    std::string target = "";
    const char *src = file;
    if (getEnvpLen(argv) > 1)
    {
        src = argv[0];
    }
    // if (getReplaceCmd(file, target))
    if (getReplaceCmd(src, target))
    {
        // printEnvArg(envp);
        size_t keyLen = strlen(full_hooked_flag_env);
        char *newHookFlagEnv = new char[keyLen + 1];
        strncpy(newHookFlagEnv, full_hooked_flag_env, keyLen);
        newHookFlagEnv[keyLen] = '\0';
        char **newenvp = getNewEnvp(envp, newHookFlagEnv);

        //printEnvArg(newenvp);
        int rc = 0;
        std::vector<std::string> result_array;
        int size = split(target, " ", result_array);
        if (size > 1)
        {
            std::vector<char *> arg_array;
            for (size_t i = 0; i < result_array.size(); ++i)
            {
                size_t strlen = result_array[i].size();
                char *arg = new char[strlen + 1];
                strncpy(arg, result_array[i].c_str(), strlen);
                arg[strlen] = '\0';
                arg_array.push_back(arg);
            }
            char **newArgv = getNewArgv(argv, arg_array);

            bk_log(ERROR, "enter hook execve with new cmd[%s]...\n", result_array[0].c_str());
            printEnvArg(newArgv);

            // run cmd
            rc = SILENT_CALL_REAL(execvpe, result_array[0].c_str(), newArgv, newenvp);

            // free arg_array
            for (size_t i = 0; i < arg_array.size(); ++i)
            {
                delete[] arg_array[i];
                arg_array[i] = NULL;
            }

            // free newArgv
            free(newArgv);
        }
        else
        {
            bk_log(ERROR, "enter hook execve with new cmd[%s]...\n", result_array[0].c_str());
            rc = SILENT_CALL_REAL(execvpe, target.c_str(), argv, newenvp);
        }

        free(newenvp);
        delete[] newHookFlagEnv;
        newHookFlagEnv = NULL;

        return rc;
    }
    else
    {
        return SILENT_CALL_REAL(execvpe, file, argv, envp);
    }
}
#endif