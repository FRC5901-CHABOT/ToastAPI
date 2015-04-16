package jaci.openrio.toast.core.loader.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import jaci.openrio.toast.core.ToastBootstrap;
import jaci.openrio.toast.lib.log.Logger;
import jaci.openrio.toast.lib.module.GroovyScript;
import jaci.openrio.toast.lib.state.RobotState;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * A Loader for loading classes beneath the toast/groovy/ tree. This allows for
 * Groovy Scripts to be executed as if they were modules
 *
 * @author Jaci
 */
public class GroovyLoader {

    static String searchDir = "groovy";
    static Logger logger = new Logger("Toast|GroovyLoader", Logger.ATTR_DEFAULT);
    static ClassLoader loader = ClassLoader.getSystemClassLoader();
    static GroovyClassLoader gLoader = new GroovyClassLoader(loader);
    public static Pattern groovyFile = Pattern.compile("([^\\s$]+).groovy$");

    public static ArrayList<GroovyScript> scripts = new ArrayList<>();
    public static HashMap<File, GroovyObject> groovyFiles = new HashMap<>();
    public static HashMap<String, GroovyObject> groovyObjects = new HashMap<>();

    public static ArrayList<File> customFiles = new ArrayList<>();
    public static ArrayList<String> customClasses = new ArrayList<>();

    public static void init() {
        loadScripts();
    }

    public static GroovyClassLoader getGLoader() {
        return gLoader;
    }

    static void loadScripts() {
        try {
            File search = new File(ToastBootstrap.toastHome, searchDir);
            search.mkdirs();
            search(search);

            for (File custom : customFiles)
                if (custom.isDirectory()) {
                    search(custom);
                } else loadFile(custom);

            for (String name : customClasses)
                loadClassName(name);
        } catch (Exception e) {
            logger.error("Could not load Groovy Scripts: ");
            logger.exception(e);
        }
    }

    public static void search(File file) {
        File[] groovy = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".groovy");
            }
        });
        loadFiles(groovy);

        File[] subdirectory = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });
        for (File f : subdirectory)
            search(f);
    }

    public static void loadFiles(File[] files) {
        if (files != null)
            for (File file : files) {
                try {
                    GroovyObject object = loadFile(file);
                } catch (Exception e) {
                    logger.error("Could not load Groovy Script: " + file.getName());
                    logger.exception(e);
                }
            }
    }

    public static GroovyObject loadClassName(String name) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class groovyClass = loader.loadClass(name);
        GroovyObject object = (GroovyObject) groovyClass.newInstance();

        if (object instanceof GroovyScript) {
            GroovyScript script = (GroovyScript) object;
            script.loadScript();
            scripts.add(script);
        }
        groovyObjects.put(object.getClass().getName(), object);

        try {
            object.invokeMethod("init", null);
        } catch (Exception e) {}
        return object;
    }

    public static GroovyObject loadFile(File file) throws IOException, IllegalAccessException, InstantiationException {
        Class groovyClass = gLoader.parseClass(file);
        GroovyObject object = (GroovyObject) groovyClass.newInstance();

        if (object instanceof GroovyScript) {
            GroovyScript script = (GroovyScript) object;
            script.loadScript();
            scripts.add(script);
        }
        groovyFiles.put(file, object);
        groovyObjects.put(object.getClass().getName(), object);

        try {
            object.invokeMethod("init", new Object[0]);
        } catch (Exception e) {}
        return object;
    }

    public static void prestart() {
        for (GroovyObject object : groovyObjects.values()) {
            try {
                object.invokeMethod("prestartRobot", new Object[0]);
            } catch (Exception e) {}
        }
    }

    public static void start() {
        for (GroovyObject object : groovyObjects.values()) {
            try {
                object.invokeMethod("startRobot", new Object[0]);
            } catch (Exception e) {}
        }
    }

    public static void tick(RobotState state) {
        for (GroovyObject object : groovyObjects.values()) {
            try {
                object.invokeMethod("tickState", state);
            } catch (Exception e) {}
        }
    }

    public static void transition(RobotState state) {
        for (GroovyObject object : groovyObjects.values()) {
            try {
                object.invokeMethod("transitionState", state);
            } catch (Exception e) {}
        }
    }

    public static GroovyObject getObject(String name) {
        return groovyObjects.get(name);
    }

}
