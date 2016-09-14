package edu.ucla.cs.baggins.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import edu.ucla.cs.baggins.data.provider.model.annotations.ClientTable;
import edu.ucla.cs.baggins.data.provider.model.base.BagginsDomainModel;

/**
 * Created by ethan on 12/12/15.
 */
public class StaticUtil {
    public static final String TAG = "static_util";

    // ------------------------------------------------------------------------
    // For getting domain objects from the class
    // ------------------------------------------------------------------------

    /**
     * Looks through all classes in the project.Returns all classes which contain
     * the @ClientTable annotation. These should all be of type BagginsDomainModel<?></?>
     *
     * @param context
     * @return
     */
    public static List<Class<BagginsDomainModel<?>>> getAllModelClasses(Context context) throws IOException {

        List<Class<BagginsDomainModel<?>>> tables = new ArrayList<>();

        String              sourceDirOld = context.getApplicationInfo().sourceDir;   // Search for all classes
        String              sourceDir    = context.getPackageCodePath();
        DexFile             dexFile      = new DexFile(sourceDir);   // Get the DexFile from the sourdir
        Enumeration<String> classes      = dexFile.entries();        // Iterate through all classes in our project

        while (classes.hasMoreElements()) {
            String className = classes.nextElement();                       // Get the class name

            try {
                // Ignore google files
                if (!className.startsWith("com.google")) {
                    Class modelClass = Class.forName(className);                // Instantiate class
                    if (modelClass.isAnnotationPresent(ClientTable.class)) {    // If it is annotated as a ClientTable
                        tables.add(modelClass);
                    }
                }
            } catch (ClassNotFoundException e) {
                Log.i(TAG, "Error getting class: " + e);
            } catch (NoClassDefFoundError ncdfe) {
                Log.i(TAG,"Ignore Error: " + ncdfe);
                // Ignore this as it simply means this otherClass was not actually a class.
                // The dex files returns more than just classes.
            } catch (Throwable t) {
                Log.i(TAG,"Ignoring error: " + t);
            }
        }

        if (tables.isEmpty()) {

            throw new RuntimeException("No tables found, you must declare at least one by annotating " +
                                       "a domain model with ClientTable.class. Are you using Android Studio instant " +
                                       "run? This will not work if that is on. See: " +
                                       "http://stackoverflow.com/questions/36572515/dexfile-in-2-0-versions-of-android-studio-and-gradle");
        }

        return tables;
    }
}