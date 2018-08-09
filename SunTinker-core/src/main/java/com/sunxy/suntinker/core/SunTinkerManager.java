package com.sunxy.suntinker.core;


import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.res.TypedArrayUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * --
 * <p>
 * Created by sunxy on 2018/8/9 0009.
 */
public class SunTinkerManager {

    private volatile static SunTinkerManager manager;

    private SunTinkerManager(){}

    public static SunTinkerManager getManager(){
        if(manager == null){
            synchronized(SunTinkerManager.class){
                if (manager == null){
                    manager = new SunTinkerManager();
                }
            }
        }
        return manager;
    }

    /**
     * 修复方法，
     * @param application application
     * @param dexPath     存放补丁dex的路径
     * @return 是否修复成功
     */
    public boolean fix(Application application, String dexPath){
        try {
            //application中的getClassLoader是pathClassLoader
            //而 pathClassLoader 继承 BaseDexClassLoader
            ClassLoader classLoader = application.getClassLoader();
            // 获取  BaseDexClassLoader 的 private final DexPathList pathList;
            Field pathListField = ReflexUtils.findField(classLoader, "pathList");
            Object pathList = pathListField.get(classLoader);
//            获取 DexPathList中 private Element[] dexElements;
            Field dexElementsField = ReflexUtils.findField(pathList, "dexElements");
            Object[] dexElements = (Object[]) dexElementsField.get(pathList);

            //将dexPath中的dex文件加载成element
            Object[] addElements = dexFile2Elements(dexPath, pathList);

            if (addElements == null){
                return false;
            }
            //创建新的 Element[]
            Object[] newElements = (Object[]) Array.newInstance(dexElements.getClass().getComponentType(),
                    dexElements.length + addElements.length);
           System.arraycopy(addElements, 0, newElements, 0, addElements.length);
           System.arraycopy(dexElements, 0, newElements, addElements.length, dexElements.length);

            //替换DexPathList中的dexElements
            dexElementsField.set(pathList, newElements);

            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * dexPath转element数组
     * @param dexPath
     * @param pathList
     */
    private Object[] dexFile2Elements(String dexPath,  Object pathList) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        File dexFile = new File(dexPath);
        File[] listFiles = dexFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".dex");
            }
        });
        if (listFiles == null || listFiles.length == 0){
            return null;
        }
        List<File> fileList = Arrays.asList(listFiles);

        //获取makeDexElement方法
        Method makeDexElement;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            //5.0 - 6.0
            makeDexElement = ReflexUtils.findMethod(pathList, "makeDexElements",
                    ArrayList.class, File.class, ArrayList.class);
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //大于 6.0
            makeDexElement = ReflexUtils.findMethod(pathList, "makePathElements",
                    List.class, File.class, List.class);
        }else{
            return null;
        }
        ArrayList<IOException> suppressedExceptions = new ArrayList<>();
        return (Object[]) makeDexElement
                .invoke(pathList, fileList, null, suppressedExceptions);
    }


}
