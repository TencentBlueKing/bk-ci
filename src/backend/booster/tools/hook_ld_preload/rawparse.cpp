
#include "rawparse.h"
#include "prints.h"

#include <errno.h>
#include <fnmatch.h>
#include <stdlib.h>
#include <string.h>

const std::string key_env_hook_preload_config_content_raw = "BK_DIST_HOOK_CONFIG_CONTENT_RAW";

struct hookrule *head = NULL;

bool fnMatchPatternHasWildcards(const char *str)
{
	const unsigned int max_check_len = 10240;
	unsigned int index = 0;
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

bool cParseFromEnv()
{
	bk_log(DEBUG, "cParse for env[%s]\n", key_env_hook_preload_config_content_raw.c_str());

	const char *str = getenv(key_env_hook_preload_config_content_raw.c_str());
	if (str == NULL)
	{
		bk_log(ERROR, "failed to get json data for env [%s], errno %d\n", key_env_hook_preload_config_content_raw.c_str(), errno);
		return false;
	}

	const char *start = str;
	const char *end = str;
	char *src = NULL;
	char *target = NULL;
	hookrule* node = NULL;
	hookrule *cur = NULL;
	const char *p = str;
	int len = 0;
	while (*p)
	{
		end = p;
		switch(*p)
		{
			case ',':
			    len = end - start + 1;
				src = (char*)malloc(len);
				strncpy(src, start, end-start);
				src[len-1] = '\0';
				start = end + 1;
				break;
			case '|':
			case '\0':
				len = end - start + 1;
				target = (char*)malloc(len);
				strncpy(target, start, end-start);
				target[len-1] = '\0';
				start = end + 1;
				node = (hookrule*)malloc(sizeof(hookrule));
				node->src = src;
				node->isWildcards = fnMatchPatternHasWildcards(src);
				node->target = target;
				node->next = NULL;
				if (head == NULL) 
				{
					head = node;
					cur = node;
				}
				else
				{
					cur->next = node;
					cur = node;
				}
				break;
			default:
				break;
		}
		++p;
	}
	
	return true;
}

int min(int a1, int a2)
{
	if (a1 < a2)
	{
		return a1;
	}
	
	return a2;
}

int compare(const char *src, const char *dst)
{
        int ret = 0 ;
        while(!(ret = *(unsigned char *)src - *(unsigned char *)dst) && *dst && *src)
        {
                ++src;
                ++dst;
        }
        if ( ret < 0 )
                ret = -1 ;
        else if ( ret > 0 )
                ret = 1 ;
        return( ret );
}

const char* cGetConfig(const char* src)
{
	if (src == NULL || head == NULL)
	{
		return NULL;
	}
	
	hookrule* node = head;
	while (node != NULL)
	{
		if (node->isWildcards && fnmatch(node->src, src, 0) == 0)
		{
			return node->target;
		} else if (!node->isWildcards && compare(node->src, src) == 0)
		{
			return node->target;
		}

		node = node->next;
	}

	return NULL;
}

void printrules()
{
	hookrule* node = head;
	while (node != NULL)
	{
		bk_log(DEBUG, "src[%s] target[%s]\n", node->src, node->target);
		node = node->next;
	}
}









