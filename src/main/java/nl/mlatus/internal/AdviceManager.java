package nl.mlatus.internal;

import nl.mlatus.api.ClassHook;
import nl.mlatus.api.MethodHookParam;
import nl.mlatus.internal.classloader.AdviceClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public class AdviceManager {
    private static Logger logger = Logger.getLogger(AdviceManager.class.getName());

    private static List<ClassHook> hookList;

    public static void init(Map<String, String> option, Instrumentation inst){
        hookList = new ArrayList<>();

        String moduleDirPath = "./module";
        if(option.containsKey("moduleDir")){
            moduleDirPath = option.get("moduleDir");
        }

        File moduleDir = new File(moduleDirPath);

        if(moduleDir.exists() && moduleDir.isDirectory()){
            for(File file : Objects.requireNonNull(moduleDir.listFiles())){
                if(file.getName().endsWith(".jar")){
                    loadModulesFromJar(file);
                }
            }

        } else {
            logger.warning("Loading modules failed: " + moduleDirPath + " does not exist or is not a directory.");
        }
    }

    public static List<ClassHook> getAdvice(){
        return hookList;
    }
    public static Logger getLogger(){
        return logger;
    }

    private static void loadModulesFromJar(File file) {
        String jarPath = file.getAbsolutePath();
        logger.info("Loading modules from: " + jarPath + "...");

        try {
            JarInputStream jar = new JarInputStream(new FileInputStream(file));
            Manifest manifest = jar.getManifest();
            Attributes attributes = manifest.getMainAttributes();

            String adviceModule = attributes.getValue("Advice-Module");
            if(null != adviceModule){
                String[] moduleClasses = adviceModule.split(" ");
//                URL[] urls = { new URL("jar:file:" + jarPath + "!/") };
//                URLClassLoader cl = new URLClassLoader(urls, MethodHookParam.class.getClassLoader());
                String[] path = { jarPath };
                AdviceClassLoader cl = new AdviceClassLoader(path, MethodHookParam.class.getClassLoader());

                for(String moduleClass : moduleClasses){
                    logger.info("Loading module " + moduleClass + " from " + jarPath + "...");

                    try {
                        Class module = cl.loadClass(moduleClass);
                        if(!ClassHook.class.isAssignableFrom(module)){
                            logger.warning("Module " + moduleClass + " does not extends ClassHook, skip it.");
                            continue;
                        }

                        try {
                            ClassHook advice = (ClassHook) module.getDeclaredConstructor().newInstance();
                            hookList.add(advice);
                        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                            logger.warning("Failed to create new instance for " + moduleClass + ", module not loaded.");
                        }

                    } catch (ClassNotFoundException e) {
                        logger.warning("Failed to load module " + moduleClass + ": Class not found. skip it.");
                    }
                }

            } else {
                logger.warning(file.getAbsolutePath() + " does not have the entry \"Advice-Module\" in the MANIFEST.MF, skip it.");
            }
        } catch (IOException e) {
            logger.warning("Loading modules from " + file.getAbsolutePath() + " failed, skip it: " + e.getCause());
        }
    }
}
