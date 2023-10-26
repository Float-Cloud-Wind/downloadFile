package com.wsd.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 *  日志工具类
 */
public class LogUtils {

    public static void info(String msg, Object... args) {
        print(msg,"-info-",args);
    }

    public static void error(String msg, Object... args) {
        print(msg,"-error-",args);
    }

    private static void print(String msg, String level, Object... args) {
        if (args != null && args.length > 0) {
            msg = String.format(msg.replace("{}", "%s"), args);
        }
        String name = Thread.currentThread().getName();
        String now = LocalTime.now().format( DateTimeFormatter.ofPattern("hh:mm:ss") );
        System.out.println(now + " " + name + level + msg);

    }
}
