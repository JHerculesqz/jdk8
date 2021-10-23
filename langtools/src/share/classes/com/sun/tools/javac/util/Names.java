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

package com.sun.tools.javac.util;

/**
 * HCZ：Names对象，包含N个Name对象，所属的Table对象
 *
 * Access to the compiler's name table.  STandard names are defined,
 * as well as methods to create new names.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Names {
    /**
     * HCZ：记录Names对象
     */
    public static final Context.Key<Names> namesKey = new Context.Key<Names>();

    /**
     * HCZ：从Context对象中获得缓存的Names对象，如果没有则创建
     */
    public static Names instance(Context context) {
        Names instance = context.get(namesKey);
        if (instance == null) {
            instance = new Names(context);
            context.put(namesKey, instance);
        }
        return instance;
    }

    // operators and punctuation
    /**
     * HCZ：星号
     */
    public final Name asterisk;
    /**
     * HCZ：逗号
     */
    public final Name comma;
    /**
     * HCZ：空字符串
     */
    public final Name empty;
    /**
     * HCZ：连字符-
     */
    public final Name hyphen;
    /**
     * HCZ：1
     */
    public final Name one;
    /**
     * HCZ：句号.
     */
    public final Name period;
    /**
     * HCZ：分号;
     */
    public final Name semicolon;
    /**
     * HCZ：斜杠/
     */
    public final Name slash;
    /**
     * HCZ：/=
     */
    public final Name slashequals;

    // keywords
    /**
     * HCZ：关键字class
     */
    public final Name _class;
    /**
     * HCZ：关键字default
     */
    public final Name _default;
    /**
     * HCZ：关键字super
     */
    public final Name _super;
    /**
     * HCZ：关键字this
     */
    public final Name _this;

    // field and method names
    /**
     * HCZ：关键字name
     */
    public final Name _name;
    /**
     * HCZ：关键字addSuppressed
     */
    public final Name addSuppressed;
    /**
     * HCZ：关键字<any>
     */
    public final Name any;
    /**
     * HCZ：?关键字append
     */
    public final Name append;
    /**
     * HCZ：关键字<clinit>
     */
    public final Name clinit;
    /**
     * HCZ：方法clone
     */
    public final Name clone;
    /**
     * HCZ：方法close
     */
    public final Name close;
    /**
     * HCZ：方法compareTo
     */
    public final Name compareTo;
    /**
     * HCZ：?lambda-$deserializeLambda$
     */
    public final Name deserializeLambda;
    /**
     * HCZ：?关键字desiredAssertionStatus
     */
    public final Name desiredAssertionStatus;
    /**
     * HCZ：方法equals
     */
    public final Name equals;
    /**
     * HCZ：标签<error>
     */
    public final Name error;
    /**
     * HCZ：?family
     */
    public final Name family;
    /**
     * HCZ：方法finalize
     */
    public final Name finalize;
    /**
     * HCZ：方法forName
     */
    public final Name forName;
    /**
     * HCZ：方法getClass
     */
    public final Name getClass;
    /**
     * HCZ：方法getClassLoader
     */
    public final Name getClassLoader;
    /**
     * HCZ：方法getComponentType
     */
    public final Name getComponentType;
    /**
     * HCZ：方法getDeclaringClass
     */
    public final Name getDeclaringClass;
    /**
     * HCZ：方法getMessage
     */
    public final Name getMessage;
    /**
     * HCZ：方法hasNext
     */
    public final Name hasNext;
    /**
     * HCZ：方法hashCode
     */
    public final Name hashCode;
    /**
     * HCZ：方法<init>
     */
    public final Name init;
    /**
     * HCZ：?方法initCause
     */
    public final Name initCause;
    /**
     * HCZ：方法iterator
     */
    public final Name iterator;
    /**
     * HCZ：属性length
     */
    public final Name length;
    /**
     * HCZ：方法next
     */
    public final Name next;
    /**
     * HCZ：?ordinal
     */
    public final Name ordinal;
    /**
     * HCZ：属性serialVersionUID
     */
    public final Name serialVersionUID;
    /**
     * HCZ：方法toString
     */
    public final Name toString;
    /**
     * HCZ：属性value
     */
    public final Name value;
    /**
     * HCZ：方法valueOf
     */
    public final Name valueOf;
    /**
     * HCZ：属性values
     */
    public final Name values;

    // class names
    /**
     * HCZ：接口Serializable
     */
    public final Name java_io_Serializable;
    /**
     * HCZ：接口AutoCloseable
     */
    public final Name java_lang_AutoCloseable;
    /**
     * HCZ：类Class
     */
    public final Name java_lang_Class;
    /**
     * HCZ：接口Cloneable
     */
    public final Name java_lang_Cloneable;
    /**
     * HCZ：类Enum
     */
    public final Name java_lang_Enum;
    /**
     * HCZ：类Object
     */
    public final Name java_lang_Object;
    /**
     * HCZ：类MethodHandle
     */
    public final Name java_lang_invoke_MethodHandle;

    // names of builtin classes
    /**
     * HCZ：内置类Array
     */
    public final Name Array;
    /**
     * HCZ：内置类Bound
     */
    public final Name Bound;
    /**
     * HCZ：内置类Method
     */
    public final Name Method;

    // package names
    /**
     * HCZ：包java.lang
     */
    public final Name java_lang;

    // attribute names
    /**
     * HCZ：？
     */
    public final Name Annotation;
    /**
     * HCZ：？
     */
    public final Name AnnotationDefault;
    /**
     * HCZ：？
     */
    public final Name BootstrapMethods;
    /**
     * HCZ：？
     */
    public final Name Bridge;
    /**
     * HCZ：？
     */
    public final Name CharacterRangeTable;
    /**
     * HCZ：？
     */
    public final Name Code;
    /**
     * HCZ：？
     */
    public final Name CompilationID;
    /**
     * HCZ：？
     */
    public final Name ConstantValue;
    /**
     * HCZ：注解Deprecated
     */
    public final Name Deprecated;
    /**
     * HCZ：？
     */
    public final Name EnclosingMethod;
    /**
     * HCZ：？
     */
    public final Name Enum;
    /**
     * HCZ：？
     */
    public final Name Exceptions;
    /**
     * HCZ：？
     */
    public final Name InnerClasses;
    /**
     * HCZ：？
     */
    public final Name LineNumberTable;
    /**
     * HCZ：？
     */
    public final Name LocalVariableTable;
    /**
     * HCZ：？
     */
    public final Name LocalVariableTypeTable;
    /**
     * HCZ：？
     */
    public final Name MethodParameters;
    /**
     * HCZ：？
     */
    public final Name RuntimeInvisibleAnnotations;
    /**
     * HCZ：？
     */
    public final Name RuntimeInvisibleParameterAnnotations;
    /**
     * HCZ：？
     */
    public final Name RuntimeInvisibleTypeAnnotations;
    /**
     * HCZ：？
     */
    public final Name RuntimeVisibleAnnotations;
    /**
     * HCZ：？
     */
    public final Name RuntimeVisibleParameterAnnotations;
    /**
     * HCZ：？
     */
    public final Name RuntimeVisibleTypeAnnotations;
    /**
     * HCZ：？
     */
    public final Name Signature;
    /**
     * HCZ：？
     */
    public final Name SourceFile;
    /**
     * HCZ：？
     */
    public final Name SourceID;
    /**
     * HCZ：？
     */
    public final Name StackMap;
    /**
     * HCZ：？
     */
    public final Name StackMapTable;
    /**
     * HCZ：？
     */
    public final Name Synthetic;
    /**
     * HCZ：？
     */
    public final Name Value;
    /**
     * HCZ：？
     */
    public final Name Varargs;

    // members of java.lang.annotation.ElementType
    /**
     * HCZ：注解的ElementType-ANNOTATION_TYPE
     */
    public final Name ANNOTATION_TYPE;
    /**
     * HCZ：注解的ElementType-CONSTRUCTOR
     */
    public final Name CONSTRUCTOR;
    /**
     * HCZ：注解的ElementType-FIELD
     */
    public final Name FIELD;
    /**
     * HCZ：注解的ElementType-LOCAL_VARIABLE
     */
    public final Name LOCAL_VARIABLE;
    /**
     * HCZ：注解的ElementType-METHOD
     */
    public final Name METHOD;
    /**
     * HCZ：注解的ElementType-PACKAGE
     */
    public final Name PACKAGE;
    /**
     * HCZ：注解的ElementType-PARAMETER
     */
    public final Name PARAMETER;
    /**
     * HCZ：注解的ElementType-TYPE
     */
    public final Name TYPE;
    /**
     * HCZ：注解的ElementType-TYPE_PARAMETER
     */
    public final Name TYPE_PARAMETER;
    /**
     * HCZ：注解的ElementType-TYPE_USE
     */
    public final Name TYPE_USE;

    // members of java.lang.annotation.RetentionPolicy
    /**
     * HCZ：注解的RetentionPolicy-CLASS
     */
    public final Name CLASS;
    /**
     * HCZ：注解的RetentionPolicy-RUNTIME
     */
    public final Name RUNTIME;
    /**
     * HCZ：注解的RetentionPolicy-SOURCE
     */
    public final Name SOURCE;

    // other identifiers
    /**
     * HCZ：泛型T
     */
    public final Name T;
    /**
     * HCZ：标记deprecated
     */
    public final Name deprecated;
    /**
     * HCZ：异常变量ex
     */
    public final Name ex;
    /**
     * HCZ：package_info
     */
    public final Name package_info;

    //lambda-related
    /**
     * HCZ：lambda$
     */
    public final Name lambda;
    /**
     * HCZ：？metafactory
     */
    public final Name metafactory;
    /**
     * HCZ：？altMetafactory
     */
    public final Name altMetafactory;

    /**
     * HCZ：所属的Table对象
     */
    public final Name.Table table;

    /**
     * HCZ：构造函数，赋初值
     */
    public Names(Context context) {
        //HCZ：创建Name.Table
        Options options = Options.instance(context);
        table = createTable(options);

        // operators and punctuation
        asterisk = fromString("*");
        comma = fromString(",");
        empty = fromString("");
        hyphen = fromString("-");
        one = fromString("1");
        period = fromString(".");
        semicolon = fromString(";");
        slash = fromString("/");
        slashequals = fromString("/=");

        // keywords
        _class = fromString("class");
        _default = fromString("default");
        _super = fromString("super");
        _this = fromString("this");

        // field and method names
        _name = fromString("name");
        addSuppressed = fromString("addSuppressed");
        any = fromString("<any>");
        append = fromString("append");
        clinit = fromString("<clinit>");
        clone = fromString("clone");
        close = fromString("close");
        compareTo = fromString("compareTo");
        deserializeLambda = fromString("$deserializeLambda$");
        desiredAssertionStatus = fromString("desiredAssertionStatus");
        equals = fromString("equals");
        error = fromString("<error>");
        family = fromString("family");
        finalize = fromString("finalize");
        forName = fromString("forName");
        getClass = fromString("getClass");
        getClassLoader = fromString("getClassLoader");
        getComponentType = fromString("getComponentType");
        getDeclaringClass = fromString("getDeclaringClass");
        getMessage = fromString("getMessage");
        hasNext = fromString("hasNext");
        hashCode = fromString("hashCode");
        init = fromString("<init>");
        initCause = fromString("initCause");
        iterator = fromString("iterator");
        length = fromString("length");
        next = fromString("next");
        ordinal = fromString("ordinal");
        serialVersionUID = fromString("serialVersionUID");
        toString = fromString("toString");
        value = fromString("value");
        valueOf = fromString("valueOf");
        values = fromString("values");

        // class names
        java_io_Serializable = fromString("java.io.Serializable");
        java_lang_AutoCloseable = fromString("java.lang.AutoCloseable");
        java_lang_Class = fromString("java.lang.Class");
        java_lang_Cloneable = fromString("java.lang.Cloneable");
        java_lang_Enum = fromString("java.lang.Enum");
        java_lang_Object = fromString("java.lang.Object");
        java_lang_invoke_MethodHandle = fromString("java.lang.invoke.MethodHandle");

        // names of builtin classes
        Array = fromString("Array");
        Bound = fromString("Bound");
        Method = fromString("Method");

        // package names
        java_lang = fromString("java.lang");

        // attribute names
        Annotation = fromString("Annotation");
        AnnotationDefault = fromString("AnnotationDefault");
        BootstrapMethods = fromString("BootstrapMethods");
        Bridge = fromString("Bridge");
        CharacterRangeTable = fromString("CharacterRangeTable");
        Code = fromString("Code");
        CompilationID = fromString("CompilationID");
        ConstantValue = fromString("ConstantValue");
        Deprecated = fromString("Deprecated");
        EnclosingMethod = fromString("EnclosingMethod");
        Enum = fromString("Enum");
        Exceptions = fromString("Exceptions");
        InnerClasses = fromString("InnerClasses");
        LineNumberTable = fromString("LineNumberTable");
        LocalVariableTable = fromString("LocalVariableTable");
        LocalVariableTypeTable = fromString("LocalVariableTypeTable");
        MethodParameters = fromString("MethodParameters");
        RuntimeInvisibleAnnotations = fromString("RuntimeInvisibleAnnotations");
        RuntimeInvisibleParameterAnnotations = fromString("RuntimeInvisibleParameterAnnotations");
        RuntimeInvisibleTypeAnnotations = fromString("RuntimeInvisibleTypeAnnotations");
        RuntimeVisibleAnnotations = fromString("RuntimeVisibleAnnotations");
        RuntimeVisibleParameterAnnotations = fromString("RuntimeVisibleParameterAnnotations");
        RuntimeVisibleTypeAnnotations = fromString("RuntimeVisibleTypeAnnotations");
        Signature = fromString("Signature");
        SourceFile = fromString("SourceFile");
        SourceID = fromString("SourceID");
        StackMap = fromString("StackMap");
        StackMapTable = fromString("StackMapTable");
        Synthetic = fromString("Synthetic");
        Value = fromString("Value");
        Varargs = fromString("Varargs");

        // members of java.lang.annotation.ElementType
        ANNOTATION_TYPE = fromString("ANNOTATION_TYPE");
        CONSTRUCTOR = fromString("CONSTRUCTOR");
        FIELD = fromString("FIELD");
        LOCAL_VARIABLE = fromString("LOCAL_VARIABLE");
        METHOD = fromString("METHOD");
        PACKAGE = fromString("PACKAGE");
        PARAMETER = fromString("PARAMETER");
        TYPE = fromString("TYPE");
        TYPE_PARAMETER = fromString("TYPE_PARAMETER");
        TYPE_USE = fromString("TYPE_USE");

        // members of java.lang.annotation.RetentionPolicy
        CLASS = fromString("CLASS");
        RUNTIME = fromString("RUNTIME");
        SOURCE = fromString("SOURCE");

        // other identifiers
        T = fromString("T");
        deprecated = fromString("deprecated");
        ex = fromString("ex");
        package_info = fromString("package-info");

        //lambda-related
        lambda = fromString("lambda$");
        metafactory = fromString("metafactory");
        altMetafactory = fromString("altMetafactory");
    }

    /**
     * HCZ：创建SharedNameTable对象或UnSharedNameTable对象
     */
    protected Name.Table createTable(Options options) {
        boolean useUnsharedTable = options.isSet("useUnsharedTable");
        if (useUnsharedTable)
            return new UnsharedNameTable(this);
        else
            return new SharedNameTable(this);
    }

    /**
     * HCZ：销毁table对象
     */
    public void dispose() {
        table.dispose();
    }

    /**
     * HCZ：调用Table对象的方法，从cs对象(char[])中读取指定的char子数组，更新Table对象，并转换成Name对象
     */
    public Name fromChars(char[] cs, int start, int len) {
        return table.fromChars(cs, start, len);
    }

    /**
     * HCZ：调用Table对象的方法，从String对象中读取指定的char子数组，更新Table对象，并转换成Name对象
     */
    public Name fromString(String s) {
        return table.fromString(s);
    }

    /**
     * HCZ：调用Table对象的方法，从cs对象(byte[])中读取指定的byte子数组，更新Table对象，转换成Name对象
     */
    public Name fromUtf(byte[] cs) {
        return table.fromUtf(cs);
    }

    /**
     * HCZ：调用Table对象的方法，从cs对象(byte[])中读取指定的byte子数组，更新Table对象，转换成Name对象
     */
    public Name fromUtf(byte[] cs, int start, int len) {
        return table.fromUtf(cs, start, len);
    }
}
