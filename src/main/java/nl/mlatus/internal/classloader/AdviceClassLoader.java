package nl.mlatus.internal.classloader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class AdviceClassLoader extends URLClassLoader {
    private RedirectClassLoader defaultLoader;
    private String[] jarPath;

    public AdviceClassLoader(URL[] urls, RedirectClassLoader defaultLoader, ClassLoader parent) {
        super(urls, parent);
        this.defaultLoader = defaultLoader;
        defaultLoader.addAdviceLoader(this);
    }

    public AdviceClassLoader(URL[] urls, ClassLoader parent){
        super(urls, parent);
        this.defaultLoader = null;
    }

    public AdviceClassLoader(String[] jarPath, ClassLoader parent) throws MalformedURLException {
        super(getURLs(jarPath), parent);
        this.jarPath = jarPath;
    }

    private static URL[] getURLs(String[] jarPath) throws MalformedURLException {
        URL[] urls = new URL[jarPath.length];
        for(int i = 0; i < jarPath.length; i++){
            urls[i] = new URL("jar:file:" + jarPath[i] + "!/");
        }
        return urls;
    }

    public void setDefaultLoader(RedirectClassLoader loader){
        this.defaultLoader = loader;
        loader.addAdviceLoader(this);
    }

    public String[] getJarPath() {
        return jarPath;
    }

    public Class<?> tryLoadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = null;

        if(defaultLoader != null){
            try {
                c = defaultLoader.tryLoadClass(name, resolve);
            } catch (ClassNotFoundException ignored){}
        }

        if(c == null){
            return super.loadClass(name, resolve);
        } else {
            return c;
        }
    }
}
