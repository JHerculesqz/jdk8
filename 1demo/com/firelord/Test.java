package com.firelord;

public class Test {
    static {
        System.load("/home/hulk/Documents/eclipse_workspace/jdk8-jdk8-b132/1demo/lib.so");
    }

    public void start() {
        start0();
    }

    private native void start0();

    public void run() {
        System.out.println("Java侧run方法被回调");
    }

    public static void main(String[] args) {
        Test myThread = new Test();
        myThread.start();
    }
}
