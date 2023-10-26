package com.wsd.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @program: downloadFile
 * @description:
 * @author: Mr.Wang
 * @create: 2023-10-23 20:46
 **/
public class HttpUtil {

    /**
     *  根据url获取一个对应的用于分块下载的连接
     * @param url         下载地址
     * @param startPos    下载文件起始位置
     * @param endPos      下载文件的结束位置
     * @return
     */
    public static HttpURLConnection getHttpURLConnection(String url, long startPos, long endPos) throws IOException {
        URL httpURL =  new URL(url);
        //建立一个与该URL相关的HTTP连接
        HttpURLConnection connection = (HttpURLConnection)httpURL.openConnection();

        //设置HTTP请求的头部属性（HTTP request headers）
        //connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.1234.567 Safari/537.36 Edg/99.0.1234.567");

        //endPos = 0 代表下载的是最后一部分
        if (endPos != 0) {
            /*
              在HTTP协议中，Range头字段用于指定客户端希望获取的资源的范围。
              通过设置Range头字段，可以实现文件的分块下载或断点续传功能。
              具体来说，"bytes="+startPos + "-" + endPos表示客户端希望获取资源的字节范围。其中，startPos表示起始字节位置，endPos表示结束字节位置。
              通过指定起始和结束位置，可以实现只下载文件的一部分内容。
              例如，如果设置"bytes=0-999"，表示客户端希望获取文件的前1000个字节；如果设置"bytes=1000-"，表示客户端希望获取文件从第1001个字节开始的所有内容。
             */
            connection.setRequestProperty("RANGE","bytes="+startPos + "-" + endPos);
        }else {
            connection.setRequestProperty("RANGE","bytes="+startPos + "-");
        }

        LogUtils.info("下载的区间是：{}-{}",startPos,endPos);
        return connection;
    }

    /**
     *  获取HttpURLConnection链接对象
     * @param url  文件的地址
     * @return
     */
    public static HttpURLConnection getHttpURLConnection(String url) throws IOException {
        URL httpUrl = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection)httpUrl.openConnection();
        return httpURLConnection;
    }

    //获取文件名
    public static String getFileName(String url){
        int index = url.lastIndexOf("/");
        return url.substring(index +  1 );
    }

    /**
     *  获取下载文件大小
     * @param url
     * @return
     * @throws IOException
     */
    public static long getHttpFileContentLength(String url) throws IOException {
        int contentLength;
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = getHttpURLConnection(url);
            //when getContentLength() is executed,
            // it will send a request to the server to retrieve the content length of the resource specified by the URL.
            // This method sends a HEAD request to the server, which retrieves the headers of the HTTP response,
            // including the content length.
            // It does not download the entire resource, only the headers.
            //获取要下载的文件的大小
            contentLength = httpURLConnection.getContentLength();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return contentLength;
    }
}
