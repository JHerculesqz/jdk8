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

import java.lang.ref.SoftReference;

/**
 * HCZ：Table对象
 *
 * Implementation of Name.Table that stores all names in a single shared
 * byte array, expanding it as needed. This avoids the overhead incurred
 * by using an array of bytes for each name.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SharedNameTable extends Name.Table {
    /**
     * HCZ：已经使用过的SharedNameTable对象的软引用列表
     *
     * maintain a freelist of recently used name tables for reuse.
     */
    private static List<SoftReference<SharedNameTable>> freelist = List.nil();

    /**
     * HCZ：同步方法，在已经使用过的SharedNameTable对象的软引用列表找到可以使用的SharedNameTable对象。找不到就创建一个新的。
     */
    static public synchronized SharedNameTable create(Names names) {
        while (freelist.nonEmpty()) {
            SharedNameTable t = freelist.head.get();
            freelist = freelist.tail;
            if (t != null) {
                return t;
            }
        }
        return new SharedNameTable(names);
    }

    /**
     * HCZ：同步方法，在软引用列表中删除SharedNameTable对象
     */
    static private synchronized void dispose(SharedNameTable t) {
        freelist = freelist.prepend(new SoftReference<SharedNameTable>(t));
    }

    /**
     * HCZ：Name对象的HashTable，计算NameImpl对象的哈希值，存储在如下数组的特定位置。如果出现冲突，就是用NameImpl对象#next形成单链表。
     *
     *  The hash table for names.
     */
    private NameImpl[] hashes;

    /**
     * HCZ：以字节形式存储N个Name对象对应的字符串。每个Name对象记录了自己在此bytes数组中的start和length
     *
     *  The shared byte array holding all encountered names.
     */
    public byte[] bytes;

    /**
     * HCZ：哈希掩码
     *
     *  The mask to be used for hashing
     */
    private int hashMask;

    /**
     * HCZ：记录了bytes数组中下一个可用的位置
     *  The number of filled bytes in `names'.
     */
    private int nc = 0;

    /**
     * HCZ：构造函数
     *
     *  Allocator
     *  @param names The main name table
     *  @param hashSize the (constant) size to be used for the hash table
     *                  needs to be a power of two.
     *  @param nameSize the initial size of the name table.
     */
    public SharedNameTable(Names names, int hashSize, int nameSize) {
        super(names);
        hashMask = hashSize - 1;
        hashes = new NameImpl[hashSize];
        bytes = new byte[nameSize];
    }

    /**
     * HCZ：构造函数
     */
    public SharedNameTable(Names names) {
        this(names, 0x8000, 0x20000);
    }

    /**
     * HCZ：根据char数组生成Name对象
     * [调用链]
     * Scanner#token()：返回Token对象
     *  Scanner#ensureLookahead()
     *      JavaTokenizer#readToken()
     *          JavaTokenizer#scanIdent()
     *              UnicodeReader#name()
     *                  Names#fromChars(char[], int start, int len)
     *                      SharedNameTable#fromChars(char[], int start, int len)
     *
     * ParserFactory#newParser()
     *  得到Lexer对象=>JavacParser#构造函数()
     *      JavacParser#nextToken()
     *          JavaTokenizer#readToken()
     *              JavaTokenizer#scanIdent()
     *                  UnicodeReader#name()
     *                      Names#fromChars(char[], int start, int len)
     *                          SharedNameTable#fromChars(char[], int start, int len)
     */
    @Override
    public Name fromChars(char[] cs, int start, int len) {
        //HCZ：获得bytes数组中下一个可用的位置
        int nc = this.nc;
        //HCZ：对bytes数组进行扩容(如果有必要)
        byte[] bytes = this.bytes = ArrayUtils.ensureCapacity(this.bytes, nc + len * 3);
        //HCZ：将char数组转换为byte数组，并且写入到bytes属性中，返回bytes属性中最后的index，得到byte数组的长度。
        int nbytes = Convert.chars2utf(cs, start, bytes, nc, len) - nc;
        //HCZ：根据bytes数组中下一个可用位置(也就是新写入的char数组的startIndex)、根据新写入的char数组转换后的byte数组长度，计算得到hashCode。
        int h = hashValue(bytes, nc, nbytes) & hashMask;
        //HCZ：从hashes数组中(就是NameImpl对象数组)，获得与新计算出来的hashCode相同的冲突NameImpl对象。
        NameImpl n = hashes[h];
        //HCZ：如果能找到冲突的NameImpl对象，说明hashCode冲突了，就将NameImpl对象的next指向新的NameImpl对象，形成单链表。
        while (n != null &&
                (n.getByteLength() != nbytes ||
                !equals(bytes, n.index, bytes, nc, nbytes))) {
            n = n.next;
        }
        //HCZ：如果没有找到冲突的NameImpl对象，就创建新的NameImpl对象，并记录到hashes属性。
        if (n == null) {
            n = new NameImpl(this);
            n.index = nc;
            n.length = nbytes;
            n.next = hashes[h];
            hashes[h] = n;
            this.nc = nc + nbytes;
            if (nbytes == 0) {
                this.nc++;
            }
        }
        return n;
    }

    /**
     * HCZ：？根据byte数组生成Name对象
     */
    @Override
    public Name fromUtf(byte[] cs, int start, int len) {
        int h = hashValue(cs, start, len) & hashMask;
        NameImpl n = hashes[h];
        byte[] names = this.bytes;
        while (n != null &&
                (n.getByteLength() != len || !equals(names, n.index, cs, start, len))) {
            n = n.next;
        }
        if (n == null) {
            int nc = this.nc;
            names = this.bytes = ArrayUtils.ensureCapacity(names, nc + len);
            System.arraycopy(cs, start, names, nc, len);
            n = new NameImpl(this);
            n.index = nc;
            n.length = len;
            n.next = hashes[h];
            hashes[h] = n;
            this.nc = nc + len;
            if (len == 0) {
                this.nc++;
            }
        }
        return n;
    }

    /**
     * HCZ：同步方法，在软引用列表中删除SharedNameTable对象
     */
    @Override
    public void dispose() {
        dispose(this);
    }

    /**
     * HCZ：Name类的具体实现类NameImpl类
     */
    static class NameImpl extends Name {
        /**
         * HCZ：HashTable中，下一个Name对象
         *
         *  The next name occupying the same hash bucket.
         */
        NameImpl next;

        /**
         * HCZ：存在在Table中的index
         *
         *  The index where the bytes of this name are stored in the global name
         *  buffer `byte'.
         */
        int index;

        /**
         * HCZ：Name对象在byte数组中，占据的字节长度
         *
         *  The number of bytes in this name.
         */
        int length;

        /**
         * HCZ：构造函数
         */
        NameImpl(SharedNameTable table) {
            super(table);
        }

        /**
         * HCZ：从Table对象维护的bytes数组中，获取对应信息
         */
        @Override
        public int getIndex() {
            return index;
        }

        /**
         * HCZ：从Table对象维护的bytes数组中，获取对应信息
         */
        @Override
        public int getByteLength() {
            return length;
        }

        /**
         * HCZ：从Table对象维护的bytes数组中，获取对应信息
         */
        @Override
        public byte getByteAt(int i) {
            return getByteArray()[index + i];
        }

        /**
         * HCZ：从Table对象维护的bytes数组中，获取对应信息
         */
        @Override
        public byte[] getByteArray() {
            return ((SharedNameTable) table).bytes;
        }

        /**
         * HCZ：从Table对象维护的bytes数组中，获取对应信息
         */
        @Override
        public int getByteOffset() {
            return index;
        }

        /**
         * HCZ：计算hashCode
         *
         * Return the hash value of this name.
         */
        public int hashCode() {
            return index;
        }

        /**
         * HCZ：比较Name对象是否相等
         *
         * Is this name equal to other?
         */
        public boolean equals(Object other) {
            if (other instanceof Name)
                return
                    table == ((Name)other).table && index == ((Name) other).getIndex();
            else return false;
        }
    }
}
