/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tools.javac.tree.DocTreeMaker;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import java.util.Locale;

/**
 * HCZ：解析器工厂，用于创建Parser对象
 *
 * A factory for creating parsers.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class ParserFactory {

    /**
     * HCZ：在Context对象中，记录ParserFactory对象
     *
     *  The context key for the parser factory.
     */
    protected static final Context.Key<ParserFactory> parserFactoryKey = new Context.Key<ParserFactory>();

    /**
     * 创建ParserFactory对象，如果上下文中有则直接获取
     */
    public static ParserFactory instance(Context context) {
        ParserFactory instance = context.get(parserFactoryKey);
        if (instance == null) {
            instance = new ParserFactory(context);
        }
        return instance;
    }

    /**
     * HCZ：trees的工厂类对象
     */
    final TreeMaker F;
    /**
     * HCZ：DocTree的工厂类对象
     */
    final DocTreeMaker docTreeMaker;
    /**
     * HCZ：日志工具类
     */
    final Log log;
    /**
     * HCZ：Tokens对象
     */
    final Tokens tokens;
    /**
     * HCZ：？
     */
    final Source source;
    /**
     * HCZ：Names对象
     */
    final Names names;
    /**
     * HCZ：Options对象
     */
    final Options options;
    /**
     * HCZ：ScannerFactory工厂
     */
    final ScannerFactory scannerFactory;
    /**
     * HCZ：国际化对象
     */
    final Locale locale;

    /**
     * HCZ：构造函数，通过各个类的instance方法，创建对象
     */
    protected ParserFactory(Context context) {
        super();
        context.put(parserFactoryKey, this);
        this.F = TreeMaker.instance(context);
        this.docTreeMaker = DocTreeMaker.instance(context);
        this.log = Log.instance(context);
        this.names = Names.instance(context);
        this.tokens = Tokens.instance(context);
        this.source = Source.instance(context);
        this.options = Options.instance(context);
        this.scannerFactory = ScannerFactory.instance(context);
        this.locale = context.get(Locale.class);
    }

    /**
     * HCZ：创建Parser对象
     */
    public JavacParser newParser(CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap) {
        Lexer lexer = scannerFactory.newScanner(input, keepDocComments);
        return new JavacParser(this, lexer, keepDocComments, keepLineMap, keepEndPos);
    }
}
