package nl.mlatus.api;

public class LoadClassParam {
    private String className;
    private ClassLoader classLoader;
    private byte[] classFileBuffer;
    private boolean hooked;

    public LoadClassParam(ClassLoader classLoader, String className, byte[] classFileBuffer){
        this.classLoader = classLoader;
        this.className = className;
        this.classFileBuffer = classFileBuffer;
        this.hooked = false;
    }

    public String getClassName() {
        return className;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public byte[] getClassFileBuffer() {
        return classFileBuffer;
    }

    public void setClassFileBuffer(byte[] classFileBuffer) {
        this.classFileBuffer = classFileBuffer;
    }

    public boolean isHooked() {
        return hooked;
    }

    public void setHooked(boolean hooked) {
        this.hooked = hooked;
    }
}
