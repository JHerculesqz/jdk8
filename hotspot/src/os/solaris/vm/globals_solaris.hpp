/*
 * Copyright 2005-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

//
// Defines Solaris specific flags. They are not available on other platforms.
//
#define RUNTIME_OS_FLAGS(develop, develop_pd, product, product_pd, diagnostic, notproduct) \
                                                                               \
  product(bool, UseISM, false,                                                 \
          "Use Intimate Shared Memory (Solaris Only)")                         \
                                                                               \
  product(bool, UsePermISM, false,                                             \
          "Obsolete flag for compatibility (same as UseISM)")                  \
                                                                               \
  product(bool, UseMPSS, true,                                                 \
          "Use Multiple Page Size Support (Solaris 9 Only)")                   \
                                                                               \
  product(bool, UseExtendedFileIO, true,                                       \
          "Enable workaround for limitations of stdio FILE structure")

//
// Defines Solaris-specific default values. The flags are available on all
// platforms, but they may have different default values on other platforms.
//
define_pd_global(bool, UseLargePages, true);
define_pd_global(bool, UseOSErrorReporting, false);
define_pd_global(bool, UseThreadPriorities, false);