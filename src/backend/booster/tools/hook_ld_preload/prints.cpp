

#include <unistd.h>

#include <errno.h>
#include <stdarg.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/timeb.h>

#include "prints.h"

#if defined(__MACH__)
#define program_invocation_name ""
#endif

int debug_print_level = 0;
void set_log_level(int level)
{
	//fflush(NULL);
	//printf("ready set log level to [%d]\n", level);
	debug_print_level = level;
	//fflush(NULL);
}

void die(void)
{
	/*
	if (strace_tracer_pid == getpid()) {
		cleanup(0);
		exit(1);
	}
	*/

	_exit(1);
}

static void
verror_msg(int err_no, const char *fmt, va_list p)
{
	char *msg;

	fflush(NULL);

	/* We want to print entire message with single fprintf to ensure
	 * message integrity if stderr is shared with other programs.
	 * Thus we use vasprintf + single fprintf.
	 */
	msg = NULL;
	if (vasprintf(&msg, fmt, p) >= 0)
	{
		if (err_no)
			fprintf(stderr, "%s: %s: %s\n",
					program_invocation_name, msg, strerror(err_no));
		else
			fprintf(stderr, "%s: %s\n",
					program_invocation_name, msg);
		free(msg);
	}
	else
	{
		/* malloc in vasprintf failed, try it without malloc */
		fprintf(stderr, "%s: ", program_invocation_name);
		vfprintf(stderr, fmt, p);
		if (err_no)
			fprintf(stderr, ": %s\n", strerror(err_no));
		else
			putc('\n', stderr);
	}
	/* We don't switch stderr to buffered, thus fprintf(stderr)
	 * always flushes its output and this is not necessary: */
	/* fflush(stderr); */
}

static void
vbk_msg(const char *fmt, va_list p)
{
	struct timeb tp;
	char ti[32];
	ftime(&tp);
	strftime(ti, sizeof(ti), "%Y-%m-%d %H:%M:%S", localtime(&tp.time));

	char *msg;
	fflush(NULL);
	msg = NULL;
	if (vasprintf(&msg, fmt, p) >= 0)
	{
		fprintf(stderr, "[%d] %s %03u   %s", getpid(), ti, tp.millitm, msg);
		free(msg);
	}
}

void error_msg(const char *fmt, ...)
{
	va_list p;
	va_start(p, fmt);
	verror_msg(0, fmt, p);
	va_end(p);
}

void bk_log(int level, const char *fmt, ...)
{
	if (level <= debug_print_level)
	{
		va_list p;
		va_start(p, fmt);
		vbk_msg(fmt, p);
		va_end(p);
	}
}

void error_msg_and_die(const char *fmt, ...)
{
	va_list p;
	va_start(p, fmt);
	verror_msg(0, fmt, p);
	va_end(p);
	die();
}

void error_msg_and_help(const char *fmt, ...)
{
	if (fmt != NULL)
	{
		va_list p;
		va_start(p, fmt);
		verror_msg(0, fmt, p);
		va_end(p);
	}
	fprintf(stderr, "Try '%s -h' for more information.\n",
			program_invocation_name);
	die();
}

void perror_msg(const char *fmt, ...)
{
	va_list p;
	va_start(p, fmt);
	verror_msg(errno, fmt, p);
	va_end(p);
}

void perror_msg_and_die(const char *fmt, ...)
{
	va_list p;
	va_start(p, fmt);
	verror_msg(errno, fmt, p);
	va_end(p);
	die();
}