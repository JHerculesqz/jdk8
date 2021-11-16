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

import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.util.*;

import java.nio.CharBuffer;

import static com.sun.tools.javac.parser.Tokens.*;
import static com.sun.tools.javac.util.LayoutCharacters.*;

/**
 * HCZ：Java的Token分析器对象
 *
 *  The lexical analyzer maps an input stream consisting of
 *  ASCII characters and Unicode escapes into a token sequence.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavaTokenizer {
    /**
     * HCZ：debug标志位
     */
    private static final boolean scannerDebug = false;

    /**
     * HCZ：？
     *  Allow hex floating-point literals.
     */
    private boolean allowHexFloats;

    /**
     * HCZ：？
     *  Allow binary literals.
     */
    private boolean allowBinaryLiterals;

    /**
     * HCZ：？
     *  Allow underscores in literals.
     */
    private boolean allowUnderscoresInLiterals;

    /**
     * HCZ：？
     *  The source language setting.
     */
    private Source source;

    /**
     * HCZ：日志对象
     *
     *  The log to be used for error reporting.
     */
    private final Log log;

    /**
     * HCZ：Tokens对象
     *
     *  The token factory. */
    private final Tokens tokens;

    /**
     * HCZ：nextToken方法记录的nextToken对象的TokenKind对象
     *
     *  The token kind, set by nextToken().
     */
    protected TokenKind tk;

    /**
     * HCZ：？
     *
     *  The token's radix, set by nextToken().
     */
    protected int radix;

    /**
     * HCZ：nextToken方法记录的nextToken对象的Name对象
     *
     *  The token's name, set by nextToken().
     */
    protected Name name;

    /**
     * HCZ：X
     *
     *  The position where a lexical error occurred;
     */
    protected int errPos = Position.NOPOS;

    /**
     * HCZ：UnicodeReader对象
     *
     *  The Unicode reader (low-level stream reader).
     */
    protected UnicodeReader reader;

    /**
     * HCZ：ScanFactory对象
     */
    protected ScannerFactory fac;

    /**
     * HCZ：？
     */
    private static final boolean hexFloatsWork = hexFloatsWork();
    /**
     * HCZ：？
     */
    private static boolean hexFloatsWork() {
        try {
            Float.valueOf("0x1.0p1");
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * HCZ：构造函数，会根据CharBuffer输入对象创建UnicodeReader
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
    protected JavaTokenizer(ScannerFactory fac, CharBuffer buf) {
        this(fac, new UnicodeReader(fac, buf));
    }

    /**
     * HCZ：X
     */
    protected JavaTokenizer(ScannerFactory fac, char[] buf, int inputLength) {
        this(fac, new UnicodeReader(fac, buf, inputLength));
    }

    /**
     * HCZ：X
     */
    protected JavaTokenizer(ScannerFactory fac, UnicodeReader reader) {
        this.fac = fac;
        this.log = fac.log;
        this.tokens = fac.tokens;
        this.source = fac.source;
        this.reader = reader;
        this.allowBinaryLiterals = source.allowBinaryLiterals();
        this.allowHexFloats = source.allowHexFloats();
        this.allowUnderscoresInLiterals = source.allowUnderscoresInLiterals();
    }

    /**
     * HCZ：X
     *  Report an error at the given position using the provided arguments.
     */
    protected void lexError(int pos, String key, Object... args) {
        log.error(pos, key, args);
        tk = TokenKind.ERROR;
        errPos = pos;
    }

    /**
     * HCZ：处理转义字符串
     *
     *  Read next character in character or string literal and copy into sbuf.
     */
    private void scanLitChar(int pos) {
        //HCZ：处理转义字符
        if (reader.ch == '\\') {
            if (reader.peekChar() == '\\' && !reader.isUnicode()) {
                reader.skipChar();
                reader.putChar('\\', true);
            } else {
                reader.scanChar();
                switch (reader.ch) {
                case '0': case '1': case '2': case '3':
                case '4': case '5': case '6': case '7':
                    char leadch = reader.ch;
                    //HCZ：将8进制表示的字符串转为10进制数
                    int oct = reader.digit(pos, 8);
                    reader.scanChar();
                    if ('0' <= reader.ch && reader.ch <= '7') {
                        oct = oct * 8 + reader.digit(pos, 8);
                        reader.scanChar();
                        if (leadch <= '3' && '0' <= reader.ch && reader.ch <= '7') {
                            oct = oct * 8 + reader.digit(pos, 8);
                            reader.scanChar();
                        }
                    }
                    reader.putChar((char)oct);
                    break;
                case 'b':
                    reader.putChar('\b', true); break;
                case 't':
                    reader.putChar('\t', true); break;
                case 'n':
                    reader.putChar('\n', true); break;
                case 'f':
                    reader.putChar('\f', true); break;
                case 'r':
                    reader.putChar('\r', true); break;
                case '\'':
                    reader.putChar('\'', true); break;
                case '\"':
                    reader.putChar('\"', true); break;
                case '\\':
                    reader.putChar('\\', true); break;
                default:
                    lexError(reader.bp, "illegal.esc.char");
                }
            }
        }
        //HCZ:处理非转义字符
        else if (reader.bp != reader.buflen) {
            reader.putChar(true);
        }
    }

    /**
     * HCZ：？
     */
    private void scanDigits(int pos, int digitRadix) {
        char saveCh;
        int savePos;
        do {
            if (reader.ch != '_') {
                reader.putChar(false);
            } else {
                if (!allowUnderscoresInLiterals) {
                    lexError(pos, "unsupported.underscore.lit", source.name);
                    allowUnderscoresInLiterals = true;
                }
            }
            saveCh = reader.ch;
            savePos = reader.bp;
            reader.scanChar();
        } while (reader.digit(pos, digitRadix) >= 0 || reader.ch == '_');
        if (saveCh == '_')
            lexError(savePos, "illegal.underscore");
    }

    /**
     * HCZ：？
     *
     *  Read fractional part of hexadecimal floating point number.
     */
    private void scanHexExponentAndSuffix(int pos) {
        if (reader.ch == 'p' || reader.ch == 'P') {
            reader.putChar(true);
            skipIllegalUnderscores();
            if (reader.ch == '+' || reader.ch == '-') {
                reader.putChar(true);
            }
            skipIllegalUnderscores();
            if ('0' <= reader.ch && reader.ch <= '9') {
                scanDigits(pos, 10);
                if (!allowHexFloats) {
                    lexError(pos, "unsupported.fp.lit", source.name);
                    allowHexFloats = true;
                }
                else if (!hexFloatsWork)
                    lexError(pos, "unsupported.cross.fp.lit");
            } else
                lexError(pos, "malformed.fp.lit");
        } else {
            lexError(pos, "malformed.fp.lit");
        }
        if (reader.ch == 'f' || reader.ch == 'F') {
            reader.putChar(true);
            tk = TokenKind.FLOATLITERAL;
            radix = 16;
        } else {
            if (reader.ch == 'd' || reader.ch == 'D') {
                reader.putChar(true);
            }
            tk = TokenKind.DOUBLELITERAL;
            radix = 16;
        }
    }

    /**
     * HCZ：？
     *
     *  Read fractional part of floating point number.
     */
    private void scanFraction(int pos) {
        skipIllegalUnderscores();
        if ('0' <= reader.ch && reader.ch <= '9') {
            scanDigits(pos, 10);
        }
        int sp1 = reader.sp;
        if (reader.ch == 'e' || reader.ch == 'E') {
            reader.putChar(true);
            skipIllegalUnderscores();
            if (reader.ch == '+' || reader.ch == '-') {
                reader.putChar(true);
            }
            skipIllegalUnderscores();
            if ('0' <= reader.ch && reader.ch <= '9') {
                scanDigits(pos, 10);
                return;
            }
            lexError(pos, "malformed.fp.lit");
            reader.sp = sp1;
        }
    }

    /**
     * HCZ：处理10进制中的小数以及后缀部分
     *
     *  Read fractional part and 'd' or 'f' suffix of floating point number.
     */
    private void scanFractionAndSuffix(int pos) {
        radix = 10;
        scanFraction(pos);
        if (reader.ch == 'f' || reader.ch == 'F') {
            reader.putChar(true);
            tk = TokenKind.FLOATLITERAL;
        } else {
            if (reader.ch == 'd' || reader.ch == 'D') {
                reader.putChar(true);
            }
            tk = TokenKind.DOUBLELITERAL;
        }
    }

    /**
     * HCZ：处理16进制的小数以及后缀部分
     *
     *  Read fractional part and 'd' or 'f' suffix of floating point number.
     */
    private void scanHexFractionAndSuffix(int pos, boolean seendigit) {
        radix = 16;
        Assert.check(reader.ch == '.');
        reader.putChar(true);
        skipIllegalUnderscores();
        if (reader.digit(pos, 16) >= 0) {
            seendigit = true;
            scanDigits(pos, 16);
        }
        if (!seendigit)
            lexError(pos, "invalid.hex.number");
        else
            scanHexExponentAndSuffix(pos);
    }

    /**
     * HCZ：处理下划线字符
     */
    private void skipIllegalUnderscores() {
        if (reader.ch == '_') {
            lexError(reader.bp, "illegal.underscore");
            while (reader.ch == '_')
                reader.scanChar();
        }
    }

    /**
     * HCZ：？
     *
     *  Read a number.
     *  @param radix  The radix of the number; one of 2, j8, 10, 16.
     */
    private void scanNumber(int pos, int radix) {
        // for octal, allow base-10 digit in case it's a float literal
        this.radix = radix;
        int digitRadix = (radix == 8 ? 10 : radix);
        boolean seendigit = false;
        if (reader.digit(pos, digitRadix) >= 0) {
            seendigit = true;
            scanDigits(pos, digitRadix);
        }
        if (radix == 16 && reader.ch == '.') {
            scanHexFractionAndSuffix(pos, seendigit);
        } else if (seendigit && radix == 16 && (reader.ch == 'p' || reader.ch == 'P')) {
            scanHexExponentAndSuffix(pos);
        } else if (digitRadix == 10 && reader.ch == '.') {
            reader.putChar(true);
            scanFractionAndSuffix(pos);
        } else if (digitRadix == 10 &&
                   (reader.ch == 'e' || reader.ch == 'E' ||
                    reader.ch == 'f' || reader.ch == 'F' ||
                    reader.ch == 'd' || reader.ch == 'D')) {
            scanFractionAndSuffix(pos);
        } else {
            if (reader.ch == 'l' || reader.ch == 'L') {
                reader.scanChar();
                tk = TokenKind.LONGLITERAL;
            } else {
                tk = TokenKind.INTLITERAL;
            }
        }
    }

    /**
     * HCZ：扫描标识符。while-true，打破条件是非字母、数字、下划线、美元符号的ASCII。
     *
     *  Read an identifier.
     */
    private void scanIdent() {
        boolean isJavaIdentifierPart;
        char high;
        //HCZ：扩容sbuf、将ch追加进sbuf、触发scanChar方法
        reader.putChar(true);
        do {
            switch (reader.ch) {
                //HCZ:字母、数字、下划线、美元符号，不做任何处理
            case 'A': case 'B': case 'C': case 'D': case 'E':
            case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O':
            case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y':
            case 'Z':
            case 'a': case 'b': case 'c': case 'd': case 'e':
            case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o':
            case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y':
            case 'z':
            case '$': case '_':
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                break;
                //HCZ：？
            case '\u0000': case '\u0001': case '\u0002': case '\u0003':
            case '\u0004': case '\u0005': case '\u0006': case '\u0007':
            case '\u0008': case '\u000E': case '\u000F': case '\u0010':
            case '\u0011': case '\u0012': case '\u0013': case '\u0014':
            case '\u0015': case '\u0016': case '\u0017':
            case '\u0018': case '\u0019': case '\u001B':
            case '\u007F':
                reader.scanChar();
                continue;
                //HCZ：EOI
            case '\u001A': // EOI is also a legal identifier part
                //HCZ：已经没有待处理的字符了
                if (reader.bp >= reader.buflen) {
                    name = reader.name();
                    tk = tokens.lookupKind(name);
                    return;
                }
                reader.scanChar();
                continue;
            default:
                //HCZ：如果ch是ASCII编码中的一个字符，则不管(因为所有合法的ASCII字符已经在上面的case分支处理过了)
                if (reader.ch < '\u0080') {
                    // all ASCII range chars already handled, above
                    isJavaIdentifierPart = false;
                }
                //HCZ：？待研究
                else {
                    if (Character.isIdentifierIgnorable(reader.ch)) {
                        reader.scanChar();
                        continue;
                    } else {
                        //HCZ：？获取高代理项
                        high = reader.scanSurrogates();
                        if (high != 0) {
                            reader.putChar(high);
                            //HCZ：判断通过高代理项和低代理项表示的字符是否为合法标识符的首字母
                            isJavaIdentifierPart = Character.isJavaIdentifierPart(
                                Character.toCodePoint(high, reader.ch));
                        } else {
                            isJavaIdentifierPart = Character.isJavaIdentifierPart(reader.ch);
                        }
                    }
                }
                if (!isJavaIdentifierPart) {
                    name = reader.name();
                    tk = tokens.lookupKind(name);
                    return;
                }
            }
            //HCZ：调用UnicodeReader对象#putChar方法进行sbuf数组的扩容
            reader.putChar(true);
        } while (true);
    }

    /**
     * HCZ：是否是特殊字符
     *
     *  Return true if reader.ch can be part of an operator.
     */
    private boolean isSpecial(char ch) {
        switch (ch) {
        case '!': case '%': case '&': case '*': case '?':
        case '+': case '-': case ':': case '<': case '=':
        case '>': case '^': case '|': case '~':
        case '@':
            return true;
        default:
            return false;
        }
    }

    /**
     * HCZ：扫描标识符号，尽可能多地的扫描出完整标识符号，如：/=会被扫成一个标识符号。
     *
     *  Read longest possible sequence of special characters and convert
     *  to token.
     */
    private void scanOperator() {
        while (true) {
            reader.putChar(false);
            Name newname = reader.name();
            TokenKind tk1 = tokens.lookupKind(newname);
            if (tk1 == TokenKind.IDENTIFIER) {
                reader.sp--;
                break;
            }
            tk = tk1;
            reader.scanChar();
            if (!isSpecial(reader.ch)) break;
        }
    }

    /**
     * HCZ：被Scanner对象#nextToken方法调用
     *  Read token.
     */
    public Token readToken() {
        reader.sp = 0;
        name = null;
        radix = 0;

        int pos = 0;
        int endPos = 0;
        List<Comment> comments = null;

        try {
            loop: while (true) {
                pos = reader.bp;
                switch (reader.ch) {
                    //HCZ：特殊字符-空格。[关键点]下述3个case分支都当做空格处理
                case ' ': // (Spec 3.6)
                    //HCZ：特殊字符-水平制表符，即Tab键
                case '\t': // (Spec 3.6)
                    //HCZ：特殊字符-换行、换页符
                case FF: // (Spec 3.6)
                    do {
                        reader.scanChar();
                    } while (reader.ch == ' ' || reader.ch == '\t' || reader.ch == FF);
                    processWhiteSpace(pos, reader.bp);
                    break;
                    //HCZ：特殊字符-换行符。
                case LF: // (Spec 3.4)
                    reader.scanChar();
                    processLineTerminator(pos, reader.bp);
                    break;
                    //HCZ：特殊字符-回车。
                case CR: // (Spec 3.4)
                    reader.scanChar();
                    if (reader.ch == LF) {
                        reader.scanChar();
                    }
                    processLineTerminator(pos, reader.bp);
                    break;
                    //HCZ：标识符
                case 'A': case 'B': case 'C': case 'D': case 'E':
                case 'F': case 'G': case 'H': case 'I': case 'J':
                case 'K': case 'L': case 'M': case 'N': case 'O':
                case 'P': case 'Q': case 'R': case 'S': case 'T':
                case 'U': case 'V': case 'W': case 'X': case 'Y':
                case 'Z':
                case 'a': case 'b': case 'c': case 'd': case 'e':
                case 'f': case 'g': case 'h': case 'i': case 'j':
                case 'k': case 'l': case 'm': case 'n': case 'o':
                case 'p': case 'q': case 'r': case 's': case 't':
                case 'u': case 'v': case 'w': case 'x': case 'y':
                case 'z':
                case '$': case '_':
                    scanIdent();
                    break loop;
                    //HCZ：数字-0
                case '0':
                    reader.scanChar();
                    //HCZ:如果是16进制表示的整数or浮点数，
                    if (reader.ch == 'x' || reader.ch == 'X') {
                        reader.scanChar();
                        skipIllegalUnderscores();
                        if (reader.ch == '.') {
                            //HCZ：处理16进制的小数以及后缀部分
                            scanHexFractionAndSuffix(pos, false);
                        } else if (reader.digit(pos, 16) < 0) {
                            lexError(pos, "invalid.hex.number");
                        } else {
                            scanNumber(pos, 16);
                        }
                    }
                    //HCZ：处理2进制表示的整数
                    else if (reader.ch == 'b' || reader.ch == 'B') {
                        if (!allowBinaryLiterals) {
                            lexError(pos, "unsupported.binary.lit", source.name);
                            allowBinaryLiterals = true;
                        }
                        reader.scanChar();
                        skipIllegalUnderscores();
                        if (reader.digit(pos, 2) < 0) {
                            lexError(pos, "invalid.binary.number");
                        } else {
                            scanNumber(pos, 2);
                        }
                    }
                    //HCZ：处理8进制表示的整数
                    else {
                        reader.putChar('0');
                        if (reader.ch == '_') {
                            int savePos = reader.bp;
                            do {
                                reader.scanChar();
                            } while (reader.ch == '_');
                            if (reader.digit(pos, 10) < 0) {
                                lexError(savePos, "illegal.underscore");
                            }
                        }
                        scanNumber(pos, 8);
                    }
                    break loop;
                    //HCZ：处理10进制表示的整数or浮点数
                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    scanNumber(pos, 10);
                    break loop;
                case '.':
                    reader.scanChar();
                    //HCZ：处理10进制中的小数部分
                    if ('0' <= reader.ch && reader.ch <= '9') {
                        reader.putChar('.');
                        //HCZ：处理10进制中的小数以及后缀部分
                        scanFractionAndSuffix(pos);
                    }
                    //HCZ：处理变长参数
                    else if (reader.ch == '.') {
                        int savePos = reader.bp;
                        reader.putChar('.'); reader.putChar('.', true);
                        if (reader.ch == '.') {
                            reader.scanChar();
                            reader.putChar('.');
                            tk = TokenKind.ELLIPSIS;
                        } else {
                            lexError(savePos, "illegal.dot");
                        }
                    }
                    //HCZ：处理分隔符
                    else {
                        tk = TokenKind.DOT;
                    }
                    break loop;
                    //HCZ：分隔符处理
                case ',':
                    reader.scanChar(); tk = TokenKind.COMMA; break loop;
                    //HCZ：分隔符处理
                case ';':
                    reader.scanChar(); tk = TokenKind.SEMI; break loop;
                    //HCZ：分隔符处理
                case '(':
                    reader.scanChar(); tk = TokenKind.LPAREN; break loop;
                    //HCZ：分隔符处理
                case ')':
                    reader.scanChar(); tk = TokenKind.RPAREN; break loop;
                    //HCZ：分隔符处理
                case '[':
                    reader.scanChar(); tk = TokenKind.LBRACKET; break loop;
                    //HCZ：分隔符处理
                case ']':
                    reader.scanChar(); tk = TokenKind.RBRACKET; break loop;
                    //HCZ：分隔符处理
                case '{':
                    reader.scanChar(); tk = TokenKind.LBRACE; break loop;
                    //HCZ：分隔符处理
                case '}':
                    reader.scanChar(); tk = TokenKind.RBRACE; break loop;
                    //HCZ：斜杠作为首字符
                case '/':
                    reader.scanChar();
                    //HCZ：单行注释
                    if (reader.ch == '/') {
                        do {
                            reader.scanCommentChar();
                        } while (reader.ch != CR && reader.ch != LF && reader.bp < reader.buflen);
                        if (reader.bp < reader.buflen) {
                            comments = addComment(comments, processComment(pos, reader.bp, CommentStyle.LINE));
                        }
                        break;
                    } else if (reader.ch == '*') {
                        boolean isEmpty = false;
                        reader.scanChar();
                        CommentStyle style;
                        if (reader.ch == '*') {
                            style = CommentStyle.JAVADOC;
                            reader.scanCommentChar();
                            if (reader.ch == '/') {
                                isEmpty = true;
                            }
                        } else {
                            style = CommentStyle.BLOCK;
                        }
                        while (!isEmpty && reader.bp < reader.buflen) {
                            if (reader.ch == '*') {
                                reader.scanChar();
                                if (reader.ch == '/') break;
                            } else {
                                reader.scanCommentChar();
                            }
                        }
                        if (reader.ch == '/') {
                            reader.scanChar();
                            comments = addComment(comments, processComment(pos, reader.bp, style));
                            break;
                        } else {
                            lexError(pos, "unclosed.comment");
                            break loop;
                        }
                    } else if (reader.ch == '=') {
                        tk = TokenKind.SLASHEQ;
                        reader.scanChar();
                    } else {
                        tk = TokenKind.SLASH;
                    }
                    break loop;
                    //HCZ：单引号作为首字符
                case '\'':
                    reader.scanChar();
                    if (reader.ch == '\'') {
                        lexError(pos, "empty.char.lit");
                    } else {
                        if (reader.ch == CR || reader.ch == LF)
                            lexError(pos, "illegal.line.end.in.char.lit");
                        scanLitChar(pos);
                        char ch2 = reader.ch;
                        if (reader.ch == '\'') {
                            reader.scanChar();
                            tk = TokenKind.CHARLITERAL;
                        } else {
                            lexError(pos, "unclosed.char.lit");
                        }
                    }
                    break loop;
                    //HCZ：双引号作为首字符-字符串常量
                case '\"':
                    reader.scanChar();
                    //HCZ：如果ch不是双引号、不为回车且有待处理字符，则循环调用scanLitChar方法扫描字符串常量
                    while (reader.ch != '\"' && reader.ch != CR && reader.ch != LF && reader.bp < reader.buflen)
                        scanLitChar(pos);
                    if (reader.ch == '\"') {
                        tk = TokenKind.STRINGLITERAL;
                        reader.scanChar();
                    } else {
                        lexError(pos, "unclosed.str.lit");
                    }
                    break loop;
                    //HCZ：如一些运算符首字符、以汉字开头的标识符等
                default:
                    //HCZ:如果ch是特殊字符
                    if (isSpecial(reader.ch)) {
                        scanOperator();
                    }
                    //HCZ:如果ch是标识符的首字母
                    else {
                        boolean isJavaIdentifierStart;
                        //HCZ:ch是ASCII编码中的一个字符
                        if (reader.ch < '\u0080') {
                            // all ASCII range chars already handled, above
                            isJavaIdentifierStart = false;
                        } else {
                            //HCZ:获得高代理项
                            char high = reader.scanSurrogates();
                            if (high != 0) {
                                reader.putChar(high);
                                //HCZ:方法会判断通过高代理项和低代理项表示的字符是否是合法标识符的首字符
                                isJavaIdentifierStart = Character.isJavaIdentifierStart(
                                    Character.toCodePoint(high, reader.ch));
                            } else {
                                isJavaIdentifierStart = Character.isJavaIdentifierStart(reader.ch);
                            }
                        }
                        //HCZ：是合法标识符的首字符
                        if (isJavaIdentifierStart) {
                            scanIdent();
                        }
                        //HCZ：已经没有待处理的字符了
                        else if (reader.bp == reader.buflen || reader.ch == EOI && reader.bp + 1 == reader.buflen) { // JLS 3.5
                            tk = TokenKind.EOF;
                            pos = reader.buflen;
                        } else {
                            String arg = (32 < reader.ch && reader.ch < 127) ?
                                            String.format("%s", reader.ch) :
                                            String.format("\\u%04x", (int)reader.ch);
                            lexError(pos, "illegal.char", arg);
                            reader.scanChar();
                        }
                    }
                    break loop;
                }
            }
            endPos = reader.bp;
            //HCZ：根据当前Token对象.tag属性+计算好的token、pos、endPos对象，创建对应的Token对象
            switch (tk.tag) {
                case DEFAULT: return new Token(tk, pos, endPos, comments);
                case NAMED: return new NamedToken(tk, pos, endPos, name, comments);
                case STRING: return new StringToken(tk, pos, endPos, reader.chars(), comments);
                case NUMERIC: return new NumericToken(tk, pos, endPos, reader.chars(), radix, comments);
                default: throw new AssertionError();
            }
        }
        finally {
            //HCZ：scannerDebug属性一直是false，估计是调时的时候打开用来看日志
            if (scannerDebug) {
                    System.out.println("nextToken(" + pos
                                       + "," + endPos + ")=|" +
                                       new String(reader.getRawCharacters(pos, endPos))
                                       + "|");
            }
        }
    }
    //where HCZ：X
        List<Comment> addComment(List<Comment> comments, Comment comment) {
            return comments == null ?
                    List.of(comment) :
                    comments.prepend(comment);
        }

    /**
     * HCZ：X
     *
     *  Return the position where a lexical error occurred;
     */
    public int errPos() {
        return errPos;
    }

    /**
     * HCZ：X
     *
     *  Set the position where a lexical error occurred;
     */
    public void errPos(int pos) {
        errPos = pos;
    }

    /**
     * HCZ:X
     *
     * Called when a complete comment has been scanned. pos and endPos
     * will mark the comment boundary.
     */
    protected Tokens.Comment processComment(int pos, int endPos, CommentStyle style) {
        if (scannerDebug)
            System.out.println("processComment(" + pos
                               + "," + endPos + "," + style + ")=|"
                               + new String(reader.getRawCharacters(pos, endPos))
                               + "|");
        char[] buf = reader.getRawCharacters(pos, endPos);
        return new BasicComment<UnicodeReader>(new UnicodeReader(fac, buf, buf.length), style);
    }

    /**
     * HCZ:X
     *
     * Called when a complete whitespace run has been scanned. pos and endPos
     * will mark the whitespace boundary.
     */
    protected void processWhiteSpace(int pos, int endPos) {
        if (scannerDebug)
            System.out.println("processWhitespace(" + pos
                               + "," + endPos + ")=|" +
                               new String(reader.getRawCharacters(pos, endPos))
                               + "|");
    }

    /**
     * HCZ:X
     * Called when a line terminator has been processed.
     */
    protected void processLineTerminator(int pos, int endPos) {
        if (scannerDebug)
            System.out.println("processTerminator(" + pos
                               + "," + endPos + ")=|" +
                               new String(reader.getRawCharacters(pos, endPos))
                               + "|");
    }

    /**
     * HCZ：创建LineMap对象
     *
     *  Build a map for translating between line numbers and
     * positions in the input.
     *
     * @return a LineMap */
    public Position.LineMap getLineMap() {
        return Position.makeLineMap(reader.getRawCharacters(), reader.buflen, false);
    }

    /**
     * HCZ：X
     *
    * Scan a documentation comment; determine if a deprecated tag is present.
    * Called once the initial /, * have been skipped, positioned at the second *
    * (which is treated as the beginning of the first line).
    * Stops positioned at the closing '/'.
    */
    protected static class BasicComment<U extends UnicodeReader> implements Comment {

        CommentStyle cs;
        U comment_reader;

        protected boolean deprecatedFlag = false;
        protected boolean scanned = false;

        protected BasicComment(U comment_reader, CommentStyle cs) {
            this.comment_reader = comment_reader;
            this.cs = cs;
        }

        public String getText() {
            return null;
        }

        public int getSourcePos(int pos) {
            return -1;
        }

        public CommentStyle getStyle() {
            return cs;
        }

        public boolean isDeprecated() {
            if (!scanned && cs == CommentStyle.JAVADOC) {
                scanDocComment();
            }
            return deprecatedFlag;
        }

        @SuppressWarnings("fallthrough")
        protected void scanDocComment() {
            try {
                boolean deprecatedPrefix = false;

                comment_reader.bp += 3; // '/**'
                comment_reader.ch = comment_reader.buf[comment_reader.bp];

                forEachLine:
                while (comment_reader.bp < comment_reader.buflen) {

                    // Skip optional WhiteSpace at beginning of line
                    while (comment_reader.bp < comment_reader.buflen && (comment_reader.ch == ' ' || comment_reader.ch == '\t' || comment_reader.ch == FF)) {
                        comment_reader.scanCommentChar();
                    }

                    // Skip optional consecutive Stars
                    while (comment_reader.bp < comment_reader.buflen && comment_reader.ch == '*') {
                        comment_reader.scanCommentChar();
                        if (comment_reader.ch == '/') {
                            return;
                        }
                    }

                    // Skip optional WhiteSpace after Stars
                    while (comment_reader.bp < comment_reader.buflen && (comment_reader.ch == ' ' || comment_reader.ch == '\t' || comment_reader.ch == FF)) {
                        comment_reader.scanCommentChar();
                    }

                    deprecatedPrefix = false;
                    // At beginning of line in the JavaDoc sense.
                    if (!deprecatedFlag) {
                        String deprecated = "@deprecated";
                        int i = 0;
                        while (comment_reader.bp < comment_reader.buflen && comment_reader.ch == deprecated.charAt(i)) {
                            comment_reader.scanCommentChar();
                            i++;
                            if (i == deprecated.length()) {
                                deprecatedPrefix = true;
                                break;
                            }
                        }
                    }

                    if (deprecatedPrefix && comment_reader.bp < comment_reader.buflen) {
                        if (Character.isWhitespace(comment_reader.ch)) {
                            deprecatedFlag = true;
                        } else if (comment_reader.ch == '*') {
                            comment_reader.scanCommentChar();
                            if (comment_reader.ch == '/') {
                                deprecatedFlag = true;
                                return;
                            }
                        }
                    }

                    // Skip rest of line
                    while (comment_reader.bp < comment_reader.buflen) {
                        switch (comment_reader.ch) {
                            case '*':
                                comment_reader.scanCommentChar();
                                if (comment_reader.ch == '/') {
                                    return;
                                }
                                break;
                            case CR: // (Spec 3.4)
                                comment_reader.scanCommentChar();
                                if (comment_reader.ch != LF) {
                                    continue forEachLine;
                                }
                            /* fall through to LF case */
                            case LF: // (Spec 3.4)
                                comment_reader.scanCommentChar();
                                continue forEachLine;
                            default:
                                comment_reader.scanCommentChar();
                        }
                    } // rest of line
                } // forEachLine
                return;
            } finally {
                scanned = true;
            }
        }
    }
}
