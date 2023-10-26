package com.wsd.core;

import com.wsd.util.FileUtils;
import com.wsd.util.HttpUtil;
import com.wsd.util.LogUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * @program: downloadFile
 * @description:
 * @author: Mr.Wang
 * @create: 2023-10-23 23:57
 **/
public class Downloader {

    //线程池中核心线程的数量
    public int THREAD_NUM = 5;

    //create a ScheduledThreadPool with a fixed number of threads.
    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    //线程池对象
    public ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(THREAD_NUM, THREAD_NUM, 0, TimeUnit.SECONDS, new ArrayBlockingQueue(THREAD_NUM));

    /*
    在Java中，CountDownLatch是一个同步辅助类，用于控制多个线程之间的同步。它的作用是允许一个或多个线程等待其他线程完成某个操作后再继续执行。

CountDownLatch通过一个计数器来实现，该计数器初始化为一个正整数，表示需要等待的线程数量。每当一个线程完成了预期的操作，就会调用countDown()方法将计数器减1。当计数器的值变为0时，所有等待的线程将被释放，可以继续执行。

CountDownLatch的主要方法包括：

- void countDown()：将计数器减1。
- void await()：等待计数器变为0，阻塞当前线程。

CountDownLatch常用于以下场景：

1. 主线程等待多个子线程完成任务后再继续执行。
2. 多个子线程等待某个共享资源就绪后再同时开始执行。
3. 控制多个线程按照预定的顺序执行。

通过使用CountDownLatch，可以实现线程之间的协调和同步，确保线程在合适的时机进行操作，避免出现竞态条件和数据不一致的问题。
     */
    private CountDownLatch countDownLatch = new CountDownLatch(THREAD_NUM);

    public void download(String url)  {
        //获取连接
        //HttpURLConnection connection = null;
        try{

            //获取文件名
            String fileName = HttpUtil.getFileName(url);
            //从path.txt中获取下载路径
            String downdoloadPath = getDownloadPath();
            //拼接文件路径
            String path = downdoloadPath +"/"+ fileName;



            //when getContentLength() is executed,
            // it will send a request to the server to retrieve the content length of the resource specified by the URL.
            // This method sends a HEAD request to the server, which retrieves the headers of the HTTP response,
            // including the content length.
            // It does not download the entire resource, only the headers.
            //获取要下载的文件的大小
            //int contentLength = connection.getContentLength();

            // //获取本地文件的大小
            long localFileLength = FileUtils.getFileContentLength(path);

            long contentLength = HttpUtil.getHttpFileContentLength(url);
            //判断文件是否已下载过
            if (localFileLength >= contentLength) {
                LogUtils.info("{}已存在，无需重新下载",path);
                return;
            }

            //切分任务
            ArrayList<Future> list = new ArrayList();
            spilt(url, contentLength, path, list);

            //创建另一个线程,打印下载进度信息
            DownloadInfoThread downloadInfoThread = new DownloadInfoThread(contentLength);

            //将任务交给线程池去安排一个线程执行 打印下载进度信息 的任务，该任务每隔1秒执行一次
            //(downloadInfoThread,1,1, TimeUnit.SECONDS)
            //parameter means:
            //(It can be a Runnable or Callable object that represents the code you want to run.,
            // which means the task will start executing after 1 time unit has passed.,
            // which means the task will be executed every 1 time unit.,
            // which means the time is measured in seconds(时间单位))
            scheduledExecutorService.scheduleAtFixedRate(downloadInfoThread,1,1, TimeUnit.SECONDS);

            //double downSize = downloadInfoThread.getDownSize().doubleValue();
            //获取连接
            //connection = HttpUtil.getHttpURLConnection(url);
            //发送url请求,获取响应的输入流
            //BufferedInputStream in = new BufferedInputStream( connection.getInputStream() ) ;
            //输出流，保存下载的文件
            /*BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream(path) );
            byte[] bytes = new byte[1024*100];
            int len = 0;*/
            //将下载的内容保存到指定的位置
           /* while((len = in.read(bytes) ) != -1){
                out.write(bytes);
                downloadInfoThread.setDownSize(downSize+=len);
            }
            out.flush();*/



            //等待计数器变为0(5个分块下载文件的线程都下载完成)，阻塞当前线程
            countDownLatch.await();

            //合并文件
            if (merge(path)) {
                //清除临时文件
                clearTemp(path);

                System.out.print("\r");
                System.out.println("下载完成");
            }



        }catch (Exception e){
            System.out.println("Download fail");
        }
        finally {
            /*if(connection != null) {
                connection.disconnect();
            }*/
            //关闭 scheduled thread pool
            scheduledExecutorService.shutdown();

            //关闭 thread pool
            poolExecutor.shutdown();


        }


    }

    public String getDownloadPath() throws IOException{
        //从path.txt中获取下载路径
        File downloadPathFile = new File("path.txt");
        //System.out.println(downloadPathFile.getAbsolutePath());
        FileReader fileReaderforDwnloadPathFile = new FileReader(downloadPathFile);
        BufferedReader inPath = new BufferedReader(fileReaderforDwnloadPathFile);
        String downdoloadPath = inPath.readLine();
        return downdoloadPath;
    }


    /**
     *  文件切分
     * @param url
     * @param contentLength
     * @param futureList
     */
    public void spilt(String url,long contentLength,String path, ArrayList<Future> futureList) {

        try {
            //获取下载文件大小
            //long contentLength = HttpUtil.getHttpFileContentLength(url);

            //计算切分后的文件大小
            long size = contentLength / THREAD_NUM;

            //计算分块个数
            for (int i = 0; i < THREAD_NUM; i++) {
                //计算下载起始位置
                long startPos = i * size;

                //计算结束位置
                long endPos;
                if (i == THREAD_NUM - 1) {
                    //下载最后一块，下载剩余的部分
                    endPos = 0;
                }else {
                    endPos = startPos + size;
                }

                //如果不是第一块，起始位置要+1
                if (startPos != 0) {
                    startPos++;
                }

                //创建任务对象
                DownloaderTask downloaderTask = new DownloaderTask(url, startPos, endPos,i,path, countDownLatch);

                //将任务提交到线程池中
                Future<Boolean> future = poolExecutor.submit(downloaderTask);

                futureList.add(future);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     *  合并临时文件
     * @param fileName
     * @return
     */
    public boolean merge(String fileName) {

        LogUtils.info("开始合并文件{}", fileName);

        byte[] buffer = new byte[1024 * 1024];
        int len = -1;

        File file = new File(fileName);

        if (!file.exists()) {
            try{
                file.createNewFile();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        RandomAccessFile accessFile = null;
        try  {

            accessFile = new RandomAccessFile(fileName, "rw");

            for (int i = 0; i < THREAD_NUM ; i++) {
                BufferedInputStream bis = new BufferedInputStream( new FileInputStream(fileName + ".temp" + i) );
                 while ( (len = bis.read(buffer) ) != -1) {
                     accessFile.write(buffer,0,len);
                 }
            }

            LogUtils.info("文件合并完毕{}" , fileName);

        } catch (Exception e) {
            LogUtils.info("文件合并失败{}" + fileName);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /*
        清除临时文件
     */
    public boolean clearTemp(String fileName) {

        for (int i = 0; i < THREAD_NUM; i++) {

            File file = new File(fileName + ".temp" + i);
            file.delete();
        }

        return true;
    }
}
