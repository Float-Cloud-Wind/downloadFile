package com.wsd.core;


import com.wsd.util.HttpUtil;
import com.wsd.util.LogUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/*
    分块载任务

    在Java中，Callable和Runnable是两个接口，用于定义可执行的任务。它们之间的主要区别如下：

1. 返回值：Callable接口的call()方法可以返回一个结果，而Runnable接口的run()方法没有返回值。Callable接口的返回值可以通过Future对象获取。

2. 异常处理：Callable接口的call()方法可以抛出受检查的异常，允许在任务执行过程中处理异常情况。而Runnable接口的run()方法不能抛出受检查的异常，只能在方法内部进行异常处理。

3. 使用方式：Callable接口的实例可以提交给ExecutorService的submit()方法，以便在线程池中执行，并返回一个Future对象。而Runnable接口的实例可以提交给ExecutorService的execute()方法，以便在线程池中执行，但无法获取任务的返回值。

4. 并发控制：Callable接口的call()方法可以返回一个结果，可以用于实现并发控制，例如等待多个任务完成后再继续执行。而Runnable接口没有返回值，无法直接实现并发控制。

总结起来，Callable接口和Runnable接口都用于定义可执行的任务，但Callable接口支持返回值和异常处理，可以用于实现并发控制，而Runnable接口没有返回值和异常处理，适用于简单的异步任务。选择使用哪个接口取决于任务的需求和场景。

 */
public class DownloaderTask implements Callable<Boolean> {

    private String url;

    //下载的起始位置
    private long startPos;

    //下载的结束位置
    private long endPos;

    //标识当前是哪一部分
    private int part;

    //下载内容保存路径
    private String path;

    //线程计数器
    private CountDownLatch countDownLatch;

    public DownloaderTask(String url, long startPos, long endPos, int part, String path, CountDownLatch countDownLatch) {
        this.url = url;
        this.startPos = startPos;
        this.endPos = endPos;
        this.part = part;
        this.path = path;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public Boolean call() throws Exception {
        //获取文件名
        //String httpFileName = HttpUtil.getFileName(url);
        //分块的文件名
        String httpFileName = path + ".temp" + part;
        //下载路径
        //httpFileName = path + httpFileName;

        //获取分块下载的连接
        HttpURLConnection httpURLConnection = HttpUtil.getHttpURLConnection(url, startPos, endPos);

        InputStream input = httpURLConnection.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(input);

        /*
        1. RandomAccessFile是Java提供的一个类，用于对文件进行随机访问。它可以在文件中的任意位置进行读取和写入操作。

        2. "rw"是RandomAccessFile的打开模式，其中：
        - "r"表示以只读模式打开文件，只能进行读取操作。
        - "w"表示以写入模式打开文件，如果文件已存在，则会清空文件内容；如果文件不存在，则会创建新文件。
        - "rw"表示以读写模式打开文件，既可以进行读取操作，也可以进行写入操作。

        通过创建RandomAccessFile对象，可以实现对指定文件的随机读写操作。
        可以使用read()方法读取文件的内容，使用write()方法写入数据到文件中。
        同时，还可以使用seek()方法设置文件指针的位置，以便在文件中进行随机访问。
         */
        RandomAccessFile accessFile = new RandomAccessFile(httpFileName, "rw");

        try{
            byte[] buffer = new byte[1024 * 1024];
            int len = -1;
            //循环读取数据
            while ((len = bis.read(buffer)) != -1) {
                //1秒内下载数据之和, 通过原子类进行操作
                DownloadInfoThread.downSize.add(len);
                accessFile.write(buffer,0,len);
            }
        } catch (FileNotFoundException e) {
            LogUtils.error("下载文件不存在{}", url);
            return false;
        } catch (Exception e) {
            LogUtils.error("下载出现异常");
            return false;
        } finally {

            //改线程执行完毕,将计数器减1（记录正在执行中的 DownLoaderTask Thread 数量）
            countDownLatch.countDown();

            if(httpURLConnection != null)
                httpURLConnection.disconnect();
            if(bis != null)
                bis.close();
            if(accessFile != null)
                accessFile.close();
        }

        return true;
    }


}
