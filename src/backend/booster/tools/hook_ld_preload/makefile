CFLAGS:=-Wall -g -fPIC -D_GNU_SOURCE
CPPFLAGS:=-Wall -g -MD -fPIC -D_GNU_SOURCE -DDEBUG_HOOK

srcdir=./
objdir=./

src:=$(wildcard $(srcdir)*.cpp)
obj:=$(patsubst %.cpp, %.o, $(src))
dep:=$(patsubst %.cpp, %.d, $(src))


jsondir=json/
jsonsrc1:=$(wildcard $(jsondir)*.cpp)
jsonobj1:=$(patsubst %.cpp, %.o, $(jsonsrc1))
jsonsrc2:=$(wildcard $(jsondir)*.c)
jsonobj2:=$(patsubst %.c, %.o, $(jsonsrc2))

hookdir=hook_lib/
hooksrc:=$(wildcard $(hookdir)*.cpp)
hookobj:=$(patsubst %.cpp, %.o, $(hooksrc))

GPP=g++
GCC=gcc

CXX=${GPP}
CC=${GCC}

all: $(objdir)bkhook.so

deps: $(dep)

$(objdir)bkhook.so : $(hookobj) rawparse.o config.o prints.o util.o $(jsonobj1) $(jsonobj2)
	${CXX} $^ -o $@ -Winit-self -shared -ldl

clean: 
	find ./ -name "*.o" | xargs rm 
	find ./ -name "*.d" | xargs rm 
	find ./ -name "*.so" | xargs rm 

-include $(dep)

rebuild: clean all