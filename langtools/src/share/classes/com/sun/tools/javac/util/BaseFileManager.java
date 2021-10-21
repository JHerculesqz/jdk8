/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import com.sun.tools.javac.code.Lint;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.file.FSInfo;
import com.sun.tools.javac.file.Locations;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.main.OptionHelper;
import com.sun.tools.javac.main.OptionHelper.GrumpyHelper;
import com.sun.tools.javac.util.JCDiagnostic.SimpleDiagnosticPosition;

/**
 * Utility methods for building a filemanager.
 * There are no references here to file-system specific objects such as
 * java.io.File or java.nio.file.Path.
 */
public abstract class BaseFileManager {
    protected BaseFileManager(Charset charset) {
        this.charset = charset;
        byteBufferCache = new ByteBufferCache();
        locations = createLocations();
    }

    /**
     * Set the context for JavacPathFileManager.
     */
    public void setContext(Context context) {
        log = Log.instance(context);
        options = Options.instance(context);
        classLoaderClass = options.get("procloader");
        locations.update(log, options, Lint.instance(context), FSInfo.instance(context));
    }

    protected Locations createLocations() {
        return new Locations();
    }

    /**
     * The log to be used for error reporting.
     */
    public Log log;

    /**
     * User provided charset (through javax.tools).
     */
    protected Charset charset;

    protected Options options;

    protected String classLoaderClass;

    protected Locations locations;

    protected Source getSource() {
        String sourceName = options.get(Option.SOURCE);
        Source source = null;
        if (sourceName != null)
            source = Source.lookup(sourceName);
        return (source != null ? source : Source.DEFAULT);
    }

    protected ClassLoader getClassLoader(URL[] urls) {
        ClassLoader thisClassLoader = getClass().getClassLoader();

        // Allow the following to specify a closeable classloader
        // other than URLClassLoader.

        // 1: Allow client to specify the class to use via hidden option
        if (classLoaderClass != null) {
            try {
                Class<? extends ClassLoader> loader =
                        Class.forName(classLoaderClass).asSubclass(ClassLoader.class);
                Class<?>[] constrArgTypes = { URL[].class, ClassLoader.class };
                Constructor<? extends ClassLoader> constr = loader.getConstructor(constrArgTypes);
                return constr.newInstance(new Object[] { urls, thisClassLoader });
            } catch (Throwable t) {
                // ignore errors loading user-provided class loader, fall through
            }
        }
        return new URLClassLoader(urls, thisClassLoader);
    }

    // <editor-fold defaultstate="collapsed" desc="Option handling">
    public boolean handleOption(String current, Iterator<String> remaining) {
        OptionHelper helper = new GrumpyHelper(log) {
            @Override
            public String get(Option option) {
                return options.get(option.getText());
            }

            @Override
            public void put(String name, String value) {
                options.put(name, value);
            }

            @Override
            public void remove(String name) {
                options.remove(name);
            }
        };
        for (Option o: javacFileManagerOptions) {
            if (o.matches(current))  {
                if (o.hasArg()) {
                    if (remaining.hasNext()) {
                        if (!o.process(helper, current, remaining.next()))
                            return true;
                    }
                } else {
                    if (!o.process(helper, current))
                        return true;
                }
                // operand missing, or process returned false
                throw new IllegalArgumentException(current);
            }
        }

        return false;
    }
    // where
        private static final Set<Option> javacFileManagerOptions =
            Option.getJavacFileManagerOptions();

    public int isSupportedOption(String option) {
        for (Option o : javacFileManagerOptions) {
            if (o.matches(option))
                return o.hasArg() ? 1 : 0;
        }
        return -1;
    }

    public abstract boolean isDefaultBootClassPath();

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Encoding">
    /**
     * HCZ：默认的Java源文件的编码名称
     */
    private String defaultEncodingName;

    /**
     * HCZ：获得默认的Java源文件的编码名称
     */
    private String getDefaultEncodingName() {
        //HCZ：如果默认的Java源文件的编码名称为null，则读取OutputStreamWriter内部Charset的默认编码名称
        if (defaultEncodingName == null) {
            defaultEncodingName =
                new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
        }
        return defaultEncodingName;
    }

    /**
     * HCZ：获得Java源文件的编码名称
     */
    public String getEncodingName() {
        //HCZ：优先从命令行参数对应的option中获取编码名称
        String encName = options.get(Option.ENCODING);
        //HCZ：如果没有读到，则返回默认的Java源文件的编码名称
        if (encName == null)
            return getDefaultEncodingName();
        //HCZ：如果读到了，则返回命令行参数对应的option中的编码名称
        else
            return encName;
    }

    /**
     * HCZ：根据编码，将ByteBuffer对象转换为CharBuffer对象
     */
    public CharBuffer decode(ByteBuffer inbuf, boolean ignoreEncodingErrors) {
        //HCZ：获得Java源文件的编码名称
        String encodingName = getEncodingName();
        CharsetDecoder decoder;
        try {
            //HCZ：根据编码名称，获得CharsetDecoder对象
            decoder = getDecoder(encodingName, ignoreEncodingErrors);
        } catch (IllegalCharsetNameException e) {
            log.error("unsupported.encoding", encodingName);
            return (CharBuffer)CharBuffer.allocate(1).flip();
        } catch (UnsupportedCharsetException e) {
            log.error("unsupported.encoding", encodingName);
            return (CharBuffer)CharBuffer.allocate(1).flip();
        }

        // slightly overestimate the buffer size to avoid reallocation.
        float factor =
            decoder.averageCharsPerByte() * 0.8f +
            decoder.maxCharsPerByte() * 0.2f;
        CharBuffer dest = CharBuffer.
            allocate(10 + (int)(inbuf.remaining()*factor));

        while (true) {
            CoderResult result = decoder.decode(inbuf, dest, true);
            dest.flip();

            if (result.isUnderflow()) { // done reading
                // make sure there is at least one extra character
                if (dest.limit() == dest.capacity()) {
                    dest = CharBuffer.allocate(dest.capacity()+1).put(dest);
                    dest.flip();
                }
                return dest;
            } else if (result.isOverflow()) { // buffer too small; expand
                int newCapacity =
                    10 + dest.capacity() +
                    (int)(inbuf.remaining()*decoder.maxCharsPerByte());
                dest = CharBuffer.allocate(newCapacity).put(dest);
            } else if (result.isMalformed() || result.isUnmappable()) {
                // bad character in input

                // report coding error (warn only pre 1.5)
                if (!getSource().allowEncodingErrors()) {
                    log.error(new SimpleDiagnosticPosition(dest.limit()),
                              "illegal.char.for.encoding",
                              charset == null ? encodingName : charset.name());
                } else {
                    log.warning(new SimpleDiagnosticPosition(dest.limit()),
                                "illegal.char.for.encoding",
                                charset == null ? encodingName : charset.name());
                }

                // skip past the coding error
                inbuf.position(inbuf.position() + result.length());

                // undo the flip() to prepare the output buffer
                // for more translation
                dest.position(dest.limit());
                dest.limit(dest.capacity());
                dest.put((char)0xfffd); // backward compatible
            } else {
                throw new AssertionError(result);
            }
        }
        // unreached
    }

    /**
     * HCZ：根据编码名称，获得CharsetDecoder对象
     */
    public CharsetDecoder getDecoder(String encodingName, boolean ignoreEncodingErrors) {
        //HCZ：根据编码名称，创建Charset对象，进而创建CharsetDecoder对象
        Charset cs = (this.charset == null)
            ? Charset.forName(encodingName)
            : this.charset;
        CharsetDecoder decoder = cs.newDecoder();

        //HCZ：创建编码错误Action对象(CodingErrorAction对象)
        CodingErrorAction action;
        if (ignoreEncodingErrors)
            action = CodingErrorAction.REPLACE;
        else
            action = CodingErrorAction.REPORT;

        //HCZ：将CodingErrorAction对象塞进CharsetDecoder对象中
        return decoder
            .onMalformedInput(action)
            .onUnmappableCharacter(action);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="ByteBuffers">
    /**
     * HCZ：从ByteBuffer缓存中获取重用的java.nio.ByteBuffer对象，再将InputStream对象塞入到java.nio.ByteBuffer对象。
     * Make a byte buffer from an input stream.
     */
    public ByteBuffer makeByteBuffer(InputStream in)
        throws IOException {
        int limit = in.available();
        if (limit < 1024) limit = 1024;
        ByteBuffer result = byteBufferCache.get(limit);
        int position = 0;
        while (in.available() != 0) {
            if (position >= limit)
                // expand buffer
                result = ByteBuffer.
                    allocate(limit <<= 1).
                    put((ByteBuffer)result.flip());
            int count = in.read(result.array(),
                position,
                limit - position);
            if (count < 0) break;
            result.position(position += count);
        }
        return (ByteBuffer)result.flip();
    }

    /**
     * HCZ：将用完的ByteBuffer对象还回来，等着下次使用
     */
    public void recycleByteBuffer(ByteBuffer bb) {
        byteBufferCache.put(bb);
    }

    /**
     * HCZ：静态内部类-ByteBuffer的缓存对象
     * A single-element cache of direct byte buffers.
     */
    private static class ByteBufferCache {
        //HCZ：java.nio.ByteBuffer对象
        private ByteBuffer cached;
        //HCZ：get方法
        ByteBuffer get(int capacity) {
            if (capacity < 20480) capacity = 20480;
            //HCZ：有机关——为了重用java.nio.ByteBuffer对象，在不满足条件的情况下就清空此对象内容，进而支持了对象公用但对象内容更新。
            ByteBuffer result =
                (cached != null && cached.capacity() >= capacity)
                ? (ByteBuffer)cached.clear()
                : ByteBuffer.allocate(capacity + capacity>>1);
            cached = null;
            return result;
        }
        //HCZ：set方法，记录java.nio.ByteBuffer对象
        void put(ByteBuffer x) {
            cached = x;
        }
    }

    /**
     * HCZ：从Java源文件对象读取出来的文件内容对应的ByteBuffer对象，进而加入到ByteBuffer缓存中
     */
    private final ByteBufferCache byteBufferCache;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Content cache">

    /**
     * HCZ：获得缓存中指定的"Java源文件对象"对应的"从Java源文件对象读取后的被编码过的CharBuffer对象"
     */
    public CharBuffer getCachedContent(JavaFileObject file) {
        ContentCacheEntry e = contentCache.get(file);
        if (e == null)
            return null;

        if (!e.isValid(file)) {
            contentCache.remove(file);
            return null;
        }

        return e.getValue();
    }

    /**
     * HCZ：缓存中增加新的"Java源文件对象-从Java源文件对象读取后的被编码过的CharBuffer对象"
     */
    public void cache(JavaFileObject file, CharBuffer cb) {
        contentCache.put(file, new ContentCacheEntry(file, cb));
    }

    /**
     * HCZ：缓存中删除"Java源文件对象-从Java源文件对象读取后的被编码过的CharBuffer对象"
     */
    public void flushCache(JavaFileObject file) {
        contentCache.remove(file);
    }

    /**
     * HCZ：key：Java源文件对象，value：对应的内容缓存-有机关
     */
    protected final Map<JavaFileObject, ContentCacheEntry> contentCache
            = new HashMap<JavaFileObject, ContentCacheEntry>();

    protected static class ContentCacheEntry {
        //HCZ：时间戳
        final long timestamp;
        //HCZ：CharBuffer对象的软引用，为了性能
        final SoftReference<CharBuffer> ref;

        //HCZ：构造函数，关键点是创建了CharBuffer对象的软引用
        ContentCacheEntry(JavaFileObject file, CharBuffer cb) {
            this.timestamp = file.getLastModified();
            this.ref = new SoftReference<CharBuffer>(cb);
        }

        //HCZ：如果"Java源文件对象的最后修改时间"与"此内容缓存对象记录的时间"不一致，则认为不合法
        boolean isValid(JavaFileObject file) {
            return timestamp == file.getLastModified();
        }

        //HCZ：读取此内容缓存对象的实际干货——CharBuffer对象
        CharBuffer getValue() {
            return ref.get();
        }
    }
    // </editor-fold>

    /**
     * HCZ:X
     */
    public static Kind getKind(String name) {
        if (name.endsWith(Kind.CLASS.extension))
            return Kind.CLASS;
        else if (name.endsWith(Kind.SOURCE.extension))
            return Kind.SOURCE;
        else if (name.endsWith(Kind.HTML.extension))
            return Kind.HTML;
        else
            return Kind.OTHER;
    }

    /**
     * HCZ:X
     */
    protected static <T> T nullCheck(T o) {
        o.getClass(); // null check
        return o;
    }

    /**
     * HCZ:X
     */
    protected static <T> Collection<T> nullCheck(Collection<T> it) {
        for (T t : it)
            t.getClass(); // null check
        return it;
    }
}
