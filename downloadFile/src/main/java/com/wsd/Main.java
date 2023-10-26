package com.wsd;

import com.wsd.core.Downloader;

import java.util.Scanner;

/**
 * @program: downloadFile
 * @description: Main class
 * @author: Mr.Wang
 * @create: 2023-10-23 19:14
 **/
public class Main {

    public static void main(String[] args) {
        //下载地址
        String url = null;
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("Please enter a url for download a file: ");
            url = scanner.next();
            if(url != null){
                break;
            }
        }
        scanner.close();
        //System.out.println(url);
        Downloader downloader = new Downloader();
        downloader.download(url);
    }

}
