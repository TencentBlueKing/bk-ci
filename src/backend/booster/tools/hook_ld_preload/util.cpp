//#include "defs.h"
#include "util.h"

//#include <sys/ptrace.h>
#include <sys/stat.h>

#include <errno.h>
#include <limits.h>
//#include <paths.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>

#define __STDC_FORMAT_MACROS
#include <inttypes.h>

//#include "print_utils.h"
#include "prints.h"

#if 0
void
printaddr(const uint64_t addr)
{
	if (!addr)
		bk_log(DEBUG, "NULL");
	else
		bk_log(DEBUG, "%#"PRIx64, addr);
}

int
string_quote(const char *instr, char *outstr, const unsigned int size,
	     const unsigned int style, const char *escape_chars)
{
	const unsigned char *ustr = (const unsigned char *) instr;
	char *s = outstr;
	unsigned int i;
	int usehex, c, eol;
	bool printable;

	if (style & QUOTE_0_TERMINATED)
		eol = '\0';
	else
		eol = 0x100; 

	usehex = 0;

	if (style & QUOTE_EMIT_COMMENT)
		s = stpcpy(s, " /* ");
	if (!(style & QUOTE_OMIT_LEADING_TRAILING_QUOTES))
		*s++ = '\"';

	if (usehex) {
		for (i = 0; i < size; ++i) {
			c = ustr[i];
			if (c == eol)
				goto asciz_ended;
			*s++ = '\\';
			*s++ = 'x';
			s = sprint_byte_hex(s, c);
		}

		goto string_ended;
	}

	for (i = 0; i < size; ++i) {
		c = ustr[i];
		if (c == eol)
			goto asciz_ended;
		if ((i == (size - 1)) &&
		    (style & QUOTE_OMIT_TRAILING_0) && (c == '\0'))
			goto asciz_ended;
		switch (c) {
		case '\"': case '\\':
			*s++ = '\\';
			*s++ = c;
			break;
		case '\f':
			*s++ = '\\';
			*s++ = 'f';
			break;
		case '\n':
			*s++ = '\\';
			*s++ = 'n';
			break;
		case '\r':
			*s++ = '\\';
			*s++ = 'r';
			break;
		case '\t':
			*s++ = '\\';
			*s++ = 't';
			break;
		case '\v':
			*s++ = '\\';
			*s++ = 'v';
			break;
		default:
			printable = is_print(c);

			if (printable && escape_chars)
				printable = !strchr(escape_chars, c);

			if (printable) {
				*s++ = c;
			} else {
				
				*s++ = '\\';
				if (i + 1 < size
				    && ustr[i + 1] >= '0'
				    && ustr[i + 1] <= '7'
				) {
					
					*s++ = '0' + (c >> 6);
					*s++ = '0' + ((c >> 3) & 0x7);
				} else {
					
					if ((c >> 3) != 0) {
						if ((c >> 6) != 0)
							*s++ = '0' + (c >> 6);
						*s++ = '0' + ((c >> 3) & 0x7);
					}
				}
				*s++ = '0' + (c & 0x7);
			}
		}
	}

 string_ended:
	if (!(style & QUOTE_OMIT_LEADING_TRAILING_QUOTES))
		*s++ = '\"';
	if (style & QUOTE_EMIT_COMMENT)
		s = stpcpy(s, " */");
	*s = '\0';

	if (style & QUOTE_0_TERMINATED && ustr[i] == '\0') {
		return 0;
	}

	return 1;

 asciz_ended:
	if (!(style & QUOTE_OMIT_LEADING_TRAILING_QUOTES))
		*s++ = '\"';
	if (style & QUOTE_EMIT_COMMENT)
		s = stpcpy(s, " */");
	*s = '\0';
	/* Return zero: we printed entire ASCIZ string (didn't truncate it) */
	return 0;
}

#ifndef ALLOCA_CUTOFF
#define ALLOCA_CUTOFF 4032
#endif
#define use_alloca(n) ((n) <= ALLOCA_CUTOFF)

int
print_quoted_string_ex(const char *str, unsigned int size,
		       const unsigned int style, const char *escape_chars)
{
	char *buf;
	char *outstr;
	unsigned int alloc_size;
	int rc;

	if (size && style & QUOTE_0_TERMINATED)
		--size;

	alloc_size = 4 * size;
	if (alloc_size / 4 != size) {
		error_func_msg("requested %u bytes exceeds %u bytes limit",
			       size, -1U / 4);
		bk_log(DEBUG, "???");
		return -1;
	}
	alloc_size += 1 + (style & QUOTE_OMIT_LEADING_TRAILING_QUOTES ? 0 : 2) +
		(style & QUOTE_EMIT_COMMENT ? 7 : 0);

	if (use_alloca(alloc_size)) {
		outstr = (char*)(alloca(alloc_size));
		buf = NULL;
	} else {
		outstr = buf = (char*)(malloc(alloc_size));
		if (!buf) {
			error_func_msg("memory exhausted when tried to allocate"
				       " %u bytes", alloc_size);
			bk_log(DEBUG, "???");
			return -1;
		}
	}

	rc = string_quote(str, outstr, size, style, escape_chars);
	bk_log(DEBUG, outstr);

	free(buf);
	return rc;
}

inline int
print_quoted_string(const char *str, unsigned int size,
		    const unsigned int style)
{
	return print_quoted_string_ex(str, size, style, NULL);
}

int
print_quoted_cstring(const char *str, unsigned int size)
{
	//bk_log(DEBUG, "print_quoted_cstring\n");
	
	int unterminated =
		print_quoted_string(str, size, QUOTE_0_TERMINATED);

	if (unterminated)
		bk_log(DEBUG, "...");

	return unterminated;
}

int
umovestr_peekdata(const int pid, kernel_ulong_t addr, unsigned int len, void *laddr)
{
	unsigned int nread = 0;
	unsigned int residue = addr & (sizeof(long) - 1);
	void *orig_addr = laddr;

	while (len) {
		addr &= -sizeof(long);		

		errno = 0;
		union {
			unsigned long val;
			char x[sizeof(long)];
		//} u = { .val = (unsigned long)ptrace(PTRACE_PEEKDATA, pid, addr, 0) };
		} u;
		u.val = (unsigned long)ptrace(PTRACE_PEEKDATA, pid, addr, 0) ;


		switch (errno) {
			case 0:
				break;
			case ESRCH: case EINVAL:
				
				return -1;
			case EFAULT: case EIO: case EPERM:
				
				if (nread) {
					perror_func_msg("short read (%d < %d)"
							" @0x%" PRI_klx,
							nread, nread + len,
							addr - nread);
				}
				return -1;
			default:
				
				perror_func_msg("pid:%d @0x%" PRI_klx, pid, addr);
				return -1;
		}

		unsigned int m = MIN(sizeof(long) - residue, len);
		memcpy(laddr, &u.x[residue], m);
		while (residue < sizeof(long))
			if (u.x[residue++] == '\0')
				return ((char*)laddr - (char*)orig_addr) + residue;
				//return residue;
		residue = 0;
		addr += sizeof(long);
		laddr = (char*)laddr + m;
		nread += m;
		len -= m;
	}

	return 0;
}

int
printpathn(const int pid, const kernel_ulong_t addr, unsigned int n)
{
	char path[PATH_MAX];
	int nul_seen;

	if (!addr) {
		bk_log(DEBUG, "NULL");
		return -1;
	}

	if (n > sizeof(path) - 1)
		n = sizeof(path) - 1;

	//nul_seen = umovestr(tcp, addr, n + 1, path);
	nul_seen = umovestr_peekdata(pid, addr, n + 1, path);
	if (nul_seen < 0) {
		printaddr(addr);
	}
	else {
		//path[n++] = !nul_seen;
		path[nul_seen] = '\0';
		print_quoted_cstring(path, n);
	}

	return nul_seen;
}

int
printpath(const int pid, const kernel_ulong_t addr)
{
	return printpathn(pid, addr, PATH_MAX - 1);
}
#endif

int split(const std::string &source, const char *delimitor, std::vector<std::string> &result_array)
{
	if (delimitor == NULL)
		return 0;

	result_array.clear();

	std::string::size_type startPos = 0;
	bool reachEnd = false;
	while (!reachEnd)
	{
		std::string::size_type curPos = source.find(delimitor, startPos);
		if (curPos != std::string::npos)
		{
			if (curPos != startPos)
			{
				result_array.push_back(source.substr(startPos, curPos - startPos));
			}

			startPos = curPos + 1;
		}
		else
		{
			// add the last part
			if (startPos < source.length())
				result_array.push_back(source.substr(startPos));

			reachEnd = true;
		}
	}

	return result_array.size();
}

// 需要追加一个NULL参数
char **stringvec2argv(std::vector<std::string> &result_array)
{
	if (result_array.empty())
	{
		return NULL;
	}

	int argc = result_array.size() + 1;
	char **argv = new char *[argc];
	for (size_t i = 0; i < result_array.size(); ++i)
	{
		int strlen = result_array[i].size();
		argv[i] = new char[strlen + 1];
		bk_log(DEBUG, "copy [%s] now\n", result_array[i].c_str());
		strncpy(argv[i], result_array[i].c_str(), strlen);
		argv[i][strlen] = '\0';
		bk_log(DEBUG, "argv[%d]=[%s]\n", i, argv[i]);
	}
	argv[argc - 1] = NULL;
	bk_log(DEBUG, "argv[%d]=NULL\n", argc - 1);

	return argv;
}
std::string get_path(char **argv)
{
	struct stat64 statbuf;
	size_t filename_len;
	char pathname[PATH_MAX];
	const char *filename = argv[0];
	bk_log(DEBUG, "get_path for [%s]\n", filename);
	filename_len = strlen(filename);
	if (strchr(filename, '/'))
	{
		strcpy(pathname, filename);
	}
	else
	{
		const char *path;
		size_t m, n, len;

		for (path = getenv("PATH"); path && *path; path += m)
		{
			const char *colon = strchr(path, ':');
			if (colon)
			{
				n = colon - path;
				m = n + 1;
			}
			else
				m = n = strlen(path);
			if (n == 0)
			{
				if (!getcwd(pathname, PATH_MAX))
					continue;
				len = strlen(pathname);
			}
			else if (n > sizeof(pathname) - 1)
				continue;
			else
			{
				strncpy(pathname, path, n);
				len = n;
			}
			if (len && pathname[len - 1] != '/')
				pathname[len++] = '/';
			if (filename_len + len > sizeof(pathname) - 1)
				continue;
			strcpy(pathname + len, filename);
			bk_log(DEBUG, "stat [%s] now\n", pathname);
			if (stat64(pathname, &statbuf) == 0 &&
				/* Accept only regular files
			       with some execute bits set.
			       XXX not perfect, might still fail */
				S_ISREG(statbuf.st_mode) &&
				(statbuf.st_mode & 0111))
				break;
		}
		if (!path || !*path)
			pathname[0] = '\0';
	}
	if (stat64(pathname, &statbuf) < 0)
	{
		perror_msg_and_die("Can't stat '%s'", filename);
	}

	return (std::string)pathname;
}

bool isFileExist(const char *pathname)
{
	struct stat buf;
	int err = ::stat(pathname, &buf);
	if (err == 0)
		return true;
	if (errno == 2)
		return false;

	return true;
}

int replace_all(std::string &str, const std::string &pattern, const std::string &newpat)
{
	int count = 0;
	const size_t nsize = newpat.size();
	const size_t psize = pattern.size();

	for (size_t pos = str.find(pattern, 0);
		 pos != std::string::npos;
		 pos = str.find(pattern, pos + nsize))
	{
		str.replace(pos, psize, newpat);
		count++;
	}

	return count;
}