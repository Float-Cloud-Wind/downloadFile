package com.wsd.core;

import java.util.concurrent.atomic.LongAdder;

/**
 * @program: downloadFile
 * @description: 展示下载信息
 * @author: Mr.Wang
 * @create: 2023-10-25 00:28
 **/
public class DownloadInfoThread implements Runnable{
    //下载文件总大小
    private long httpFileContentLength;

    /*
    Java中，原子类（Atomic classes）是一组线程安全的类，用于在多线程环境下进行原子操作。原子操作是不可分割的操作，要么完全执行，要么完全不执行，不存在中间状态。

Java提供了一系列的原子类，位于java.util.concurrent.atomic包中。这些原子类提供了一些常见的数据类型，如整数、长整数、布尔值等的原子操作。

以下是一些常见的原子类：

- AtomicInteger：提供对整数类型的原子操作。
- AtomicLong：提供对长整数类型的原子操作。
- AtomicBoolean：提供对布尔值类型的原子操作。
- AtomicReference：提供对引用类型的原子操作。
- AtomicIntegerArray：提供对整数数组类型的原子操作。
- AtomicLongArray：提供对长整数数组类型的原子操作。

这些原子类通过使用底层的硬件级别的原子指令或锁机制来确保操作的原子性。它们提供了一些方法，如get()、set()、compareAndSet()等，用于进行原子操作。

使用原子类可以避免多线程环境下的竞态条件和数据不一致的问题，提供了一种线程安全的方式来进行共享数据的操作。

LongAdder是Java中的一个原子类，位于java.util.concurrent.atomic包中。它是AtomicLong的一个改进版本，专门用于高并发环境下对长整数类型进行原子操作。

LongAdder提供了比AtomicLong更高的并发性能，特别适用于高度竞争的多线程环境。它通过将内部的计数器分解为多个单独的计数器，每个线程访问不同的计数器，从而减少了线程之间的竞争。

LongAdder的主要方法包括：

- void add(long x)：将给定的值增加到计数器中。
- void increment()：将计数器增加1。
- void decrement()：将计数器减少1。
- long sum()：返回当前计数器的总和。
- void reset()：重置计数器为0。

使用LongAdder时，可以通过调用add()、increment()、decrement()等方法来进行原子操作，而无需使用显式的锁或同步机制。这使得LongAdder在高并发场景下具有更好的性能和可伸缩性。

总之，LongAdder是一个用于高并发环境下对长整数类型进行原子操作的原子类，通过分解计数器来减少线程之间的竞争，提供了更好的性能和可伸缩性。
     */
    //累计下载的大小
    public static volatile LongAdder downSize = new LongAdder();;

    //前一次(秒)累计下载的大小
    private double prevSize;

    public DownloadInfoThread(long httpFileContentLength){
        this.httpFileContentLength = httpFileContentLength;
    }

    @Override
    public void run(){
        //计算文件总大小 单位：mb
        String httpFileSize = String.format("%.2f", httpFileContentLength / (1024d * 1024d));

        //计算每秒下载速度 kb
        int speed = (int)((downSize.doubleValue() - prevSize) / 1024d);
        prevSize = downSize.doubleValue();

        //剩余文件的大小
        double remainSize = httpFileContentLength  - downSize.doubleValue();

        //计算剩余时间
        //当你尝试将正无穷大的浮点数值格式化为字符串时，它会被转化为字符串 "Infinity"(when download speed too slow,speed -> 0)
        String remainTime = String.format("%.1f", remainSize / 1024d / speed);

        if ("Infinity".equalsIgnoreCase(remainTime)) {
            remainTime = "-";
        }

        //已下载大小
        String currentFileSize = String.format("%.2f", downSize.doubleValue() / (1024d * 1024d));

        //This method 每间隔一秒执行一次
        String downInfo = String.format("已下载 %smb/%smb,速度 %skb/s,剩余时间 %ss",
                currentFileSize, httpFileSize, speed, remainTime);

        //回车，每间隔一秒输出一次，输出在同一行
        System.out.print("\r");
        System.out.print(downInfo);
    }

    public long getHttpFileContentLength() {
        return httpFileContentLength;
    }

    public void setHttpFileContentLength(long httpFileContentLength) {
        this.httpFileContentLength = httpFileContentLength;
    }

    public LongAdder getDownSize() {
        return downSize;
    }



    public double getPrevSize() {
        return prevSize;
    }

    public void setPrevSize(double prevSize) {
        this.prevSize = prevSize;
    }
}
