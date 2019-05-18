package nl.mlatus.api;

public class MethodHookParam {
    public Object thisObject;
    public Object[] args;
    private boolean returnEarly;
    private boolean throwEarly;
    private Object result;
    private Throwable throwable;

    public MethodHookParam(Object thisObject, Object[] args){
        this.thisObject = thisObject;
        this.args = args;
        this.returnEarly = false;
        this.throwEarly = false;
        this.result = null;
        this.throwable = null;
    }

    public MethodHookParam(Object result, Object thisObject, Object[] args){
        this(thisObject, args);
        this.result = result;
    }

    public MethodHookParam(Throwable e, Object thisObject, Object[] args){
        this(thisObject, args);
        this.throwable = e;
    }

    public void returnEarly(){
        this.returnEarly = true;
        this.throwEarly = false;
    }

    public void setResult(Object result) {
        this.result = result;
        this.throwable = null;
        this.returnEarly = true;
        this.throwEarly = false;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
        this.result = null;
        this.throwEarly = true;
        this.returnEarly = false;
    }

    public Object getResult() {
        return result;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public boolean isReturnEarly() {
        return returnEarly;
    }

    public boolean isThrowEarly() {
        return throwEarly;
    }
}
