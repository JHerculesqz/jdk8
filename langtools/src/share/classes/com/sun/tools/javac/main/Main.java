/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.doclint.DocLint;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.file.CacheFSInfo;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.jvm.Profile;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.processing.AnnotationProcessingError;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.Log.PrefixKind;
import com.sun.tools.javac.util.Log.WriterKind;
import com.sun.tools.javac.util.ServiceLoader;
import static com.sun.tools.javac.main.Option.*;

/** This class provides a command line interface to the javac compiler.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Main {

    /** HCZ：调用本类的构造函数时创建or传入的，其实就是javac
     * The name of the compiler, for use in diagnostics.
     */
    String ownName;

    /**
     * HCZ：调用本类的构造函数时创建or传入的。如：System.err的PrintWriter包装
     *
     * The writer to use for diagnostic output.
     */
    PrintWriter out;

    /**
     * HCZ：调用本类的构造函数时创建or传入的。就是Log工具类的实例
     * The log to use for diagnostic output.
     */
    public Log log;

    /**
     * HCZ：这个属性的作用就是在api模式下抛异常——JavacTaskImpl会调用setAPIMode，进而更新这个属性=true。javac命令行正常调用，此属性永远是false
     *
     * If true, certain errors will cause an exception, such as command line
     * arg errors, or exceptions in user provided code.
     */
    boolean apiMode;

    /**
     * HCZ：Main#compile的编译结果数据结构，仅仅表达编译成功or失败
     * Result codes.
     */
    public enum Result {
        OK(0),        // Compilation completed with no errors.
        ERROR(1),     // Completed but reported errors.
        CMDERR(2),    // Bad command-line arguments
        SYSERR(3),    // System error or resource exhaustion.
        ABNORMAL(4);  // Compiler terminated abnormally

        Result(int exitCode) {
            this.exitCode = exitCode;
        }

        public boolean isOK() {
            return (exitCode == 0);
        }

        public final int exitCode;
    }

    /**
     * HCZ:将Option枚举类的枚举值变成Option[]存下来。这些Option元素都是javac支持的合法的命令行配置参数
     */
    private Option[] recognizedOptions =
            Option.getJavaCompilerOptions().toArray(new Option[0]);

    /**
     * HCZ：实现OptionHelper抽象类
     */
    private OptionHelper optionHelper = new OptionHelper() {
        @Override
        public String get(Option option) {
            return options.get(option);
        }

        @Override
        public void put(String name, String value) {
            options.put(name, value);
        }

        @Override
        public void remove(String name) {
            options.remove(name);
        }

        @Override
        public Log getLog() {
            return log;
        }

        @Override
        public String getOwnName() {
            return ownName;
        }

        @Override
        public void error(String key, Object... args) {
            Main.this.error(key, args);
        }

        @Override
        public void addFile(File f) {
            filenames.add(f);
        }

        @Override
        public void addClassName(String s) {
            classnames.append(s);
        }

    };

    /**
     * HCZ:X
     * Construct a compiler instance.
     */
    public Main(String name) {
        this(name, new PrintWriter(System.err, true));
    }

    /**
     * HCZ:X
     * Construct a compiler instance.
     */
    public Main(String name, PrintWriter out) {
        this.ownName = name;
        this.out = out;
    }

    /**
     * HCZ：用户实际输入的命令行配置参数，都是在本类的调用方初始化的，初始化都是用的Options.instance(context)
     *
     * A table of all options that's passed to the JavaCompiler constructor.
     */
    private Options options = null;

    /**
     * HCZ：持有本类实例的调用方，直接给给此属性初始化，初始化为LinkedHashSet<File>。然后通过本类的optionHelper属性重载的addFile方法将javac跟着的文件列表加入到本属性中。
     *
     * The list of source files to process
     */
    public Set<File> filenames = null; // XXX sb protected

    /**
     * HCZ：
     * 在Main#compile方法中，将此属性初始化为ListBuffer<String>，
     * 在Main#processArgs方法中，给此属性添加javac跟着的文件列表中对应的ClassName列表。
     * 在Main#optionHelper属性重载的addClassName方法将javac跟着的文件列表加入到本属性中。
     *
     * List of class files names passed on the command line
     */
    public ListBuffer<String> classnames = null; // XXX sb protected  HCZ：你骂人？

    /**
     * HCZ:封装了log对象，记录错误日志，给Main类调用
     * Report a usage error.
     */
    void error(String key, Object... args) {
        if (apiMode) {
            String msg = log.localize(PrefixKind.JAVAC, key, args);
            throw new PropagatedException(new IllegalStateException(msg));
        }
        warning(key, args);
        log.printLines(PrefixKind.JAVAC, "msg.usage", ownName);
    }

    /**
     * HCZ：封装了log对象，记录warning日志，给Main类调用
     * Report a warning.
     */
    void warning(String key, Object... args) {
        log.printRawLines(ownName + ": " + log.localize(PrefixKind.JAVAC, key, args));
    }

    /**
     * HCZ:X
     */
    public Option getOption(String flag) {
        for (Option option : recognizedOptions) {
            if (option.matches(flag))
                return option;
        }
        return null;
    }

    /**
     * HCZ:X
     */
    public void setOptions(Options options) {
        if (options == null)
            throw new NullPointerException();
        this.options = options;
    }

    /**
     * HCZ:X
     */
    public void setAPIMode(boolean apiMode) {
        this.apiMode = apiMode;
    }

    /**
     *  HCZ：参数的处理函数，没啥好仔细阅读的，debug一下就明白了。
     *
     *  Process command line arguments: store all command line options
     *  in `options' table and return all source filenames.
     *
     *  @param flags    The array of command line arguments.
     */
    public Collection<File> processArgs(String[] flags) { // XXX sb protected  HCZ：你骂人？！
        return processArgs(flags, null);
    }

    /**
     * HCZ:X
     */
    public Collection<File> processArgs(String[] flags, String[] classNames) { // XXX sb protected
        int ac = 0;
        while (ac < flags.length) {
            String flag = flags[ac];
            ac++;

            Option option = null;

            if (flag.length() > 0) {
                // quick hack to speed up file processing:
                // if the option does not begin with '-', there is no need to check
                // most of the compiler options.
                int firstOptionToCheck = flag.charAt(0) == '-' ? 0 : recognizedOptions.length-1;
                for (int j=firstOptionToCheck; j<recognizedOptions.length; j++) {
                    if (recognizedOptions[j].matches(flag)) {
                        option = recognizedOptions[j];
                        break;
                    }
                }
            }

            if (option == null) {
                error("err.invalid.flag", flag);
                return null;
            }

            if (option.hasArg()) {
                if (ac == flags.length) {
                    error("err.req.arg", flag);
                    return null;
                }
                String operand = flags[ac];
                ac++;
                if (option.process(optionHelper, flag, operand))
                    return null;
            } else {
                if (option.process(optionHelper, flag))
                    return null;
            }
        }

        if (options.get(PROFILE) != null && options.get(BOOTCLASSPATH) != null) {
            error("err.profile.bootclasspath.conflict");
            return null;
        }

        if (this.classnames != null && classNames != null) {
            this.classnames.addAll(Arrays.asList(classNames));
        }

        if (!checkDirectory(D))
            return null;
        if (!checkDirectory(S))
            return null;

        String sourceString = options.get(SOURCE);
        Source source = (sourceString != null)
            ? Source.lookup(sourceString)
            : Source.DEFAULT;
        String targetString = options.get(TARGET);
        Target target = (targetString != null)
            ? Target.lookup(targetString)
            : Target.DEFAULT;
        // We don't check source/target consistency for CLDC, as J2ME
        // profiles are not aligned with J2SE targets; moreover, a
        // single CLDC target may have many profiles.  In addition,
        // this is needed for the continued functioning of the JSR14
        // prototype.
        if (Character.isDigit(target.name.charAt(0))) {
            if (target.compareTo(source.requiredTarget()) < 0) {
                if (targetString != null) {
                    if (sourceString == null) {
                        warning("warn.target.default.source.conflict",
                                targetString,
                                source.requiredTarget().name);
                    } else {
                        warning("warn.source.target.conflict",
                                sourceString,
                                source.requiredTarget().name);
                    }
                    return null;
                } else {
                    target = source.requiredTarget();
                    options.put("-target", target.name);
                }
            } else {
                if (targetString == null && !source.allowGenerics()) {
                    target = Target.JDK1_4;
                    options.put("-target", target.name);
                }
            }
        }

        String profileString = options.get(PROFILE);
        if (profileString != null) {
            Profile profile = Profile.lookup(profileString);
            if (!profile.isValid(target)) {
                warning("warn.profile.target.conflict", profileString, target.name);
                return null;
            }
        }

        // handle this here so it works even if no other options given
        String showClass = options.get("showClass");
        if (showClass != null) {
            if (showClass.equals("showClass")) // no value given for option
                showClass = "com.sun.tools.javac.Main";
            showClass(showClass);
        }

        options.notifyListeners();

        return filenames;
    }
    // where   HCZ：检查需要用到文件的Option中的文件路径是否存在
        private boolean checkDirectory(Option option) {
            String value = options.get(option);
            if (value == null)
                return true;
            File file = new File(value);
            if (!file.exists()) {
                error("err.dir.not.found", value);
                return false;
            }
            if (!file.isDirectory()) {
                error("err.file.not.directory", value);
                return false;
            }
            return true;
        }

    /**
     * HCZ：被Main调用，触发Java源文件的compile
     * Programmatic interface for main function.
     * @param args    The command line parameters.
     */
    public Result compile(String[] args) {
        //HCZ：创建上下文对象，上下文对象的解读详见Context
        Context context = new Context();
        //HCZ：向上下文对象中放置Javac文件管理对象
        JavacFileManager.preRegister(context); // can't create it until Log has been set up
        //HCZ：前戏做完，准备真的compile，得到
        Result result = compile(args, context);
        //HCZ：释放老的Javac文件管理对象
        if (fileManager instanceof JavacFileManager) {
            // A fresh context was created above, so jfm must be a JavacFileManager
            ((JavacFileManager)fileManager).close();
        }
        return result;
    }

    /**
     * HCZ:X
     */
    public Result compile(String[] args, Context context) {
        return compile(args, context, List.<JavaFileObject>nil(), null);
    }

    /**
     * HCZ：X
     * Programmatic interface for main function.
     * @param args    The command line parameters.
     */
    public Result compile(String[] args,
                       Context context,
                       List<JavaFileObject> fileObjects,
                       Iterable<? extends Processor> processors)
    {
        return compile(args,  null, context, fileObjects, processors);
    }

    /**
     * HCZ:被Main#compile(String[] args)调用，真正实现Java源码的compile
     */
    public Result compile(String[] args,
                          String[] classNames,
                          Context context,
                          List<JavaFileObject> fileObjects,
                          Iterable<? extends Processor> processors)
    {
        //HCZ：前戏：在上下文对象中塞out对象，初始化log对象、options对象、fileNames、classnames
        context.put(Log.outKey, out);
        log = Log.instance(context);

        if (options == null)
            options = Options.instance(context); // creates a new one

        filenames = new LinkedHashSet<File>();
        classnames = new ListBuffer<String>();
        JavaCompiler comp = null;
        /*
         * TODO: Logic below about what is an acceptable command line
         * should be updated to take annotation processing semantics
         * into account.
         */
        try {
            //HCZ：如果各种不合法，返回编译错误，并告知记得敲一下-help
            if (args.length == 0
                    && (classNames == null || classNames.length == 0)
                    && fileObjects.isEmpty()) {
                Option.HELP.process(optionHelper, "-help");
                return Result.CMDERR;
            }

            Collection<File> files;
            try {
                //HCZ：调用Main#processArgs，获得命令行参数中的.java文件对象列表
                files = processArgs(CommandLine.parse(args), classNames);
                //HCZ：Main#processArgs竟然返回了null？，返回错误信息
                if (files == null) {
                    // null signals an error in options, abort
                    return Result.CMDERR;
                } else if (files.isEmpty() && fileObjects.isEmpty() && classnames.isEmpty()) {
                    // it is allowed to compile nothing if just asking for help or version info
                    //HCZ：文件列表是Empty，说明可能是javac --help/-X/-version/-fullversion，则返回正确
                    if (options.isSet(HELP)
                        || options.isSet(X)
                        || options.isSet(VERSION)
                        || options.isSet(FULLVERSION))
                        return Result.OK;
                    if (JavaCompiler.explicitAnnotationProcessingRequested(options)) {
                        //HCZ:还不太懂？
                        error("err.no.source.files.classes");
                    } else {
                        //HCZ:又不是-help这种参数配置，说明用户输入错了，返回错误
                        error("err.no.source.files");
                    }
                    return Result.CMDERR;
                }
            } catch (java.io.FileNotFoundException e) {
                //HCZ：指定的文件找不到，则返回错误
                warning("err.file.not.found", e.getMessage());
                return Result.SYSERR;
            }

            //HCZ：如果指定了stdout，清一下log对象，重定向到System.out上
            boolean forceStdOut = options.isSet("stdout");
            if (forceStdOut) {
                log.flush();
                log.setWriters(new PrintWriter(System.out, true));
            }

            //HCZ：如果没有设置nonBatchMode，则在上下文对象中创建CacheFSInfo对象，用于多次编译时的缓存。
            // allow System property in following line as a Mustang legacy
            boolean batchMode = (options.isUnset("nonBatchMode")
                        && System.getProperty("nonBatchMode") == null);
            if (batchMode)
                CacheFSInfo.preRegister(context);

            // FIXME: this code will not be invoked if using JavacTask.parse/analyze/generate
            // invoke any available plugins
            //HCZ：如果存在javac的插件，则...
            //啥是javac插件？参考：https://www.baeldung.com/java-build-compiler-plugin
            //？待研究：如下初始化javac插件的逻辑细节。
            String plugins = options.get(PLUGIN);
            if (plugins != null) {
                JavacProcessingEnvironment pEnv = JavacProcessingEnvironment.instance(context);
                ClassLoader cl = pEnv.getProcessorClassLoader();
                ServiceLoader<Plugin> sl = ServiceLoader.load(Plugin.class, cl);
                Set<List<String>> pluginsToCall = new LinkedHashSet<List<String>>();
                for (String plugin: plugins.split("\\x00")) {
                    pluginsToCall.add(List.from(plugin.split("\\s+")));
                }
                JavacTask task = null;
                Iterator<Plugin> iter = sl.iterator();
                while (iter.hasNext()) {
                    Plugin plugin = iter.next();
                    for (List<String> p: pluginsToCall) {
                        if (plugin.getName().equals(p.head)) {
                            pluginsToCall.remove(p);
                            try {
                                if (task == null)
                                    task = JavacTask.instance(pEnv);
                                plugin.init(task, p.tail.toArray(new String[p.tail.size()]));
                            } catch (Throwable ex) {
                                if (apiMode)
                                    throw new RuntimeException(ex);
                                pluginMessage(ex);
                                return Result.SYSERR;
                            }
                        }
                    }
                }
                for (List<String> p: pluginsToCall) {
                    log.printLines(PrefixKind.JAVAC, "msg.plugin.not.found", p.head);
                }
            }

            //HCZ：初始化JavaCompiler
            comp = JavaCompiler.instance(context);

            // FIXME: this code will not be invoked if using JavacTask.parse/analyze/generate
            //HCZ：xdoclint相关
            //待研究：如下xdoclint的各种初始化细节
            String xdoclint = options.get(XDOCLINT);
            String xdoclintCustom = options.get(XDOCLINT_CUSTOM);
            if (xdoclint != null || xdoclintCustom != null) {
                Set<String> doclintOpts = new LinkedHashSet<String>();
                if (xdoclint != null)
                    doclintOpts.add(DocLint.XMSGS_OPTION);
                if (xdoclintCustom != null) {
                    for (String s: xdoclintCustom.split("\\s+")) {
                        if (s.isEmpty())
                            continue;
                        doclintOpts.add(s.replace(XDOCLINT_CUSTOM.text, DocLint.XMSGS_CUSTOM_PREFIX));
                    }
                }
                if (!(doclintOpts.size() == 1
                        && doclintOpts.iterator().next().equals(DocLint.XMSGS_CUSTOM_PREFIX + "none"))) {
                    JavacTask t = BasicJavacTask.instance(context);
                    // standard doclet normally generates H1, H2
                    doclintOpts.add(DocLint.XIMPLICIT_HEADERS + "2");
                    new DocLint().init(t, doclintOpts.toArray(new String[doclintOpts.size()]));
                    comp.keepComments = true;
                }
            }

            //HCZ：如果有待编译的java文件列表，则...
            fileManager = context.get(JavaFileManager.class);

            if (!files.isEmpty()) {
                // add filenames to fileObjects
                //HCZ：这里为啥又要初始化一次JavaCompiler？
                comp = JavaCompiler.instance(context);
                //HCZ：待研究，啥是otherFiles？
                List<JavaFileObject> otherFiles = List.nil();
                JavacFileManager dfm = (JavacFileManager)fileManager;
                for (JavaFileObject fo : dfm.getJavaFileObjectsFromFiles(files))
                    otherFiles = otherFiles.prepend(fo);
                for (JavaFileObject fo : otherFiles)
                    fileObjects = fileObjects.prepend(fo);
            }
            //HCZ：调用JavaCompiler#compile方法，里面有机关，详见那边的标注
            comp.compile(fileObjects,
                         classnames.toList(),
                         processors);

            if (log.expectDiagKeys != null) {
                if (log.expectDiagKeys.isEmpty()) {
                    log.printRawLines("all expected diagnostics found");
                    return Result.OK;
                } else {
                    log.printRawLines("expected diagnostic keys not found: " + log.expectDiagKeys);
                    return Result.ERROR;
                }
            }

            if (comp.errorCount() != 0)
                return Result.ERROR;
        } catch (IOException ex) {
            ioMessage(ex);
            return Result.SYSERR;
        } catch (OutOfMemoryError ex) {
            resourceMessage(ex);
            return Result.SYSERR;
        } catch (StackOverflowError ex) {
            resourceMessage(ex);
            return Result.SYSERR;
        } catch (FatalError ex) {
            feMessage(ex);
            return Result.SYSERR;
        } catch (AnnotationProcessingError ex) {
            if (apiMode)
                throw new RuntimeException(ex.getCause());
            apMessage(ex);
            return Result.SYSERR;
        } catch (ClientCodeException ex) {
            // as specified by javax.tools.JavaCompiler#getTask
            // and javax.tools.JavaCompiler.CompilationTask#call
            throw new RuntimeException(ex.getCause());
        } catch (PropagatedException ex) {
            throw ex.getCause();
        } catch (Throwable ex) {
            // Nasty.  If we've already reported an error, compensate
            // for buggy compiler error recovery by swallowing thrown
            // exceptions.
            if (comp == null || comp.errorCount() == 0 ||
                options == null || options.isSet("dev"))
                bugMessage(ex);
            return Result.ABNORMAL;
        } finally {
            if (comp != null) {
                try {
                    comp.close();
                } catch (ClientCodeException ex) {
                    throw new RuntimeException(ex.getCause());
                }
            }
            filenames = null;
            options = null;
        }
        return Result.OK;
    }

    /**
     * HCZ:封装了log对象，记录bug日志，给Main类调用
     * Print a message reporting an internal error.
     */
    void bugMessage(Throwable ex) {
        log.printLines(PrefixKind.JAVAC, "msg.bug", JavaCompiler.version());
        ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /**
     * HCZ：封装了log对象，记录fatal日志，给Main类调用
     * Print a message reporting a fatal error.
     */
    void feMessage(Throwable ex) {
        log.printRawLines(ex.getMessage());
        if (ex.getCause() != null && options.isSet("dev")) {
            ex.getCause().printStackTrace(log.getWriter(WriterKind.NOTICE));
        }
    }

    /**
     * HCZ：封装了log对象，记录io日志，给Main类调用
     * Print a message reporting an input/output error.
     */
    void ioMessage(Throwable ex) {
        log.printLines(PrefixKind.JAVAC, "msg.io");
        ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /**
     * HCZ:封装了log对象，记录资源错误日志，给Main类调用
     * Print a message reporting an out-of-resources error.
     */
    void resourceMessage(Throwable ex) {
        log.printLines(PrefixKind.JAVAC, "msg.resource");
        ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /**
     * HCZ：封装了log对象，记录注解异常日志，给Main类调用
     *
     * Print a message reporting an uncaught exception from an
     * annotation processor.
     */
    void apMessage(AnnotationProcessingError ex) {
        log.printLines(PrefixKind.JAVAC, "msg.proc.annotation.uncaught.exception");
        ex.getCause().printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /**
     * HCZ：封装了log对象，记录-Xplugin对应的plugin错误日志，给Main类调用
     *
     * Print a message reporting an uncaught exception from an
     * annotation processor.
     */
    void pluginMessage(Throwable ex) {
        log.printLines(PrefixKind.JAVAC, "msg.plugin.uncaught.exception");
        ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /**
     * HCZ：打印指定类的.class文件路径和MD5.checksum。但啥时候用到？
     * Display the location and checksum of a class.
     */
    void showClass(String className) {
        PrintWriter pw = log.getWriter(WriterKind.NOTICE);
        pw.println("javac: show class: " + className);
        URL url = getClass().getResource('/' + className.replace('.', '/') + ".class");
        if (url == null)
            pw.println("  class not found");
        else {
            pw.println("  " + url);
            try {
                final String algorithm = "MD5";
                byte[] digest;
                MessageDigest md = MessageDigest.getInstance(algorithm);
                DigestInputStream in = new DigestInputStream(url.openStream(), md);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    do { n = in.read(buf); } while (n > 0);
                    digest = md.digest();
                } finally {
                    in.close();
                }
                StringBuilder sb = new StringBuilder();
                for (byte b: digest)
                    sb.append(String.format("%02x", b));
                pw.println("  " + algorithm + " checksum: " + sb);
            } catch (Exception e) {
                pw.println("  cannot compute digest: " + e);
            }
        }
    }

    /**
     * HCZ: javac内部实现的File管理工具类
     */
    private JavaFileManager fileManager;

    /* ************************************************************************
     * Internationalization
     *************************************************************************/

//    /** Find a localized string in the resource bundle.
//     *  @param key     The key for the localized string.
//     */
//    public static String getLocalizedString(String key, Object... args) { // FIXME sb private
//        try {
//            if (messages == null)
//                messages = new JavacMessages(javacBundleName);
//            return messages.getLocalizedString("javac." + key, args);
//        }
//        catch (MissingResourceException e) {
//            throw new Error("Fatal Error: Resource for javac is missing", e);
//        }
//    }
//
//    public static void useRawMessages(boolean enable) {
//        if (enable) {
//            messages = new JavacMessages(javacBundleName) {
//                    @Override
//                    public String getLocalizedString(String key, Object... args) {
//                        return key;
//                    }
//                };
//        } else {
//            messages = new JavacMessages(javacBundleName);
//        }
//    }

    /**
     * HCZ:X
     */
    public static final String javacBundleName =
        "com.sun.tools.javac.resources.javac";
//
//    private static JavacMessages messages;
}
