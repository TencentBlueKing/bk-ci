
#ifndef RAWPARSE_H
#define RAWPARSE_H

struct hookrule
{
	const char *src;
	bool isWildcards;
	const char *target;
	hookrule* next;
};

bool cParseFromEnv();
const char* cGetConfig(const char* src);
void printrules();

#endif