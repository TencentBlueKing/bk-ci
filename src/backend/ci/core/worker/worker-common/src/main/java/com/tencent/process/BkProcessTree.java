/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.process;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.tencent.process.ProcessTreeRemoting.IOSProcess;
import com.tencent.process.ProcessTreeRemoting.IProcessTree;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jvnet.winp.WinProcess;
import org.jvnet.winp.WinpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public abstract class BkProcessTree implements Iterable<BkProcessTree.OSProcess>, IProcessTree, Serializable {
    protected final Map<Integer, BkProcessTree.OSProcess> processes;
    private transient volatile List<ProcessKiller> killers;
    private static final boolean IS_LITTLE_ENDIAN = "little".equals(System.getProperty("sun.cpu.endian"));
    public static boolean enabled = !Boolean.getBoolean(BkProcessTree.class.getName() + ".disable");
    private static Logger logger = LoggerFactory.getLogger(BkProcessTree.class);

    private BkProcessTree() {
        this.processes = new HashMap<>();
    }

    public final BkProcessTree.OSProcess get(int pid) {
        return this.processes.get(pid);
    }

    public final Iterator<BkProcessTree.OSProcess> iterator() {
        return this.processes.values().iterator();
    }

    public abstract BkProcessTree.OSProcess get(Process var1);

    public abstract void killAll(Map<String, String> var1, boolean forceFlag) throws InterruptedException;

    public static void log(String msg) {
        logger.info(msg);
    }

    public void killAll(Process proc, Map<String, String> modelEnvVars, boolean forceFlag) throws InterruptedException {
        log("killAll: process=" + proc + " and envs=" + modelEnvVars);
        BkProcessTree.OSProcess p = this.get(proc);
        if (p != null) {
            p.killRecursively(forceFlag);
        }

        if (modelEnvVars != null) {
            this.killAll(modelEnvVars, forceFlag);
        }

    }

    final List<ProcessKiller> getKillers() throws InterruptedException {
        if (this.killers == null) {
            this.killers = Collections.emptyList();
        }

        return this.killers;
    }

    public static void log(String msg, Throwable e) {
        logger.error(msg, e);
    }

    public static BkProcessTree get() {
        try {
            if (File.pathSeparatorChar == ';') {
                return new BkProcessTree.Windows();
            }

            String os = Util.fixNull(System.getProperty("os.name"));
            if (os.equals("Linux")) {
                return new BkProcessTree.Linux();
            }

            if (os.equals("SunOS")) {
                return new BkProcessTree.Solaris();
            }

            if (os.equals("Mac OS X")) {
                return new BkProcessTree.Darwin();
            }
        } catch (Exception var1) {
            log("Failed to load winp. Reverting to the default", var1);
        }

        return new BkProcessTree.Default();
    }

    public abstract static class Local extends BkProcessTree {
        Local() {
            super();
        }
    }

    private static class Default extends BkProcessTree.Local {
        public BkProcessTree.OSProcess get(final Process proc) {
            return new BkProcessTree.OSProcess(-1) {
                public BkProcessTree.OSProcess getParent() {
                    return null;
                }

                public void killRecursively(boolean forceFlag) {
                    proc.destroy();
                }

                public void kill(boolean forceFlag) throws InterruptedException {
                    proc.destroy();
                    this.killByKiller();
                }

                public List<String> getArguments() {
                    return Collections.emptyList();
                }

                public EnvVars getEnvironmentVariables() {
                    return new EnvVars();
                }
            };
        }

        public void killAll(Map<String, String> modelEnvVars, boolean forceFlag) {
        }
    }

    private static class Darwin extends BkProcessTree.Unix {
        private final int sizeOf_kinfo_proc;
        private static final int sizeOf_kinfo_proc_32 = 492;
        private static final int sizeOf_kinfo_proc_64 = 648;
        private final int kinfo_proc_pid_offset;
        private static final int kinfo_proc_pid_offset_32 = 24;
        private static final int kinfo_proc_pid_offset_64 = 40;
        private final int kinfo_proc_ppid_offset;
        private static final int kinfo_proc_ppid_offset_32 = 416;
        private static final int kinfo_proc_ppid_offset_64 = 560;
        private static final int sizeOfInt;
        private static final int CTL_KERN = 1;
        private static final int KERN_PROC = 14;
        private static final int KERN_PROC_ALL = 0;
        private static final int ENOMEM = 12;
        private static int[] MIB_PROC_ALL;
        private static final int KERN_ARGMAX = 8;
        private static final int KERN_PROCARGS2 = 49;

        Darwin() {
            String arch = System.getProperty("sun.arch.data.model");
            if ("64".equals(arch)) {
                this.sizeOf_kinfo_proc = 648;
                this.kinfo_proc_pid_offset = 40;
                this.kinfo_proc_ppid_offset = 560;
            } else {
                this.sizeOf_kinfo_proc = 492;
                this.kinfo_proc_pid_offset = 24;
                this.kinfo_proc_ppid_offset = 416;
            }

            try {
                IntByReference defaultSize = new IntByReference(sizeOfInt);
                IntByReference size = new IntByReference(sizeOfInt);
                int var5 = 0;

                do {
                    if (GNUCLibrary.LIBC.sysctl(MIB_PROC_ALL, 3, Pointer.NULL, size, Pointer.NULL, defaultSize) != 0) {
                        throw new IOException("Failed to obtain memory requirement: " + GNUCLibrary.LIBC.strerror(Native.getLastError()));
                    }

                    Memory m = new Memory((long)size.getValue());
                    if (GNUCLibrary.LIBC.sysctl(MIB_PROC_ALL, 3, m, size, Pointer.NULL, defaultSize) == 0) {
                        int count = size.getValue() / this.sizeOf_kinfo_proc;
                        log("Found " + count + " processes");

                        for(int base = 0; base < size.getValue(); base += this.sizeOf_kinfo_proc) {
                            int pid = m.getInt((long)(base + this.kinfo_proc_pid_offset));
                            int ppid = m.getInt((long)(base + this.kinfo_proc_ppid_offset));
                            super.processes.put(pid, new BkProcessTree.Darwin.DarwinProcess(pid, ppid));
                        }

                        return;
                    }
                } while(Native.getLastError() == 12 && var5++ < 16);

                throw new IOException("Failed to call kern.proc.all: " + GNUCLibrary.LIBC.strerror(Native.getLastError()));
            } catch (IOException var10) {
                log("Failed to obtain process list", var10);
            }
        }

        static {
            sizeOfInt = Native.getNativeSize(Integer.TYPE);
            MIB_PROC_ALL = new int[]{1, 14, 0};
        }

        private class DarwinProcess extends BkProcessTree.UnixProcess {
            private final int ppid;
            private EnvVars envVars;
            private List<String> arguments;

            DarwinProcess(int pid, int ppid) {
                super(pid);
                this.ppid = ppid;
            }

            public BkProcessTree.OSProcess getParent() {
                return Darwin.this.get(this.ppid);
            }

            public synchronized EnvVars getEnvironmentVariables() {
                if (this.envVars == null) {
                    this.parse();
                }
                return this.envVars;
            }

            public List<String> getArguments() {
                if (this.arguments == null) {
                    this.parse();
                }
                return this.arguments;
            }

            private void parse() {
                try {
                    this.arguments = new ArrayList<>();
                    this.envVars = new EnvVars();
                    IntByReference defaultSize = new IntByReference();
                    IntByReference argmaxRef = new IntByReference(0);
                    IntByReference size = new IntByReference(BkProcessTree.Darwin.sizeOfInt);
                    if (GNUCLibrary.LIBC.sysctl(new int[]{1, 8}, 2, argmaxRef.getPointer(), size, Pointer.NULL, defaultSize) != 0) {
                        throw new IOException("Failed to get kernl.argmax: " + GNUCLibrary.LIBC.strerror(Native.getLastError()));
                    }

                    int argmax = argmaxRef.getValue();

                    class StringArrayMemory extends Memory {
                        private long offset = 0L;

                        StringArrayMemory(long l) {
                            super(l);
                        }

                        int readInt() {
                            int r = this.getInt(this.offset);
                            this.offset += (long) BkProcessTree.Darwin.sizeOfInt;
                            return r;
                        }

                        byte peek() {
                            return this.getByte(this.offset);
                        }

                        String readString() {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();

                            byte ch;
                            while((ch = this.getByte((long)(this.offset++))) != 0) {
                                baos.write(ch);
                            }

                            return baos.toString();
                        }

                        void skip0() {
                            while(this.getByte(this.offset) == 0) {
                                ++this.offset;
                            }

                        }
                    }

                    StringArrayMemory m = new StringArrayMemory((long)argmax);
                    size.setValue(argmax);
                    if (GNUCLibrary.LIBC.sysctl(new int[]{1, 49, this.pid}, 3, m, size, Pointer.NULL, defaultSize) != 0) {
                        throw new IOException("Failed to obtain ken.procargs2: " + GNUCLibrary.LIBC.strerror(Native.getLastError()));
                    }

                    int argc = m.readInt();
                    String args0 = m.readString();
                    m.skip0();

                    try {
                        for(int i = 0; i < argc; ++i) {
                            this.arguments.add(m.readString());
                        }
                    } catch (IndexOutOfBoundsException var9) {
                        throw new IllegalStateException("Failed to parse arguments: pid=" + this.pid + ", arg0=" + args0 + ", arguments=" + this.arguments + ", nargs=" + argc + ". Please run 'ps e " + this.pid + "' and report this to https://issues.jenkins-ci.org/browse/JENKINS-9634", var9);
                    }

                    while(m.peek() != 0) {
                        this.envVars.addLine(m.readString());
                    }
                } catch (IOException var10) {
                    log(var10.getMessage());
                }

            }
        }
    }

    static class Solaris extends BkProcessTree.ProcfsUnix {
        Solaris() {
        }

        protected BkProcessTree.OSProcess createProcess(int pid) throws IOException {
            return new BkProcessTree.Solaris.SolarisProcess(pid);
        }

        private static long to64(int i) {
            return (long)i & 4294967295L;
        }

        private static int adjust(int i) {
            return BkProcessTree.IS_LITTLE_ENDIAN ? i << 24 | i << 8 & 16711680 | i >> 8 & '\uff00' | i >>> 24 : i;
        }

        private class SolarisProcess extends BkProcessTree.UnixProcess {
            private final int ppid;
            private final int envp;
            private final int argp;
            private final int argc;
            private EnvVars envVars;
            private List<String> arguments;

            private SolarisProcess(int pid) throws IOException {
                super(pid);

                try (RandomAccessFile psinfo = new RandomAccessFile(this.getFile("psinfo"), "r")) {
                    psinfo.seek(8L);
                    if (Solaris.adjust(psinfo.readInt()) != pid) {
                        throw new IOException("psinfo PID mismatch");
                    }

                    this.ppid = Solaris.adjust(psinfo.readInt());
                    psinfo.seek(188L);
                    this.argc = Solaris.adjust(psinfo.readInt());
                    this.argp = Solaris.adjust(psinfo.readInt());
                    this.envp = Solaris.adjust(psinfo.readInt());
                }

                if (this.ppid == -1) {
                    throw new IOException("Failed to parse PPID from /proc/" + pid + "/status");
                }
            }

            public BkProcessTree.OSProcess getParent() {
                return Solaris.this.get(this.ppid);
            }

            public synchronized List<String> getArguments() {
                if (this.arguments == null) {
                    this.arguments = new ArrayList<>(this.argc);

                    try {

                        try (RandomAccessFile as = new RandomAccessFile(this.getFile("as"), "r")) {
                            BkProcessTree.log("Reading " + this.getFile("as"));
                            for (int n = 0; n < this.argc; ++n) {
                                as.seek(Solaris.to64(this.argp + n * 4));
                                int p = Solaris.adjust(as.readInt());
                                this.arguments.add(this.readLine(as, p, "argv[" + n + "]"));
                            }
                        }
                    } catch (IOException var8) {
                        log(var8.getMessage());
                    }

                    this.arguments = Collections.unmodifiableList(this.arguments);
                }
                return this.arguments;
            }

            public synchronized EnvVars getEnvironmentVariables() {
                if (this.envVars == null) {
                    this.envVars = new EnvVars();

                    try {

                        try (RandomAccessFile as = new RandomAccessFile(this.getFile("as"), "r")) {
                            BkProcessTree.log("Reading " + this.getFile("as"));
                            int n = 0;

                            while (true) {
                                as.seek(Solaris.to64(this.envp + n * 4));
                                int p = Solaris.adjust(as.readInt());
                                if (p == 0) {
                                    break;
                                }

                                this.envVars.addLine(this.readLine(as, p, "env[" + n + "]"));
                                ++n;
                            }
                        }
                    } catch (IOException var8) {
                        log(var8.getMessage());
                    }

                }
                return this.envVars;
            }

            private String readLine(RandomAccessFile as, int p, String prefix) throws IOException {
                BkProcessTree.log("Reading " + prefix + " at " + p);
                as.seek(BkProcessTree.Solaris.to64(p));
                ByteArrayOutputStream buf = new ByteArrayOutputStream();

                int ch;
                for(int i = 0; (ch = as.read()) > 0; buf.write(ch)) {
                    ++i;
                    if (i % 100 == 0) {
                        BkProcessTree.log(prefix + " is so far " + buf.toString());
                    }
                }

                String line = buf.toString();
                BkProcessTree.log(prefix + " was " + line);
                return line;
            }
        }
    }

    static class Linux extends BkProcessTree.ProcfsUnix {
        Linux() {
        }

        protected BkProcessTree.Linux.LinuxProcess createProcess(int pid) throws IOException {
            return new BkProcessTree.Linux.LinuxProcess(pid);
        }

        public byte[] readFileToByteArray(File file) throws IOException {
            FileInputStream in = FileUtils.openInputStream(file);

            byte[] var3;
            try {
                var3 = IOUtils.toByteArray(in);
            } finally {
                in.close();
            }

            return var3;
        }

        class LinuxProcess extends BkProcessTree.UnixProcess {
            private int ppid = -1;
            private EnvVars envVars;
            private List<String> arguments;

            LinuxProcess(int pid) throws IOException {
                super(pid);

                try (BufferedReader r = new BufferedReader(new FileReader(this.getFile("status")))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.toLowerCase(Locale.ENGLISH);
                        if (line.startsWith("ppid:")) {
                            this.ppid = Integer.parseInt(line.substring(5).trim());
                            break;
                        }
                    }
                }

                if (this.ppid == -1) {
                    throw new IOException("Failed to parse PPID from /proc/" + pid + "/status");
                }
            }

            public BkProcessTree.OSProcess getParent() {
                return Linux.this.get(this.ppid);
            }

            public synchronized List<String> getArguments() {
                if (this.arguments == null) {
                    this.arguments = new ArrayList<>();

                    try {
                        byte[] cmdline = Linux.this.readFileToByteArray(this.getFile("cmdline"));
                        int pos = 0;

                        for (int i = 0; i < cmdline.length; ++i) {
                            byte b = cmdline[i];
                            if (b == 0) {
                                this.arguments.add(new String(cmdline, pos, i - pos));
                                pos = i + 1;
                            }
                        }
                    } catch (IOException var5) {
                        log(var5.getMessage());
                    }

                    this.arguments = Collections.unmodifiableList(this.arguments);
                }
                return this.arguments;
            }

            public synchronized EnvVars getEnvironmentVariables() {
                if (this.envVars == null) {
                    this.envVars = new EnvVars();

                    try {
                        byte[] environ = Linux.this.readFileToByteArray(this.getFile("environ"));
                        int pos = 0;

                        for (int i = 0; i < environ.length; ++i) {
                            byte b = environ[i];
                            if (b == 0) {
                                this.envVars.addLine(new String(environ, pos, i - pos));
                                pos = i + 1;
                            }
                        }
                    } catch (IOException var5) {
                        log(var5.getMessage());
                    }

                }
                return this.envVars;
            }
        }
    }

    private static final class UnixReflection {
        private static final Field PID_FIELD;
        private static final Method DESTROY_PROCESS;

        private UnixReflection() {
        }

        public static void destroy(int pid, boolean forceFlag) throws IllegalAccessException, InvocationTargetException {
            if (isPreJava8()) {
                DESTROY_PROCESS.invoke((Object)null, pid);
            } else {
                DESTROY_PROCESS.invoke((Object)null, pid, forceFlag);
            }

        }

        private static boolean isPreJava8() {
            int javaVersionAsAnInteger = Integer.parseInt(System.getProperty("java.version").replaceAll("\\.", "").replaceAll("_", "").substring(0, 2));
            return javaVersionAsAnInteger < 18;
        }

        static {
            LinkageError x;
            try {
                Class<?> clazz = Class.forName("java.lang.UNIXProcess");
                PID_FIELD = clazz.getDeclaredField("pid");
                PID_FIELD.setAccessible(true);
                if (isPreJava8()) {
                    DESTROY_PROCESS = clazz.getDeclaredMethod("destroyProcess", Integer.TYPE);
                } else {
                    DESTROY_PROCESS = clazz.getDeclaredMethod("destroyProcess", Integer.TYPE, Boolean.TYPE);
                }

                DESTROY_PROCESS.setAccessible(true);
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
                x = new LinkageError();
                x.initCause(e);
                throw x;
            }
        }
    }

    public abstract class UnixProcess extends BkProcessTree.OSProcess {
        protected UnixProcess(int pid) {
            super(pid);
        }

        protected final File getFile(String relativePath) {
            return new File(new File("/proc/" + this.getPid()), relativePath);
        }

        public void kill(boolean forceFlag) throws InterruptedException {
            try {
                int pid = this.getPid();
                BkProcessTree.log("Killing pid=" + pid);
                BkProcessTree.UnixReflection.destroy(pid, forceFlag);
            } catch (IllegalAccessException var3) {
                IllegalAccessError x = new IllegalAccessError();
                x.initCause(var3);
                throw x;
            } catch (InvocationTargetException var4) {
                if (var4.getTargetException() instanceof Error) {
                    throw (Error)var4.getTargetException();
                }

                BkProcessTree.log("Failed to terminate pid=" + this.getPid(), var4);
            }

            this.killByKiller();
        }

        public void killRecursively(boolean forceFlag) throws InterruptedException {
            BkProcessTree.log("Recursively killing pid=" + this.getPid());

            for (OSProcess p : this.getChildren()) {
                p.killRecursively(forceFlag);
            }

            this.kill(forceFlag);
        }

        public abstract List<String> getArguments();
    }

    abstract static class ProcfsUnix extends BkProcessTree.Unix {
        ProcfsUnix() {
            File[] processes = (new File("/proc")).listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory();
                }
            });
            if (processes == null) {
                log("No /proc");
            } else {
                for (File p : processes) {
                    int pid;
                    try {
                        pid = Integer.parseInt(p.getName());
                    } catch (NumberFormatException var9) {
                        continue;
                    }

                    try {
                        this.processes.put(pid, this.createProcess(pid));
                    } catch (IOException var8) {
                        log(var8.getMessage());
                    }
                }

            }
        }

        protected abstract BkProcessTree.OSProcess createProcess(int var1) throws IOException;
    }

    abstract static class Unix extends BkProcessTree.Local {
        Unix() {
        }

        public BkProcessTree.OSProcess get(Process proc) {
            try {
                return this.get((Integer) BkProcessTree.UnixReflection.PID_FIELD.get(proc));
            } catch (IllegalAccessException var4) {
                IllegalAccessError x = new IllegalAccessError();
                x.initCause(var4);
                throw x;
            }
        }

        public void killAll(Map<String, String> modelEnvVars, boolean forceFlag) throws InterruptedException {

            for (OSProcess p : this) {
                if (p.hasMatchingEnvVars(modelEnvVars)) {
                    p.killRecursively(forceFlag);
                }
            }

        }
    }

    private static final class Windows extends BkProcessTree.Local {
        Windows() {

            for (WinProcess p : WinProcess.all()) {
                int pid = p.getPid();
                if (pid != 0 && pid != 4) {
                    super.processes.put(pid, new OSProcess(pid) {
                        private EnvVars env;
                        private List<String> args;

                        public OSProcess getParent() {
                            return null;
                        }

                        public void killRecursively(boolean forceFlag) throws InterruptedException {
                            BkProcessTree.log("Killing recursively " + this.getPid());
                            p.killRecursively();
                            this.killByKiller();
                        }

                        public void kill(boolean forceFlag) throws InterruptedException {
                            BkProcessTree.log("Killing " + this.getPid());
                            p.kill();
                            this.killByKiller();
                        }

                        public synchronized List<String> getArguments() {
                            if (this.args == null) {
                                this.args = Arrays.asList(QuotedStringTokenizer.tokenize(p.getCommandLine()));
                            }

                            return this.args;
                        }

                        public synchronized EnvVars getEnvironmentVariables() {
                            if (this.env == null) {
                                this.env = new EnvVars();

                                try {
                                    this.env.putAll(p.getEnvironmentVariables());
                                } catch (WinpException var2) {
                                    log(var2.getMessage());
                                }

                            }
                            return this.env;
                        }
                    });
                }
            }

        }

        public BkProcessTree.OSProcess get(Process proc) {
            return this.get((new WinProcess(proc)).getPid());
        }

        public void killAll(Map<String, String> modelEnvVars, boolean forceFlag) throws InterruptedException {
            Iterator<BkProcessTree.OSProcess> var2 = this.iterator();

            while(true) {
                BkProcessTree.OSProcess p;
                do {
                    if (!var2.hasNext()) {
                        return;
                    }

                    p = var2.next();
                } while(p.getPid() < 10);

                log("Considering to kill " + p.getPid());

                boolean matched;
                try {
                    matched = p.hasMatchingEnvVars(modelEnvVars);
                } catch (WinpException var6) {
                    log("  Failed to check environment variable match", var6);
                    continue;
                }

                if (matched) {
                    p.killRecursively(forceFlag);
                } else {
                    log("Environment variable didn't match");
                }
            }
        }

        static {
            WinProcess.enableDebugPrivilege();
        }
    }

    private final class SerializedProcess implements Serializable {
        private final int pid;
        private static final long serialVersionUID = 1L;

        private SerializedProcess(int pid) {
            this.pid = pid;
        }

        Object readResolve() {
            return BkProcessTree.this.get(this.pid);
        }
    }

    public abstract class OSProcess implements IOSProcess, Serializable {
        final int pid;

        private OSProcess(int pid) {
            this.pid = pid;
        }

        public final int getPid() {
            return this.pid;
        }

        public abstract BkProcessTree.OSProcess getParent();

        final BkProcessTree getTree() {
            return BkProcessTree.this;
        }

        public final List<BkProcessTree.OSProcess> getChildren() {
            List<BkProcessTree.OSProcess> r = new ArrayList<>();

            for (OSProcess p : BkProcessTree.this) {
                if (p.getParent() == this) {
                    r.add(p);
                }
            }

            return r;
        }

        public abstract void kill(boolean forceFlag) throws InterruptedException;

        void killByKiller() throws InterruptedException {

            for (ProcessKiller killer : BkProcessTree.this.getKillers()) {
                try {
                    if (killer.kill(this)) {
                        break;
                    }
                } catch (IOException var4) {
                    BkProcessTree.log("Failed to kill pid=" + this.getPid(), var4);
                }
            }

        }

        public abstract void killRecursively(boolean forceFlag) throws InterruptedException;

        public abstract List<String> getArguments();

        public abstract EnvVars getEnvironmentVariables();

        public final boolean hasMatchingEnvVars(Map<String, String> modelEnvVar) {
            if (!modelEnvVar.isEmpty()) {
                SortedMap<String, String> envs = this.getEnvironmentVariables();
                Iterator<Entry<String, String>> var3 = modelEnvVar.entrySet().iterator();

                Entry<String, String> e;
                String v;
                do {
                    if (!var3.hasNext()) {
                        return true;
                    }

                    e = var3.next();
                    v = envs.get(e.getKey());
                } while (v != null && v.equals(e.getValue()));

            }
            return false;
        }

        Object writeReplace() {
            return BkProcessTree.this.new SerializedProcess(this.pid);
        }
    }
}
