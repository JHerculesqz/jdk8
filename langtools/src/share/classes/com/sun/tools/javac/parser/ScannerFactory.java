/*
 * Copyright (c) 1999, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.parser;

import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;

import java.nio.CharBuffer;


/**
 * HCZ：Scanner工厂
 *
 * A factory for creating scanners.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own
 *  risk.  This code and its internal interfaces are subject to change
 *  or deletion without notice.</b>
 */
public class ScannerFactory {
    /**
     * HCZ：上下文对象，缓存Scanner工厂对象
     *
     *  The context key for the scanner factory.
     */
    public static final Context.Key<ScannerFactory> scannerFactoryKey =
        new Context.Key<ScannerFactory>();

    /**
     * HCZ：创建Scanner工厂对象，如果上下文中已经缓存了，则直接返回
     *
     *  Get the Factory instance for this context.
     */
    public static ScannerFactory instance(Context context) {
        ScannerFactory instance = context.get(scannerFactoryKey);
        if (instance == null)
            instance = new ScannerFactory(context);
        return instance;
    }

    /**
     * HCZ：Log对象
     */
    final Log log;
    /**
     * HCZ：Names对象
     */
    final Names names;
    /**
     * HCZ：Source对象
     */
    final Source source;
    /**
     * HCZ：Tokens对象
     */
    final Tokens tokens;

    /**
     * HCZ：构造函数
     *
     *  Create a new scanner factory.
     */
    protected ScannerFactory(Context context) {
        context.put(scannerFactoryKey, this);
        this.log = Log.instance(context);
        this.names = Names.instance(context);
        this.source = Source.instance(context);
        this.tokens = Tokens.instance(context);
    }

    /**
     * HCZ：创建Scanner对象
     */
    public Scanner newScanner(CharSequence input, boolean keepDocComments) {
        if (input instanceof CharBuffer) {
            CharBuffer buf = (CharBuffer) input;
            if (keepDocComments)
                return new Scanner(this, new JavadocTokenizer(this, buf));
            else
                return new Scanner(this, buf);
        } else {
            char[] array = input.toString().toCharArray();
            return newScanner(array, array.length, keepDocComments);
        }
    }

    /**
     * HCZ：创建Scanner对象
     */
    public Scanner newScanner(char[] input, int inputLength, boolean keepDocComments) {
        if (keepDocComments)
            return new Scanner(this, new JavadocTokenizer(this, input, inputLength));
        else
            return new Scanner(this, input, inputLength);
    }
}
