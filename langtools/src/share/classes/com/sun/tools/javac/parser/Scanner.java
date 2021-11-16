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

package com.sun.tools.javac.parser;

import com.sun.tools.javac.util.Position.LineMap;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.sun.tools.javac.parser.Tokens.DUMMY;
import static com.sun.tools.javac.parser.Tokens.Token;

/**
 * HCZ：实现了Lexer接口的Scanner对象
 *
 *  The lexical analyzer maps an input stream consisting of
 *  ASCII characters and Unicode escapes into a token sequence.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Scanner implements Lexer {
    /**
     * HCZ：Tokens对象
     */
    private Tokens tokens;

    /**
     * HCZ：调用nextToken()后，将得到的Token对象记录下来
     *
     *  The token, set by nextToken().
     */
    private Token token;

    /**
     * HCZ：调用nextToken()后，完成解析Token对象后，将当前的Token对象记录为上一个Token对象
     *
     *  The previous token, set by nextToken().
     */
    private Token prevToken;

    /**
     * HCZ：将已经遍历过的Token对象记录下来，便于lookahead场景调用
     *
     *  Buffer of saved tokens (used during lookahead)
     */
    private List<Token> savedTokens = new ArrayList<Token>();

    /**
     * HCZ：Java的Token分析器对象
     */
    private JavaTokenizer tokenizer;

    /**
     * HCZ：X
     *
     * Create a scanner from the input array.  This method might
     * modify the array.  To avoid copying the input array, ensure
     * that {@code inputLength < input.length} or
     * {@code input[input.length -1]} is a white space character.
     *
     * @param fac the factory which created this Scanner
     * @param buf the input, might be modified
     * Must be positive and less than or equal to input.length.
     */
    protected Scanner(ScannerFactory fac, CharBuffer buf) {
        this(fac, new JavaTokenizer(fac, buf));
    }

    /**
     * HCZ：X
     */
    protected Scanner(ScannerFactory fac, char[] buf, int inputLength) {
        this(fac, new JavaTokenizer(fac, buf, inputLength));
    }

    /**
     * HCZ：X
     */
    protected Scanner(ScannerFactory fac, JavaTokenizer tokenizer) {
        this.tokenizer = tokenizer;
        tokens = fac.tokens;
        token = prevToken = DUMMY;
    }

    /**
     * HCZ：获得指定位置的Token对象
     */
    public Token token() {
        return token(0);
    }

    /**
     * HCZ：获得指定位置的Token对象
     */
    public Token token(int lookahead) {
        if (lookahead == 0) {
            return token;
        } else {
            ensureLookahead(lookahead);
            return savedTokens.get(lookahead - 1);
        }
    }
    //where HCZ：X
        private void ensureLookahead(int lookahead) {
            for (int i = savedTokens.size() ; i < lookahead ; i ++) {
                savedTokens.add(tokenizer.readToken());
            }
        }

    /**
     * HCZ：获得上一个Token对象
     */
    public Token prevToken() {
        return prevToken;
    }

    /**
     * HCZ：遍历下一个Token对象
     */
    public void nextToken() {
        //HCZ：将当前Token对象赋值给上一个Token对象
        prevToken = token;
        //HCZ：设置当前Token对象
        if (!savedTokens.isEmpty()) {
            token = savedTokens.remove(0);
        } else {
            //HCZ：调用JavaTokenizer对象#readToken()，解析获得当前Token对象
            token = tokenizer.readToken();
        }
    }

    /**
     * HCZ：拆分当前Token对象为2个Token对象，并返回第一个Token对象。如：<<<拆成<和<<两个Token对象。
     */
    public Token split() {
        Token[] splitTokens = token.split(tokens);
        prevToken = splitTokens[0];
        token = splitTokens[1];
        return token;
    }

    /**
     * HCZ：创建LineMap对象
     */
    public LineMap getLineMap() {
        return tokenizer.getLineMap();
    }

    /**
     * HCZ：X
     */
    public int errPos() {
        return tokenizer.errPos();
    }

    /**
     * HCZ：X
     */
    public void errPos(int pos) {
        tokenizer.errPos(pos);
    }
}
