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

import java.util.Locale;

import com.sun.tools.javac.api.Formattable;
import com.sun.tools.javac.api.Messages;
import com.sun.tools.javac.parser.Tokens.Token.Tag;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Filter;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;


/**
 * HCZ：Tokens对象
 *
 *  A class that defines codes/utilities for Java source tokens
 *  returned from lexical analysis.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Tokens {
    /**
     * HCZ：Names对象
     */
    private final Names names;

    /**
     * HCZ：建立Name对象的Index->TokenKind对象的映射
     *
     * Keyword array. Maps name indices to Token.
     */
    private final TokenKind[] key;

    /**
     * HCZ：记录Name对象在Table中的最大Index
     *
     *  The number of the last entered keyword.
     */
    private int maxKey = 0;

    /**
     * HCZ：每个TokenKind的枚举值的序号作为数组下标，用于建立token序号->Name对象的映射关系。
     *
     *  The names of all tokens.
     */
    private Name[] tokenName = new Name[TokenKind.values().length];

    /**
     * HCZ：在上下文对象中，加入Tokens对象缓存
     */
    public static final Context.Key<Tokens> tokensKey =
        new Context.Key<Tokens>();

    /**
     * HCZ：创建Tokens实例，如果在Context对象能够获取到，则直接返回
     */
    public static Tokens instance(Context context) {
        Tokens instance = context.get(tokensKey);
        if (instance == null)
            instance = new Tokens(context);
        return instance;
    }

    /**
     * HCZ：[调用链]创建Tokens、SharedNameTable、Names、Name实例
     *  JavaCompiler#JavaCompiler(Context)
     *      Names#Names(Context context)：初始化Names对象
     *          SharedNameTable#SharedNameTable(Names, int hashSize, int nameSize)：初始化Names对象所属的Table对象
     *          NameImpl#NameImpl(SharedNameTable)：初始化Names对象包含的各个Name对象
     *      ParserFactory#ParserFactory(Context)：初始化ParserFactory对象
     *          Tokens#Tokens(Context context)：初始化ParserFactory对象包含的Tokens对象
     *              (Tokens对象会初始化"token枚举值序列号->Name对象"和"Name对象Index->TokenKind对象"的映射)
     */
    protected Tokens(Context context) {
        //HCZ：加到上下文对象中
        context.put(tokensKey, this);
        //HCZ：创建Names对象
        names = Names.instance(context);
        //HCZ：遍历TokenKind的枚举值列表
        for (TokenKind t : TokenKind.values()) {
            //HCZ：如果枚举值.name不为空(关键点：有的TokenKind#name不为空，一般是关键字，如：abstract)，则
            if (t.name != null)
                //HCZ：建立token枚举值序号->Name对象的映射
                enterKeyword(t.name, t);
            //HCZ：如果枚举值.name为空，则
            else
                //HCZ：建立token枚举值序号->null的映射
                tokenName[t.ordinal()] = null;
        }

        //HCZ：建立Name对象的Index->TokenKind对象的映射
        key = new TokenKind[maxKey+1];
        for (int i = 0; i <= maxKey; i++) key[i] = TokenKind.IDENTIFIER;
        for (TokenKind t : TokenKind.values()) {
            if (t.name != null)
            key[tokenName[t.ordinal()].getIndex()] = t;
        }
    }

    /**
     * HCZ：建立token枚举值序号->Name对象的映射
     */
    private void enterKeyword(String s, TokenKind token) {
        //HCZ：根据字符串s，生成Name对象
        Name n = names.fromString(s);
        //HCZ：建立token枚举值序号->Name对象的映射
        tokenName[token.ordinal()] = n;
        //HCZ：记录Name对象在Table中的最大Index
        if (n.getIndex() > maxKey) maxKey = n.getIndex();
    }

    /**
     * HCZ：根据Name对象，查找TokenKind对象
     *
     * Create a new token given a name; if the name corresponds to a token name,
     * a new token of the corresponding kind is returned; otherwise, an
     * identifier token is returned.
     */
    TokenKind lookupKind(Name name) {
        return (name.getIndex() > maxKey) ? TokenKind.IDENTIFIER : key[name.getIndex()];
    }

    /**
     * HCZ：根据字符串，找到Name对象，进而查找TokenKind对象
     */
    TokenKind lookupKind(String name) {
        return lookupKind(names.fromString(name));
    }

    /**
     * HCZ：Token枚举类
     *
     * This enum defines all tokens used by the javac scanner. A token is
     * optionally associated with a name.
     */
    public enum TokenKind implements Formattable, Filter<TokenKind> {
        EOF(),//HCZ：控制类型-读到EOF的Token，表示Token流结束
        ERROR(),//HCZ：控制类型
        IDENTIFIER(Tag.NAMED),//HCZ：标识符-泛指用户自定义的类名、包名、变量名、方法名等
        ABSTRACT("abstract"),//HCZ：保留关键字-面向对象
        ASSERT("assert", Tag.NAMED),//HCZ：保留关键字-断言
        BOOLEAN("boolean", Tag.NAMED),//HCZ：保留关键字-数据类型
        BREAK("break"),//HCZ：保留关键字-控制语句
        BYTE("byte", Tag.NAMED),//HCZ：保留关键字-数据类型
        CASE("case"),//HCZ：保留关键字-控制语句
        CATCH("catch"),//HCZ：保留关键字-异常处理
        CHAR("char", Tag.NAMED),//HCZ：保留关键字-数据类型
        CLASS("class"),//HCZ：保留关键字-面向对象
        CONST("const"),//HCZ：保留关键字-修饰符
        CONTINUE("continue"),//HCZ：保留关键字-控制语句
        DEFAULT("default"),//HCZ：保留关键字-控制语句/面向对象
        DO("do"),//HCZ：保留关键字-控制语句
        DOUBLE("double", Tag.NAMED),//HCZ：保留关键字-数据类型
        ELSE("else"),//HCZ：保留关键字-控制语句
        ENUM("enum", Tag.NAMED),//HCZ：保留关键字-数据类型
        EXTENDS("extends"),//HCZ：保留关键字-面向对象
        FINAL("final"),//HCZ：保留关键字-面向对象
        FINALLY("finally"),//HCZ：保留关键字-异常处理
        FLOAT("float", Tag.NAMED),//HCZ：保留关键字-数据类型
        FOR("for"),//HCZ：保留关键字-控制语句
        GOTO("goto"),//HCZ：保留关键字-控制语句
        IF("if"),//HCZ：保留关键字-控制语句
        IMPLEMENTS("implements"),//HCZ：保留关键字-面向对象
        IMPORT("import"),//HCZ：保留关键字-代码组织结构
        INSTANCEOF("instanceof"),//HCZ：保留关键字-面向对象
        INT("int", Tag.NAMED),//HCZ：保留关键字-数据类型
        INTERFACE("interface"),//HCZ：保留关键字-面向对象
        LONG("long", Tag.NAMED),//HCZ：保留关键字-数据类型
        NATIVE("native"),//HCZ：保留关键字-JNI
        NEW("new"),//HCZ：保留关键字-面向对象
        PACKAGE("package"),//HCZ：保留关键字-代码组织结构
        PRIVATE("private"),//HCZ：保留关键字-面向对象
        PROTECTED("protected"),//HCZ：保留关键字-面向对象
        PUBLIC("public"),//HCZ：保留关键字-面向对象
        RETURN("return"),//HCZ：保留关键字-控制语句
        SHORT("short", Tag.NAMED),//HCZ：保留关键字-数据类型
        STATIC("static"),//HCZ：保留关键字-面向对象
        STRICTFP("strictfp"),//HCZ：保留关键字-面向对象
        SUPER("super", Tag.NAMED),//HCZ：保留关键字-面向对象
        SWITCH("switch"),//HCZ：保留关键字-控制语句
        SYNCHRONIZED("synchronized"),//HCZ：保留关键字-多线程
        THIS("this", Tag.NAMED),//HCZ：保留关键字-面向对象
        THROW("throw"),//HCZ：保留关键字-异常处理
        THROWS("throws"),//HCZ：保留关键字-异常处理
        TRANSIENT("transient"),//HCZ：保留关键字-序列化
        TRY("try"),//HCZ：保留关键字-异常处理
        VOID("void", Tag.NAMED),//HCZ：保留关键字-方法
        VOLATILE("volatile"),//HCZ：保留关键字-多线程
        WHILE("while"),//HCZ：保留关键字-控制语句
        INTLITERAL(Tag.NUMERIC),//HCZ：字面量
        LONGLITERAL(Tag.NUMERIC),//HCZ：字面量
        FLOATLITERAL(Tag.NUMERIC),//HCZ：字面量
        DOUBLELITERAL(Tag.NUMERIC),//HCZ：字面量
        CHARLITERAL(Tag.NUMERIC),//HCZ：字面量
        STRINGLITERAL(Tag.STRING),//HCZ：字面量
        TRUE("true", Tag.NAMED),//HCZ：字面量
        FALSE("false", Tag.NAMED),//HCZ：字面量
        NULL("null", Tag.NAMED),//HCZ：字面量
        UNDERSCORE("_", Tag.NAMED),//HCZ：标识符
        ARROW("->"),//HCZ：标识符
        COLCOL("::"),//HCZ：标识符
        LPAREN("("),//HCZ：标识符
        RPAREN(")"),//HCZ：标识符
        LBRACE("{"),//HCZ：标识符
        RBRACE("}"),//HCZ：标识符
        LBRACKET("["),//HCZ：标识符
        RBRACKET("]"),//HCZ：标识符
        SEMI(";"),//HCZ：标识符
        COMMA(","),//HCZ：标识符
        DOT("."),//HCZ：标识符
        ELLIPSIS("..."),//HCZ：标识符
        EQ("="),//HCZ：标识符
        GT(">"),//HCZ：标识符
        LT("<"),//HCZ：标识符
        BANG("!"),//HCZ：标识符
        TILDE("~"),//HCZ：标识符
        QUES("?"),//HCZ：标识符
        COLON(":"),//HCZ：标识符
        EQEQ("=="),//HCZ：标识符
        LTEQ("<="),//HCZ：标识符
        GTEQ(">="),//HCZ：标识符
        BANGEQ("!="),//HCZ：标识符
        AMPAMP("&&"),//HCZ：标识符
        BARBAR("||"),//HCZ：标识符
        PLUSPLUS("++"),//HCZ：标识符
        SUBSUB("--"),//HCZ：标识符
        PLUS("+"),//HCZ：标识符
        SUB("-"),//HCZ：标识符
        STAR("*"),//HCZ：标识符
        SLASH("/"),//HCZ：标识符
        AMP("&"),//HCZ：标识符
        BAR("|"),//HCZ：标识符
        CARET("^"),//HCZ：标识符
        PERCENT("%"),//HCZ：标识符
        LTLT("<<"),//HCZ：标识符
        GTGT(">>"),//HCZ：标识符
        GTGTGT(">>>"),//HCZ：标识符
        PLUSEQ("+="),//HCZ：标识符
        SUBEQ("-="),//HCZ：标识符
        STAREQ("*="),//HCZ：标识符
        SLASHEQ("/="),//HCZ：标识符
        AMPEQ("&="),//HCZ：标识符
        BAREQ("|="),//HCZ：标识符
        CARETEQ("^="),//HCZ：标识符
        PERCENTEQ("%="),//HCZ：标识符
        LTLTEQ("<<="),//HCZ：标识符
        GTGTEQ(">>="),//HCZ：标识符
        GTGTGTEQ(">>>="),//HCZ：标识符
        MONKEYS_AT("@"),//HCZ：标识符
        CUSTOM;

        /**
         * HCZ：Token的Name
         */
        public final String name;
        /**
         * HCZ：Token的Tag
         */
        public final Tag tag;

        /**
         * HCZ：X
         */
        TokenKind() {
            this(null, Tag.DEFAULT);
        }

        /**
         * HCZ：X
         */
        TokenKind(String name) {
            this(name, Tag.DEFAULT);
        }

        /**
         * HCZ：X
         */
        TokenKind(Tag tag) {
            this(null, tag);
        }

        /**
         * HCZ：X
         */
        TokenKind(String name, Tag tag) {
            this.name = name;
            this.tag = tag;
        }

        /**
         * HCZ：实现国际化版本的toString
         */
        public String toString() {
            switch (this) {
            case IDENTIFIER:
                return "token.identifier";
            case CHARLITERAL:
                return "token.character";
            case STRINGLITERAL:
                return "token.string";
            case INTLITERAL:
                return "token.integer";
            case LONGLITERAL:
                return "token.long-integer";
            case FLOATLITERAL:
                return "token.float";
            case DOUBLELITERAL:
                return "token.double";
            case ERROR:
                return "token.bad-symbol";
            case EOF:
                return "token.end-of-input";
            case DOT: case COMMA: case SEMI: case LPAREN: case RPAREN:
            case LBRACKET: case RBRACKET: case LBRACE: case RBRACE:
                return "'" + name + "'";
            default:
                return name;
            }
        }

        /**
         * HCZ：实现国际化版本的toString
         */
        public String getKind() {
            return "Token";
        }

        /**
         * HCZ：实现国际化版本的toString
         */
        public String toString(Locale locale, Messages messages) {
            return name != null ? toString() : messages.getLocalizedString(locale, "compiler.misc." + toString());
        }

        /**
         * HCZ：实现比较过滤的接口
         */
        @Override
        public boolean accepts(TokenKind that) {
            //HCZ：？待研究用==
            return this == that;
        }
    }

    /**
     * HCZ：注释对象
     */
    public interface Comment {
        /**
         * HCZ：注释类型：行注释、注释块、JavaDoc
         */
        enum CommentStyle {
            LINE,
            BLOCK,
            JAVADOC,
        }

        /**
         * HCZ：X
         */
        String getText();
        /**
         * HCZ：X
         */
        int getSourcePos(int index);
        /**
         * HCZ：X
         */
        CommentStyle getStyle();
        /**
         * HCZ：X
         */
        boolean isDeprecated();
    }

    /**
     * HCZ：Token类
     *
     * This is the class representing a javac token. Each token has several fields
     * that are set by the javac lexer (i.e. start/end position, string value, etc).
     */
    public static class Token {
        /**
         * HCZ：Token.Tag的类型：Default、Named、String、Numeric
         *
         *  tags constants
         */
        enum Tag {
            DEFAULT,
            NAMED,
            STRING,
            NUMERIC;
        }

        /**
         * HCZ：此Token对象的TokenKind类型
         *
         *  The token kind
         */
        public final TokenKind kind;

        /**
         * HCZ：此属性描述此Token对象位于UnicodeReader对象中维护的buf数组(详见UnicodeReader#buf属性)的开始位置
         *
         *  The start position of this token */
        public final int pos;

        /**
         * HCZ：此属性描述此Token对象位于UnicodeReader对象中维护的buf数组(详见UnicodeReader#buf属性)的结束位置
         *
         *  The end position of this token
         */
        public final int endPos;

        /**
         * HCZ：记录此Token对象对应的一组注释对象
         *
         *  Comment reader associated with this token
         */
        public final List<Comment> comments;

        /**
         * HCZ：构造函数，除了赋初值，顺便检查TokenKind是否合法
         */
        Token(TokenKind kind, int pos, int endPos, List<Comment> comments) {
            this.kind = kind;
            this.pos = pos;
            this.endPos = endPos;
            this.comments = comments;
            checkKind();
        }

        /**
         * HCZ:X
         */
        Token[] split(Tokens tokens) {
            if (kind.name.length() < 2 || kind.tag != Tag.DEFAULT) {
                throw new AssertionError("Cant split" + kind);
            }

            TokenKind t1 = tokens.lookupKind(kind.name.substring(0, 1));
            TokenKind t2 = tokens.lookupKind(kind.name.substring(1));

            if (t1 == null || t2 == null) {
                throw new AssertionError("Cant split - bad subtokens");
            }
            return new Token[] {
                new Token(t1, pos, pos + t1.name.length(), comments),
                new Token(t2, pos + t1.name.length(), endPos, null)
            };
        }

        /**
         * HCZ：X
         */
        protected void checkKind() {
            if (kind.tag != Tag.DEFAULT) {
                throw new AssertionError("Bad token kind - expected " + Tag.STRING);
            }
        }

        /**
         * HCZ：禁止Token对象直接调用，必须由子类实现
         */
        public Name name() {
            throw new UnsupportedOperationException();
        }
        /**
         * HCZ：禁止Token对象直接调用，必须由子类实现
         */
        public String stringVal() {
            throw new UnsupportedOperationException();
        }
        /**
         * HCZ：禁止Token对象直接调用，必须由子类实现
         */
        public int radix() {
            throw new UnsupportedOperationException();
        }

        /**
         * HCZ：如果此Token对象找到N个注释对象，则返回第一个。
         *
         * Preserve classic semantics - if multiple javadocs are found on the token
         * the last one is returned
         */
        public Comment comment(Comment.CommentStyle style) {
            List<Comment> comments = getComments(Comment.CommentStyle.JAVADOC);
            return comments.isEmpty() ?
                    null :
                    comments.head;
        }

        /**
         * HCZ：判断注释对象中是否有@deprecated字符串
         *
         * Preserve classic semantics - deprecated should be set if at least one
         * javadoc comment attached to this token contains the '@deprecated' string
         */
        public boolean deprecatedFlag() {
            for (Comment c : getComments(Comment.CommentStyle.JAVADOC)) {
                if (c.isDeprecated()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * HCZ：X
         */
        private List<Comment> getComments(Comment.CommentStyle style) {
            if (comments == null) {
                return List.nil();
            } else {
                ListBuffer<Comment> buf = new ListBuffer<>();
                for (Comment c : comments) {
                    if (c.getStyle() == style) {
                        buf.add(c);
                    }
                }
                return buf.toList();
            }
        }
    }

    /**
     * HCZ：Named类型的Token
     */
    final static class NamedToken extends Token {
        /**
         * HCZ：X
         *  The name of this token
         */
        public final Name name;

        /**
         * HCZ：X
         */
        public NamedToken(TokenKind kind, int pos, int endPos, Name name, List<Comment> comments) {
            super(kind, pos, endPos, comments);
            this.name = name;
        }

        /**
         * HCZ：X
         */
        protected void checkKind() {
            if (kind.tag != Tag.NAMED) {
                throw new AssertionError("Bad token kind - expected " + Tag.NAMED);
            }
        }

        /**
         * HCZ：X
         */
        @Override
        public Name name() {
            return name;
        }
    }

    /**
     * HCZ：String类型的Token
     */
    static class StringToken extends Token {
        /**
         * HCZ：X
         *  The string value of this token
         */
        public final String stringVal;

        /**
         * HCZ：X
         */
        public StringToken(TokenKind kind, int pos, int endPos, String stringVal, List<Comment> comments) {
            super(kind, pos, endPos, comments);
            this.stringVal = stringVal;
        }

        /**
         * HCZ：X
         */
        protected void checkKind() {
            if (kind.tag != Tag.STRING) {
                throw new AssertionError("Bad token kind - expected " + Tag.STRING);
            }
        }

        /**
         * HCZ：X
         */
        @Override
        public String stringVal() {
            return stringVal;
        }
    }

    /**
     * HCZ：Numeric类型的Token
     */
    final static class NumericToken extends StringToken {
        /**
         * HCZ：X
         *  The 'radix' value of this token
         */
        public final int radix;

        /**
         * HCZ：X
         */
        public NumericToken(TokenKind kind, int pos, int endPos, String stringVal, int radix, List<Comment> comments) {
            super(kind, pos, endPos, stringVal, comments);
            this.radix = radix;
        }

        /**
         * HCZ：X
         */
        protected void checkKind() {
            if (kind.tag != Tag.NUMERIC) {
                throw new AssertionError("Bad token kind - expected " + Tag.NUMERIC);
            }
        }

        /**
         * HCZ：X
         */
        @Override
        public int radix() {
            return radix;
        }
    }

    /**
     * HCZ：Scanner对象解析Token的时候，给Token对象赋初值为DUMMY
     */
    public static final Token DUMMY =
                new Token(TokenKind.ERROR, 0, 0, null);
}
