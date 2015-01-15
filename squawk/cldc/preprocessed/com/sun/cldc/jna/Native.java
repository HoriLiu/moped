/*
 * Copyright 2004-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.cldc.jna;

import com.sun.squawk.VM;

/**
 * Generic machinery to support access to native code.
 * 
 * <h3>Differences from JNA</h3>
 * <ul>
 * <li> Throws RuntimeExceptions instead of UnsatisfiedLinkErrors. Are link errors really "unrecoverable"? Platform independant code might want to work around missing functions.
 * <li> Search paths unimplemented
 * <li> Calling conventions unimplemented
 * <li> no finalization in cldc, need to call dispose() explicitly (could add a shutdownhook though).
 * <li> no parseVersion();
 * <li> no getFile()
 * </ul>
 */
public class Native {
    public final static boolean DEBUG = false;
    
    // TODO: Fix these
    public static final int POINTER_SIZE = 4;
    public static final int LONG_SIZE = 4;
    public static final int WCHAR_SIZE = 2;

    public final static String DEFAULT = "RTLD";
            
    private Native() {}

    /** kludge to pass loading library to the implementation class! */
    private static volatile NativeLibrary libraryLoading;
    private static final Object lock = new Object();

    /**
     * Kludge to pass loading library to default constructor of
     * implementation class.
     * @return NativeLibrary or null if not loading a NativeLibrary.
     */
    public static NativeLibrary getLibraryLoading() {
        return libraryLoading;
    }

    private static Class getImplClass(Class interfaceClass) {
        Class implClass = null;
        String interfaceName = interfaceClass.getName();
        int pos = interfaceName.lastIndexOf('.');
        String packageName = interfaceName.substring(0, pos);
        String intfNameStem = interfaceName.substring(pos + 1);
        // look for platform-specific name first:
        try {
            if (packageName.endsWith(".natives")) {
                packageName = packageName.substring(0, packageName.length() - ".natives".length());
            }
            String platformImplName = packageName + '.' + Platform.getPlatform().platformName() + ".natives." + intfNameStem + "Impl";
            if (DEBUG) {
                System.out.println("Looking for implementation class " + platformImplName);
            }
            implClass = Class.forName(platformImplName);
        } catch (ClassNotFoundException ex) {
            try {
                implClass = Class.forName(interfaceName + "Impl");
            } catch (ClassNotFoundException ex1) {
                ex1.printStackTrace();
            }
        }

        return implClass;
    }

    public static Library loadLibrary(String name, Class interfaceClass) {
        try {
            Object implementation = null;
            synchronized (lock) {
                try {
                    String customName = Platform.commonLibraryMapping(name);
                    if (customName != null) {
                        name = customName;
                    }

                    if (name == null || name.length() == 0 || name.equals(DEFAULT)) {
                        libraryLoading = NativeLibrary.getDefaultInstance();
                    } else {
                        libraryLoading = NativeLibrary.getInstance(name);
                    }
                    Class implClass = getImplClass(interfaceClass);
                    implementation = implClass.newInstance();
                } finally {
                    libraryLoading = null;
                }
            }
            return (Library) implementation;
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        return null;
    }
   
   /**
    * a bit of sanity checking that every "constant index" is really unique and every one is checked.
    * once this is autogenerated this will be less necessary
    * @param checkArray the current value of the check array (may be null)
    * @param length the length of the check array
    * @param index the index being checked.
    * @return the checkArray object
    */
    public static boolean[] doInitCheck(boolean[] checkArray, int length, int index) {
        if (checkArray == null) {
            checkArray = new boolean[length];
        }
        if (checkArray[index]) {
            throw new RuntimeException("index already checked: " + index);
        }
        checkArray[index] = true;
        if (index == checkArray.length - 1) {
            for (int i = 0; i < checkArray.length; i++) {
                if (!checkArray[index]) {
                    throw new RuntimeException("index never checked: " + index);
                }
            }
        }
        return checkArray;
    }
}