package edu.ucla.cs.baggins.data.provider.util;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

public abstract class ClassScanner {

    private static final String TAG = "ClassScanner";
    private Context mContext;

    public ClassScanner(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    private List<String> getClassesOfPackage(String packageName) {
        ArrayList<String> classes = new ArrayList<String>();
        try {
            String path = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), 0).sourceDir;

            String packageCodePath = mContext.getPackageCodePath();
            Log.i(TAG, "Path1: " + path);
            Log.i(TAG, "Path2: " + packageCodePath);
            DexFile df = new DexFile(path);
            for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {
                String className = iter.nextElement();
                Log.i(TAG, "CLassname: " + className);
                if (className.contains(packageName)) {
                    classes.add(className.substring(className.lastIndexOf(".") + 1, className.length()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e);
        }

        return classes;
    }

    public void scan() throws IOException, ClassNotFoundException, NoSuchMethodException {
        long timeBegin = System.currentTimeMillis();

        Log.i(TAG, "Get classes");
        for (String c : getClassesOfPackage("")) {
            Log.i(TAG, "Class: " + c);

        }
        Log.i(TAG, "Got classes");

        PathClassLoader     classLoader = (PathClassLoader) getContext().getClassLoader();
        DexFile             dexFile     = new DexFile(getContext().getPackageCodePath());
        Enumeration<String> classNames  = dexFile.entries();

        while (classNames.hasMoreElements()) {
            String className = classNames.nextElement();
            if (isTargetClassName(className)) {
                Class<?> aClass = classLoader.loadClass(className);//tested on 魅蓝Note(M463C)_Android4.4.4 and Mi2s_Android5.1.1
                if (isTargetClass(aClass)) {
                    onScanResult(aClass);
                }
            }
        }

        long timeEnd     = System.currentTimeMillis();
        long timeElapsed = timeEnd - timeBegin;
    }

    protected abstract boolean isTargetClassName(String className);

    protected abstract boolean isTargetClass(Class clazz);

    protected abstract void onScanResult(Class clazz);
}