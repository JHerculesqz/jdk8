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

package com.sun.tools.javac;

import java.io.PrintWriter;

/**
 * HCZ:javac的入口
 *
 * The programmatic interface for the Java Programming Language
 * compiler, javac.
 */
@jdk.Exported
public class Main {
    /**
     * HCZ：javac的入口
     *
     * Main entry point for the launcher.
     * Note: This method calls System.exit.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) throws Exception {
        System.exit(compile(args));
    }

    /**
     * HCZ：触发com.sun.tools.javac.main.Main#compile(String[] args)开始真正的前端编译
     * (1)被Main#main(String[] args)调用
     * (2)被CreateSymbols#main(String... args)调用——CreateSymbols是干啥的呢？
     *
     * Programmatic interface to the Java Programming Language
     * compiler, javac.
     *
     * @param args The command line arguments that would normally be
     * passed to the javac program as described in the man page.
     * @return an integer equivalent to the exit value from invoking
     * javac, see the man page for details.
     */
    public static int compile(String[] args) {
        com.sun.tools.javac.main.Main compiler =
            new com.sun.tools.javac.main.Main("javac");
        return compiler.compile(args).exitCode;
    }

    /**
     * HCZ：触发com.sun.tools.javac.main.Main#compile(String[] args)开始真正的前端编译
     * (1)被JavacTool#run(InputStream in, OutputStream out, OutputStream err, String... arguments)调用——JavacTool干啥的？
     *
     * Programmatic interface to the Java Programming Language
     * compiler, javac.
     *
     * @param args The command line arguments that would normally be
     * passed to the javac program as described in the man page.
     * @param out PrintWriter to which the compiler's diagnostic
     * output is directed.
     * @return an integer equivalent to the exit value from invoking
     * javac, see the man page for details.
     */
    public static int compile(String[] args, PrintWriter out) {
        com.sun.tools.javac.main.Main compiler =
            new com.sun.tools.javac.main.Main("javac", out);
        return compiler.compile(args).exitCode;
    }
}
