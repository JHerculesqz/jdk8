/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.parser.Tokens.*;
import com.sun.tools.javac.util.Position.LineMap;

/**
 * HCZ：词法分析接口，Scanner实现了此接口
 *
 * The lexical analyzer maps an input stream consisting of ASCII
 * characters and Unicode escapes into a token sequence.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public interface Lexer {

    /**
     * HCZ：遍历下一个Token对象
     *
     * Consume the next token.
     */
    void nextToken();

    /**
     * HCZ：获得当前的Token对象
     *
     * Return current token.
     */
    Token token();

    /**
     * HCZ：获得指定的Token对象
     *
     * Return token with given lookahead.
     */
    Token token(int lookahead);

    /**
     * HCZ：获得前一个的Token对象
     *
     * Return the last character position of the previous token.
     */
    Token prevToken();

    /**
     * HCZ：拆分当前Token对象为2个Token对象，并返回第一个Token对象。如：<<<拆成<和<<两个Token对象。
     *
     * Splits the current token in two and return the first (splitted) token.
     * For instance {@literal '<<<'} is split into two tokens
     * {@literal '<'} and {@literal '<<'} respectively,
     * and the latter is returned.
     */
    Token split();

    /**
     * HCZ：X
     *
     * Return the position where a lexical error occurred;
     */
    int errPos();

    /**
     * HCZ：X
     *
     * Set the position where a lexical error occurred;
     */
    void errPos(int pos);

    /**
     * Build a map for translating between line numbers and
     * positions in the input.
     *
     * @return a LineMap
     */
    LineMap getLineMap();
}
