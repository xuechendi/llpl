/* 
 * Copyright (C) 2018 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package examples;

import lib.llpl.*;
import java.util.Arrays;
import java.util.concurrent.*;
import org.apache.commons.cli.*;


class ArgParser {

    CommandLine cmd;
    Options options = new Options();
    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    ArgParser (String[] args) {

        Option device = new Option("d", "device", true, "pmem device path");
        device.setRequired(true);
        options.addOption(device);

        Option size = new Option("s", "size", true, "input total data size(GB)");
        size.setRequired(true);
        options.addOption(size);

        Option thread_num = new Option("t", "thread_num", true, "parallel threads number");
        thread_num.setRequired(true);
        options.addOption(thread_num);

        Option block_size = new Option("bs", "block_size", true, "block size for each request");
        block_size.setRequired(true);
        options.addOption(block_size);

        try {
            this.cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

    }

    public String get(String key) {
	String ret = "";
        ret = this.cmd.getOptionValue(key);
	return ret;
    }
}

class Monitor {
    long committedJobs = 0;
    boolean alive = true;
    int bs;
    ExecutorService monitor_thread;
	Monitor (int bs) {
            this.bs = bs;
	    this.monitor_thread = Executors.newFixedThreadPool(1);
            this.monitor_thread.submit(this::run);
	}

	void run () {
        long last_committed_jobs = 0;
	    int elapse_sec = 0;
        while(alive) {
            System.out.println("Second " + elapse_sec + "(MB/s): " + (this.committedJobs - last_committed_jobs) * this.bs );
	        last_committed_jobs = this.committedJobs;
	        elapse_sec += 1;
	        try {
	            Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.exit(1);
	        }
	    }
    }

    synchronized void incCommittedJobs() {
        this.committedJobs += 1;
    }

	void stop() {
	  this.alive = false;
	  this.monitor_thread.shutdown();
	}
}

public class Writer {
    Heap h = null;
    String device;
    long size;
    int thread_num;
    byte[] bytes;
    ExecutorService executor;
    Monitor monitor;

    Writer (Monitor monitor) {
        this.monitor = monitor;
    }


    synchronized MemoryBlock<Transactional> allocateMemory(long length) {
    //MemoryBlock<Transactional> allocateMemory(long length) {
        return this.h.allocateMemoryBlock(Transactional.class, Integer.BYTES + length);
    }

    private void write() {
	int length = this.bytes.length;
	MemoryBlock<Transactional> mr = allocateMemory(length);
        mr.copyFromArray(this.bytes, 0, 0, length);
        this.monitor.incCommittedJobs();
    }


    public void run(String dev, int bs, long size, int thread_num) {
	this.device = dev;
	this.size = size;
	this.thread_num = thread_num;
        System.out.println("Thread Num: " + this.thread_num + ", Data size: " + this.size + "MB, Device: " + dev);
        this.h = Heap.getHeap(dev, 1024*1024*1024L);
	// multi write to aep for testing

        /*Console c = System.console();
        c.readLine("press Enter to start");*/
        
        bytes = new byte[bs * 1024 * 1024];
        Arrays.fill(this.bytes, (byte)'a');
       
        long total_jobs = this.size / bs;

	long remained_jobs = total_jobs;


        System.out.println("Start to run, total jobs: " + total_jobs);
	this.executor = Executors.newFixedThreadPool(this.thread_num);
	while (remained_jobs > 0) {
          this.executor.submit(this::write);
	  remained_jobs -= 1;
	}
    }

    public void wait_to_stop() {
	try {
	    this.executor.awaitTermination(60, TimeUnit.SECONDS);
	} catch (InterruptedException ie) {
            this.executor.shutdown();
	}
        System.out.println("Completed!");
    }

    public void stop() {
        this.executor.shutdown();
    }


    public static void main(String[] args) {
	//String[] device_list = {"/dev/dax0.0", "/dev/dax0.1", "/dev/dax0.2", "/dev/dax0.3", "/dev/dax0.5","/dev/dax1.0", "/dev/dax1.1", "/dev/dax1.2", "/dev/dax1.3", "/dev/dax1.5"};
	ArgParser arg_parser = new ArgParser(args);
	String[] device_list = arg_parser.get("device").trim().split("\\s*,\\s*", -1);
        long size = Integer.parseInt(arg_parser.get("size")) * 1024;
        int thread_num = Integer.parseInt(arg_parser.get("thread_num"));
        int bs = Integer.parseInt(arg_parser.get("block_size"));

	Writer[] writer = new Writer[device_list.length];
	Monitor monitor = new Monitor(bs);
	for (int i = 0; i < device_list.length; i++) { 
	    writer[i] = new Writer(monitor);
	    writer[i].run(device_list[i], bs, size, thread_num);
	}
	writer[0].wait_to_stop();
	for (int i = 1; i < device_list.length; i++) { 
	    writer[i].stop();
	}
	monitor.stop();
	System.exit(0);
    }
}
