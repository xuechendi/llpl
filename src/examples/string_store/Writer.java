/* 
 * Copyright (C) 2018 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package examples.string_store;

import lib.llpl.*;
import java.io.Console;
import java.util.Arrays;
import sun.misc.Unsafe;

public class Writer {
    public static void main(String[] args) {
        Heap h = Heap.getHeap("/mnt/mem/persistent_pool", 1024*1024*1024L);

        Console c = System.console();
        c.readLine("press Enter to start");
        /*
        byte[] bytes = new byte[32*1024*1024];
        Arrays.fill(bytes, (byte)'a');
        */
        byte[] bytes = {(byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e',
                        (byte)'f', (byte)'g', (byte)'h', (byte)'i', (byte)'j',
                        (byte)'k', (byte)'l', (byte)'m', (byte)'n', (byte)'o',
                        (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t'};
        int length = bytes.length;

        MemoryBlock<Transactional> mr = h.allocateMemoryBlock(Transactional.class, Integer.BYTES + length);
        mr.copyFromArray(bytes, 0, 0, length);

        h.setRoot(mr.address());

        System.out.println("String successfully written.");

        long addr = mr.payloadAddress(10);
        Unsafe UNSAFE;
        try {
            java.lang.reflect.Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe)f.get(null);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to initialize UNSAFE.");
        }
        byte res = UNSAFE.getByte(addr);
        System.out.println("Address 10 is " + (char)res); 
    }
}
