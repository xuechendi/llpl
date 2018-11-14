/* 
 * Copyright (C) 2018 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package examples.string_store;

import lib.llpl.*;
import java.io.Console;

public class Reader {
    public static void main(String[] args) {
        Heap h = Heap.getHeap("/mnt/mem/persistent_pool", 2147483648L);
        int length = 32*1024*1024;

        long rootAddr = h.getRoot();
        if (rootAddr == 0) {
            System.out.println("No string found!");
            System.exit(0);
        }
        MemoryBlock<Transactional> mr = h.memoryBlockFromAddress(Transactional.class, rootAddr);
        byte[] bytes = new byte[length];
        mr.copyToArray(0, bytes, 0, length - 1);

        for (int j=0; j<bytes.length; j++) {
            System.out.format("%02X ", bytes[j]);
        }
        System.out.println();
    }
}
