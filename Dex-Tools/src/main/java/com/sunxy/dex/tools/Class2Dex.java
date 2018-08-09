package com.sunxy.dex.tools;

import java.io.IOException;

/**
 * --
 * <p>
 * Created by sunxy on 2018/8/9 0009.
 */
public class Class2Dex {

    public static void main(String[] args) throws IOException, InterruptedException {

        String cmd = "cmd /c dx --dex --output %s %s";
        String outDexPath = "Dex-Tools/output/out.dex";
        String classPath = "Dex-Tools/output/class";

        Process process = Runtime.getRuntime().exec(String.format(cmd, outDexPath, classPath));
        process.waitFor();
        //失败
        if (process.exitValue() != 0) {
            throw new RuntimeException("dex error");
        }else{
            System.out.println("dex success~~");
        }

    }
}
