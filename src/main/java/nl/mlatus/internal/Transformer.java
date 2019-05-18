package nl.mlatus.internal;

import jdk.internal.loader.BuiltinClassLoader;
import nl.mlatus.api.ClassHook;
import nl.mlatus.api.HookLogger;
import nl.mlatus.api.LoadClassParam;
import nl.mlatus.internal.classloader.AdviceClassLoader;
import nl.mlatus.internal.classloader.RedirectClassLoader;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.jar.JarFile;

public class Transformer implements ClassFileTransformer {
    private Instrumentation inst;
    public Transformer(Instrumentation inst) {
        this.inst = inst;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        LoadClassParam param = new LoadClassParam(loader, className.replace('/','.'), Arrays.copyOf(classfileBuffer, classfileBuffer.length));
        try {
            for (ClassHook advice : AdviceManager.getAdvice()) {
                advice.handleLoadClass(param);
                if (param.isHooked()) {
                    for (String path : ((AdviceClassLoader)advice.getClass().getClassLoader()).getJarPath()){
                        try {
                            inst.appendToBootstrapClassLoaderSearch(new JarFile(path));
                        } catch (IOException e) {
                            HookLogger.waring(String.format("Failed to hook class %s using module %s, class remain unchanged: Module not loaded by URLClassLoader.",
                                    param.getClassName(), advice.getClass().getName()));
                            e.printStackTrace();
                            return classfileBuffer;
                        }
                    }
                }
                param.setHooked(false);
            }
        } catch (Throwable e){
            e.printStackTrace();
        }

        return param.getClassFileBuffer();
    }

    private boolean injectClassLoader(ClassHook advice, LoadClassParam param){
        ClassLoader loaderForAdvice = advice.getClass().getClassLoader();
        if (loaderForAdvice instanceof AdviceClassLoader) {
            AdviceClassLoader currentClassLoader = (AdviceClassLoader) loaderForAdvice;
            ClassLoader child = param.getClassLoader();

            if(child.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders")){
                String[] paths = currentClassLoader.getJarPath();
                for (String path : paths){
                    try {
                        inst.appendToSystemClassLoaderSearch(new JarFile(path));
                    } catch (IOException e) {
                        HookLogger.waring(String.format("Failed to hook class %s using module %s, class remain unchanged: Module not loaded by URLClassLoader.",
                                param.getClassName(), advice.getClass().getName()));
                        e.printStackTrace();
                        return false;
                    }
                }
                return true;
            }

            URL[] urls = currentClassLoader.getURLs();
            ClassLoader originalParent = param.getClassLoader().getParent();
            AdviceClassLoader adviceClassLoader = new AdviceClassLoader(urls, this.getClass().getClassLoader());

            if (!(originalParent instanceof RedirectClassLoader)) {
                RedirectClassLoader redirectClassLoader = new RedirectClassLoader(originalParent, child);
                try {
                    Field parentField = ClassLoader.class.getDeclaredField("parent");
                    parentField.setAccessible(true);
                    parentField.set(child, redirectClassLoader);
                    adviceClassLoader.setDefaultLoader(redirectClassLoader);
                } catch (NoSuchFieldException e) {
                    HookLogger.waring(String.format("Failed to hook class %s using module %s, class remain unchanged: Can't replace ClassLoader parent. ",
                            param.getClassName(), advice.getClass().getName()));
                    resetClassLoader(param.getClassLoader());
                    return false;
                } catch (IllegalAccessException e) {
                    HookLogger.waring("Unknown error.");
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                if(!((RedirectClassLoader)originalParent).getImportedURL().containsAll(Arrays.asList(urls))) {
                    adviceClassLoader.setDefaultLoader((RedirectClassLoader) originalParent);
                }
            }
            return true;
        } else {
            HookLogger.waring(String.format("Failed to hook class %s using module %s, class remain unchanged: Module not loaded by URLClassLoader.",
                    param.getClassName(), advice.getClass().getName()));
            resetClassLoader(param.getClassLoader());
            return false;
        }
    }

    private void resetClassLoader(ClassLoader loader){
        ClassLoader parent = loader.getParent();
        if(parent instanceof RedirectClassLoader){
            ClassLoader originalParent = parent.getParent();
            try {
                Field parentField = ClassLoader.class.getDeclaredField("parent");
                parentField.setAccessible(true);
                parentField.set(loader, originalParent);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                HookLogger.waring("Unknown error.");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
