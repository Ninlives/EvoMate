package nl.mlatus.internal.classloader;

import nl.mlatus.api.HookLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class RedirectClassLoader extends ClassLoader {
    private ClassLoader child;
    private Set<AdviceClassLoader> adviceLoaders;
    private List<URL> importedURL;

    public RedirectClassLoader(ClassLoader parent, ClassLoader child) {
        super(parent);
        this.child = child;
        this.adviceLoaders = new CopyOnWriteArraySet<>();
        this.importedURL = new ArrayList<>();
    }

    public void addAdviceLoader(AdviceClassLoader loader){
        if(loader != null){
            adviceLoaders.add(loader);
            importedURL.addAll(Arrays.asList(loader.getURLs()));
        }
    }

    public List<URL> getImportedURL() {
        return importedURL;
    }

    public Class<?> tryLoadClass(String name, boolean resolve) throws ClassNotFoundException {
        HookLogger.info("Try Loading class " + name);
        synchronized (getClassLoadingLock(name)){
            Class<?> c = null;
            if(child != null){
                try {
                    Method findLoadedClass = child.getClass().getDeclaredMethod("findLoadedClass", String.class);
                    findLoadedClass.setAccessible(true);
                    c = (Class<?>) findLoadedClass.invoke(child, name);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {

                }
            }

            if(c == null) {
                c = super.loadClass(name, resolve);
            }

            if (c == null) {
                throw new ClassNotFoundException(name);
            }

            if(resolve){
                resolveClass(c);
            }

            return c;
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        HookLogger.info("Loading class " + name);
        System.out.println("Loading class " + name);
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = null;

            try {
                c = super.loadClass(name, resolve);
            } catch (ClassNotFoundException ignored){

            }

            if (c == null) {
                for(AdviceClassLoader adviceLoader : adviceLoaders){
                    try {
                        c = adviceLoader.tryLoadClass(name, resolve);
                        if (c != null) break;
                    } catch (ClassNotFoundException ignored){

                    }
                }
            }

            if (c == null) {
                throw new ClassNotFoundException(name);
            }

            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
