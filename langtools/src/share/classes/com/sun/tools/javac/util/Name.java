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

package com.sun.tools.javac.util;

/**
 * HCZ：Name对象，包含所属的Table对象
 *
 *  An abstraction for internal compiler strings. They are stored in
 *  Utf8 format. Names are stored in a Name.Table, and are unique within
 *  that table.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class Name implements javax.lang.model.element.Name {
    /**
     * HCZ：所属的Table对象
     */
    public final Table table;

    /**
     * HCZ：构造函数
     */
    protected Name(Table table) {
        this.table = table;
    }

    /**
     * HCZ：比较两个Name对象toString以后是否相等
     *
     * {@inheritDoc}
     */
    public boolean contentEquals(CharSequence cs) {
        return toString().equals(cs.toString());
    }

    /**
     * HCZ：获得Name对象toString以后的字符串长度
     *
     * {@inheritDoc}
     */
    public int length() {
        return toString().length();
    }

    /**
     * HCZ：获得Name对象toString以后的字符串中指定index的字符
     *
     * {@inheritDoc}
     */
    public char charAt(int index) {
        return toString().charAt(index);
    }

    /**
     * HCZ：获得Name对象toString以后的字符串的子串
     *
     * {@inheritDoc}
     */
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    /**
     * HCZ：Name对象相加
     * Return the concatenation of this name and name `n'.
     */
    public Name append(Name n) {
        int len = getByteLength();
        byte[] bs = new byte[len + n.getByteLength()];
        getBytes(bs, 0);
        n.getBytes(bs, len);
        return table.fromUtf(bs, 0, bs.length);
    }

    /**
     * HCZ：字符c和Name对象相加，返回新的Name对象
     *
     *  Return the concatenation of this name, the given ASCII
     *  character, and name `n'.
     */
    public Name append(char c, Name n) {
        int len = getByteLength();
        byte[] bs = new byte[len + 1 + n.getByteLength()];
        getBytes(bs, 0);
        bs[len] = (byte) c;
        n.getBytes(bs, len+1);
        return table.fromUtf(bs, 0, bs.length);
    }

    /**
     * HCZ：Name对象比较
     *
     *  An arbitrary but consistent complete order among all Names.
     */
    public int compareTo(Name other) {
        return other.getIndex() - this.getIndex();
    }

    /**
     * HCZ：Name是否为空
     *  Return true if this is the empty name.
     */
    public boolean isEmpty() {
        return getByteLength() == 0;
    }

    /**
     * HCZ：获得Name对应的字节b的lastIndex
     *
     *  Returns last occurrence of byte b in this name, -1 if not found.
     */
    public int lastIndexOf(byte b) {
        byte[] bytes = getByteArray();
        int offset = getByteOffset();
        int i = getByteLength() - 1;
        while (i >= 0 && bytes[offset + i] != b) i--;
        return i;
    }

    /**
     * HCZ:此Name对象的前缀是否是传入的prefix对象(Name对象)
     *
     *  Does this name start with prefix?
     */
    public boolean startsWith(Name prefix) {
        byte[] thisBytes = this.getByteArray();
        int thisOffset   = this.getByteOffset();
        int thisLength   = this.getByteLength();
        byte[] prefixBytes = prefix.getByteArray();
        int prefixOffset   = prefix.getByteOffset();
        int prefixLength   = prefix.getByteLength();

        int i = 0;
        while (i < prefixLength &&
               i < thisLength &&
               thisBytes[thisOffset + i] == prefixBytes[prefixOffset + i])
            i++;
        return i == prefixLength;
    }

    /**
     * HCZ：此Name对象中获得子Name对象
     *
     *  Returns the sub-name starting at position start, up to and
     *  excluding position end.
     */
    public Name subName(int start, int end) {
        if (end < start) end = start;
        return table.fromUtf(getByteArray(), getByteOffset() + start, end - start);
    }

    /**
     * HCZ：将Name对象toString
     *
     *  Return the string representation of this name.
     */
    @Override
    public String toString() {
        return Convert.utf2string(getByteArray(), getByteOffset(), getByteLength());
    }

    /**
     * HCZ：返回Name对象的UTF-8编码
     *
     *  Return the Utf8 representation of this name.
     */
    public byte[] toUtf() {
        byte[] bs = new byte[getByteLength()];
        getBytes(bs, 0);
        return bs;
    }

    /**
     * HCZ：从Table对象维护的bytes数组中，获取对应信息
     *
     * Get a "reasonably small" value that uniquely identifies this name
     * within its name table.
     */
    public abstract int getIndex();

    /**
     * HCZ：从Table对象维护的bytes数组中，获取对应信息
     *
     *  Get the length (in bytes) of this name.
     */
    public abstract int getByteLength();

    /**
     * HCZ：从Table对象维护的bytes数组中，获取对应信息
     *
     *  Returns i'th byte of this name.
     */
    public abstract byte getByteAt(int i);

    /**
     * HCZ：从Table对象维护的bytes数组中，获取对应信息
     *
     * Copy all bytes of this name to buffer cs, starting at start.
     */
    public void getBytes(byte cs[], int start) {
        System.arraycopy(getByteArray(), getByteOffset(), cs, start, getByteLength());
    }

    /**
     * HCZ：从Table对象维护的bytes数组中，获取对应信息
     *
     * Get the underlying byte array for this name. The contents of the
     * array must not be modified.
     */
    public abstract byte[] getByteArray();

    /**
     * HCZ：从Table对象维护的bytes数组中，获取对应信息
     *
     * Get the start offset of this name within its byte array.
     */
    public abstract int getByteOffset();

    /**
     * HCZ：抽象类Table，1个Table对象包含1个Names对象，1个Names对象包含N个Name对象
     *
     *  An abstraction for the hash table used to create unique Name instances.
     */
    public static abstract class Table {
        /**
         * HCZ：Table中包含的Names对象
         *
         * Standard name table.
         */
        public final Names names;

        /**
         * HCZ：构造函数
         */
        Table(Names names) {
            this.names = names;
        }

        /**
         * HCZ：抽象接口，留给实现类具体实现"调用Table对象的方法，从cs对象(char[])中读取指定的char子数组，更新Table对象，并转换成Name对象"的逻辑
         *
         * Get the name from the characters in cs[start..start+len-1].
         */
        public abstract Name fromChars(char[] cs, int start, int len);

        /**
         * HCZ：调用Table对象的方法，从cs对象(char[])中读取指定的char子数组，更新Table对象，并转换成Name对象
         *
         * Get the name for the characters in string s.
         */
        public Name fromString(String s) {
            char[] cs = s.toCharArray();
            return fromChars(cs, 0, cs.length);
        }

        /**
         * HCZ：调用Table对象的方法，从cs对象(byte[])中读取指定的byte子数组，更新Table对象，转换成Name对象
         *
         * Get the name for the bytes in array cs.
         *  Assume that bytes are in utf8 format.
         */
        public Name fromUtf(byte[] cs) {
            return fromUtf(cs, 0, cs.length);
        }

        /**
         * HCZ：抽象接口，留给实现类具体实现"调用Table对象的方法，从cs对象(byte[])中读取指定的byte子数组，更新Table对象，转换成Name对象"的逻辑
         *
         * get the name for the bytes in cs[start..start+len-1].
         *  Assume that bytes are in utf8 format.
         */
        public abstract Name fromUtf(byte[] cs, int start, int len);

        /**
         * HCZ：抽象接口，回收Table对象的资源
         *
         * Release any resources used by this table.
         */
        public abstract void dispose();

        /**
         * HCZ：计算hashCode
         *
         * The hashcode of a name.
         */
        protected static int hashValue(byte bytes[], int offset, int length) {
            int h = 0;
            int off = offset;

            for (int i = 0; i < length; i++) {
                h = (h << 5) - h + bytes[off++];
            }
            return h;
        }

        /**
         * HCZ：比较两个字节数组中指定的子数组是否相等
         *
         *  Compare two subarrays
         */
        protected static boolean equals(byte[] bytes1, int offset1,
                byte[] bytes2, int offset2, int length) {
            int i = 0;
            while (i < length && bytes1[offset1 + i] == bytes2[offset2 + i]) {
                i++;
            }
            return i == length;
        }
    }
}
