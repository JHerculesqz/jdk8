/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import com.sun.tools.javac.main.Option;
import static com.sun.tools.javac.main.Option.*;

/**
 * HCZ：记录javac的命令行参数的name和value集合
 *
 *  A table of all command-line options.
 *  If an option has an argument, the option name is mapped to the argument.
 *  If a set option has no argument, it is mapped to itself.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Options {
    /**
     * HCZ:X
     */
    private static final long serialVersionUID = 0;

    /**
     * HCZ：X
     *
     * The context key for the options. */
    public static final Context.Key<Options> optionsKey =
        new Context.Key<Options>();

    /**
     * HCZ:key是option的text，value是option的value
     */
    private LinkedHashMap<String,String> values;

    /**
     * HCZ:X
     *
     *  Get the Options instance for this context. */
    public static Options instance(Context context) {
        Options instance = context.get(optionsKey);
        if (instance == null)
            instance = new Options(context);
        return instance;
    }

    /**
     * HCZ:X
     */
    protected Options(Context context) {
// DEBUGGING -- Use LinkedHashMap for reproducability
        values = new LinkedHashMap<String,String>();
        context.put(optionsKey, this);
    }

    /**
     * HCZ：根据name，获得option的value
     *
     * Get the value for an undocumented option.
     */
    public String get(String name) {
        return values.get(name);
    }

    /**
     * HCZ：根据option对象，获得option的value
     *
     * Get the value for an option.
     */
    public String get(Option option) {
        return values.get(option.text);
    }

    /**
     * HCZ：根据option的name，获得option的value的布尔值
     *
     * Get the boolean value for an option, patterned after Boolean.getBoolean,
     * essentially will return true, iff the value exists and is set to "true".
     */
    public boolean getBoolean(String name) {
        return getBoolean(name, false);
    }

    /**
     * HCZ：根据option对象的name，获得option的value的布尔值，如果找不到则返回默认值
     *
     * Get the boolean with a default value if the option is not set.
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        String value = get(name);
        return (value == null) ? defaultValue : Boolean.parseBoolean(value);
    }

    /**
     * HCZ：根据option的name，判断javac的命令行参数中已经设置了该option
     *
     * Check if the value for an undocumented option has been set.
     */
    public boolean isSet(String name) {
        return (values.get(name) != null);
    }

    /**
     * HCZ：根据option对象，判断javac的命令行参数中已经设置了该option
     *
     * Check if the value for an option has been set.
     */
    public boolean isSet(Option option) {
        return (values.get(option.text) != null);
    }

    /**
     * HCZ：根据option的name+option的value，判断javac的命令行参数中已经设置了该option——特别用于choiceOption
     *
     * Check if the value for a choice option has been set to a specific value.
     */
    public boolean isSet(Option option, String value) {
        return (values.get(option.text + value) != null);
    }

    /**
     * HCZ：根据option的name，判断javac的命令行参数中没有设置该option
     *
     * Check if the value for an undocumented option has not been set.
     */
    public boolean isUnset(String name) {
        return (values.get(name) == null);
    }

    /**
     * HCZ：根据option对象，判断javac的命令行参数中没有设置该option
     *
     * Check if the value for an option has not been set.
     */
    public boolean isUnset(Option option) {
        return (values.get(option.text) == null);
    }

    /**
     * HCZ：根据option的name+option的value，判断javac的命令行参数中没有设置该option——特别用于choiceOption
     *
     * Check if the value for a choice option has not been set to a specific value.
     */
    public boolean isUnset(Option option, String value) {
        return (values.get(option.text + value) == null);
    }

    /**
     * HCZ:向options对象中添加新的option对象
     */
    public void put(String name, String value) {
        values.put(name, value);
    }

    /**
     * HCZ:向options对象中添加新的option对象
     */
    public void put(Option option, String value) {
        values.put(option.text, value);
    }

    /**
     * HCZ:X
     */
    public void putAll(Options options) {
        values.putAll(options.values);
    }

    /**
     * HCZ:根据option的name，删除option的value
     */
    public void remove(String name) {
        values.remove(name);
    }

    /**
     * HCZ:得到所有的option对象的name集合
     */
    public Set<String> keySet() {
        return values.keySet();
    }

    /**
     * HCZ：获得option对象集合的size
     */
    public int size() {
        return values.size();
    }

    // light-weight notification mechanism
    /**
     * HCZ：轻量级通知机制-记录Runnable对象形式的监听器
     */
    private List<Runnable> listeners = List.nil();
    /**
     * HCZ：轻量级通知机制-添加Runnable对象形式的监听器
     */
    public void addListener(Runnable listener) {
        listeners = listeners.prepend(listener);
    }
    /**
     * HCZ：轻量级通知机制-遍历已经存在的Runnable对象形式的监听器集合，触发Runnable对象#run()方法
     */
    public void notifyListeners() {
        for (Runnable r: listeners)
            r.run();
    }

    /**
     * HCZ：检查xlint命令的子项配置
     *
     *  Check for a lint suboption. */
    public boolean lint(String s) {
        // return true if either the specific option is enabled, or
        // they are all enabled without the specific one being
        // disabled
        return
            isSet(XLINT_CUSTOM, s) ||
            (isSet(XLINT) || isSet(XLINT_CUSTOM, "all")) &&
                isUnset(XLINT_CUSTOM, "-" + s);
    }
}
