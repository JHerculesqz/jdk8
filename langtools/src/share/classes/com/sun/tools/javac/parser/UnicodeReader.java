/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.CharBuffer;
import java.util.Arrays;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.ArrayUtils;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import static com.sun.tools.javac.util.LayoutCharacters.*;

/**
 * HCZ：UnicodeReader对象，供JavaTokenizer对象使用
 *
 *  The char reader used by the javac lexer/tokenizer. Returns the sequence of
 * characters contained in the input stream, handling unicode escape accordingly.
 * Additionally, it provides features for saving chars into a buffer and to retrieve
 * them at a later stage.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class UnicodeReader {
    /**
     * HCZ：保存了从Java源代码中读入的所有字符，最后一个元素为EOI(即0x1A，表示没有可读取的字符了)
     *
     *  The input buffer, index of next character to be read,
     *  index of one past last character in buffer.
     */
    protected char[] buf;
    /**
     * HCZ：当前要处理的字符在buf数组中的位置，初始化为-1
     */
    protected int bp;
    /**
     * HCZ：保存了buf数组中可读字符的数量
     */
    protected final int buflen;

    /**
     * HCZ：当前待处理的字符
     *
     *  The current character.
     */
    protected char ch;

    /**
     * HCZ：？
     *
     *  The buffer index of the last converted unicode character
     */
    protected int unicodeConversionBp = -1;

    /**
     * HCZ：Log对象
     */
    protected Log log;
    /**
     * HCZ：Names对象
     */
    protected Names names;

    /**
     * HCZ：某个Token对象的Name对象由N个字符组成(如：+=)，此数组暂存了两个字符(+、=)
     *
     *  A character buffer for saved chars.
     */
    protected char[] sbuf = new char[128];
    /**
     * HCZ：sbuf数组的可用下标，每调用一次nextToken()，就会将sp初始化为0，保证sbuf可以反复使用
     */
    protected int sp;

    /**
     * HCZ：X
     *
     * Create a scanner from the input array.  This method might
     * modify the array.  To avoid copying the input array, ensure
     * that {@code inputLength < input.length} or
     * {@code input[input.length -1]} is a white space character.
     *
     * @param sf the factory which created this Scanner
     * @param buffer the input, might be modified
     * Must be positive and less than or equal to input.length.
     */
    protected UnicodeReader(ScannerFactory sf, CharBuffer buffer) {
        this(sf, JavacFileManager.toArray(buffer), buffer.limit());
    }

    /**
     * HCZ：构造函数，更新buf/buflen/bp属性(详见这些属性注释的说明)，将ch扫描到第一个字符
     */
    protected UnicodeReader(ScannerFactory sf, char[] input, int inputLength) {
        log = sf.log;
        names = sf.names;
        if (inputLength == input.length) {
            if (input.length > 0 && Character.isWhitespace(input[input.length - 1])) {
                inputLength--;
            } else {
                input = Arrays.copyOf(input, inputLength + 1);
            }
        }
        buf = input;
        buflen = inputLength;
        buf[buflen] = EOI;
        bp = -1;
        scanChar();
    }

    /**
     * HCZ：[关键点]工具方法，将bp往前移动，并从buf中取出来bp对应的字符ch，reader对象会记录下一个待处理的ch。
     *
     *  Read next character.
     */
    protected void scanChar() {
        if (bp < buflen) {
            ch = buf[++bp];
            if (ch == '\\') {
                convertUnicode();
            }
        }
    }

    /**
     * HCZ：X
     *
     *  Read next character in comment, skipping over double '\' characters.
     */
    protected void scanCommentChar() {
        scanChar();
        if (ch == '\\') {
            if (peekChar() == '\\' && !isUnicode()) {
                skipChar();
            } else {
                convertUnicode();
            }
        }
    }

    /**
     * HCZ：扩容sbuf、将ch追加进sbuf、触发scanChar方法
     *  Append a character to sbuf.
     */
    protected void putChar(char ch, boolean scan) {
        //HCZ：扩容sbuf
        sbuf = ArrayUtils.ensureCapacity(sbuf, sp);
        //HCZ：将ch追加进sbuf
        sbuf[sp++] = ch;
        //HCZ：触发scanChar方法
        if (scan)
            scanChar();
    }

    /**
     * HCZ：扩容sbuf、将ch追加进sbuf、触发scanChar方法
     */
    protected void putChar(char ch) {
        putChar(ch, false);
    }

    /**
     * HCZ：扩容sbuf、将ch追加进sbuf、触发scanChar方法(指向下一个ch)
     */
    protected void putChar(boolean scan) {
        putChar(ch, scan);
    }

    /**
     * HCZ：根据sbuf中维护的name关键词，在Names对象中维护的SharedNameTable对象(hash表)中，查找对应的Name对象
     */
    Name name() {
        return names.fromChars(sbuf, 0, sp);
    }

    /**
     * HCZ：?
     */
    String chars() {
        return new String(sbuf, 0, sp);
    }

    /**
     * HCZ：？
     *
     *  Convert unicode escape; bp points to initial '\' character
     *  (Spec 3.3).
     */
    protected void convertUnicode() {
        if (ch == '\\' && unicodeConversionBp != bp) {
            bp++; ch = buf[bp];
            if (ch == 'u') {
                do {
                    bp++; ch = buf[bp];
                } while (ch == 'u');
                int limit = bp + 3;
                if (limit < buflen) {
                    int d = digit(bp, 16);
                    int code = d;
                    while (bp < limit && d >= 0) {
                        bp++; ch = buf[bp];
                        d = digit(bp, 16);
                        code = (code << 4) + d;
                    }
                    if (d >= 0) {
                        ch = (char)code;
                        unicodeConversionBp = bp;
                        return;
                    }
                }
                log.error(bp, "illegal.unicode.esc");
            } else {
                bp--;
                ch = '\\';
            }
        }
    }

    /**
     * HCZ：？
     *  Are surrogates supported?
     */
    final static boolean surrogatesSupported = surrogatesSupported();
    /**
     * HCZ：？
     */
    private static boolean surrogatesSupported() {
        try {
            Character.isHighSurrogate('a');
            return true;
        } catch (NoSuchMethodError ex) {
            return false;
        }
    }

    /**
     * HCZ：？获得高代理项
     *
     *  Scan surrogate pairs.  If 'ch' is a high surrogate and
     *  the next character is a low surrogate, then put the low
     *  surrogate in 'ch', and return the high surrogate.
     *  otherwise, just return 0.
     */
    protected char scanSurrogates() {
        if (surrogatesSupported && Character.isHighSurrogate(ch)) {
            char high = ch;

            scanChar();

            if (Character.isLowSurrogate(ch)) {
                return high;
            }

            ch = high;
        }

        return 0;
    }

    /**
     * HCZ：将8进制表示的字符串转为10进制数、base表示2/8/10/16进制
     *
     *  Convert an ASCII digit from its base (8, 10, or 16)
     *  to its value.
     */
    protected int digit(int pos, int base) {
        char c = ch;
        int result = Character.digit(c, base);
        if (result >= 0 && c > 0x7f) {
            log.error(pos + 1, "illegal.nonascii.digit");
            ch = "0123456789abcdef".charAt(result);
        }
        return result;
    }

    /**
     * HCZ：？
     */
    protected boolean isUnicode() {
        return unicodeConversionBp == bp;
    }

    /**
     * HCZ：？
     */
    protected void skipChar() {
        bp++;
    }

    /**
     * HCZ：？
     */
    protected char peekChar() {
        return buf[bp + 1];
    }

    /**
     * HCZ：？
     *
     * Returns a copy of the input buffer, up to its inputLength.
     * Unicode escape sequences are not translated.
     */
    public char[] getRawCharacters() {
        char[] chars = new char[buflen];
        System.arraycopy(buf, 0, chars, 0, buflen);
        return chars;
    }

    /**
     * HCZ：？
     *
     * Returns a copy of a character array subset of the input buffer.
     * The returned array begins at the {@code beginIndex} and
     * extends to the character at index {@code endIndex - 1}.
     * Thus the length of the substring is {@code endIndex-beginIndex}.
     * This behavior is like
     * {@code String.substring(beginIndex, endIndex)}.
     * Unicode escape sequences are not translated.
     *
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @throws ArrayIndexOutOfBoundsException if either offset is outside of the
     *         array bounds
     */
    public char[] getRawCharacters(int beginIndex, int endIndex) {
        int length = endIndex - beginIndex;
        char[] chars = new char[length];
        System.arraycopy(buf, beginIndex, chars, 0, length);
        return chars;
    }
}
