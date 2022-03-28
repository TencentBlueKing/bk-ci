
#ifndef UTIL_H
#define UTIL_H

//#include "defs.h"
#include <vector>
#include <string>

/*
int string_quote(const char *, char *, unsigned int, unsigned int,
			const char *escape_chars);
int print_quoted_string_ex(const char *, unsigned int, unsigned int,
				  const char *escape_chars);
int print_quoted_string(const char *, unsigned int, unsigned int);
int print_quoted_cstring(const char *, unsigned int);

int
umovestr_peekdata(const int pid, kernel_ulong_t addr, unsigned int len, void *laddr);

int
printpathn(const int pid, kernel_ulong_t addr, unsigned int n);

int
printpath(const int pid, kernel_ulong_t addr);
*/

int split(const std::string &source, const char *delimitor, std::vector<std::string> &result_array);
char **stringvec2argv(std::vector<std::string> &result_array);
std::string get_path(char **argv);

bool isFileExist(const char *pathname);
int replace_all(std::string &str, const std::string &pattern, const std::string &newpat);

#endif