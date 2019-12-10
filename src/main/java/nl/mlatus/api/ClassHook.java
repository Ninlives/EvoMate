package nl.mlatus.api;

import nl.mlatus.api.annotations.AfterReturn;
import nl.mlatus.api.annotations.AfterThrow;
import nl.mlatus.api.annotations.BeforeCall;
import nl.mlatus.internal.visitor.AdviceWeaver;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import static nl.mlatus.api.HookLogger.failedToHook;
import static nl.mlatus.api.HookLogger.success;
import static nl.mlatus.internal.Constant.PRIMITIVE_TYPE_DESCRIPTORS;

public abstract class ClassHook {

    public void handleLoadClass(LoadClassParam param) {}

    protected final void hookMethod(LoadClassParam param,
                                    Class hookClass,
                                    String methodName,
                                    Object... paramTypesClassOrString){
        if(!Modifier.isPublic(hookClass.getModifiers())){
            failedToHook(methodName, param.getClassName(), this.getClass(), "Hook Class should be public.");
            param.setHooked(false);
            return;
        }
        try {
            Type[] paramTypes  = getParamTypes(paramTypesClassOrString);
            hookMethod(hookClass, param, methodName, paramTypes);
        } catch (IllegalArgumentException e){
            failedToHook(methodName, param.getClassName(), this.getClass(), "Illegal Arguments.");
        }
    }

    private Type[] getParamTypes(Object[] paramTypesClassOrString) {
        Type[] types = new Type[paramTypesClassOrString.length];

        for(int i = 0; i < paramTypesClassOrString.length; i++){
            if(paramTypesClassOrString[i] instanceof Class){
                types[i] = Type.getType((Class)paramTypesClassOrString[i]);
            } else if (paramTypesClassOrString[i] instanceof String){
                types[i] = Type.getType(getDescriptor((String) paramTypesClassOrString[i], false));
            } else {
                throw new IllegalArgumentException();
            }
        }

        return types;
    }

    private static String getDescriptor(final String type, final boolean defaultPackage) {
        if ("".equals(type)) {
            return type;
        }

        StringBuilder stringBuilder = new StringBuilder();
        int arrayBracketsIndex = 0;
        while ((arrayBracketsIndex = type.indexOf("[]", arrayBracketsIndex) + 1) > 0) {
            stringBuilder.append('[');
        }

        String elementType = type.substring(0, type.length() - stringBuilder.length() * 2);
        String descriptor = PRIMITIVE_TYPE_DESCRIPTORS.get(elementType);
        if (descriptor != null) {
            stringBuilder.append(descriptor);
        } else {
            stringBuilder.append('L');
            if (elementType.indexOf('.') < 0) {
                if (!defaultPackage) {
                    stringBuilder.append("java/lang/");
                }
                stringBuilder.append(elementType);
            } else {
                stringBuilder.append(elementType.replace('.', '/'));
            }
            stringBuilder.append(';');
        }
        return stringBuilder.toString();
    }

    private void hookMethod(Class hookClass,
                            LoadClassParam param,
                            String methodName,
                            Type... paramTypes){

        Method beforeCall = null;
        Method afterReturn = null;
        Method afterThrow = null;

        try {
            for (Method method : hookClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(BeforeCall.class)) {
                    checkMultipleAssign(beforeCall, "BeforeCall", methodName, param);
                    beforeCall = returnValidHookMethod(method, methodName, param);
                }

                if (method.isAnnotationPresent(AfterReturn.class)) {
                    checkMultipleAssign(afterReturn, "AfterReturn", methodName, param);
                    afterReturn = returnValidHookMethod(method, methodName, param);
                }

                if (method.isAnnotationPresent(AfterThrow.class)) {
                    checkMultipleAssign(afterThrow, "AfterThrow", methodName, param);
                    afterThrow = returnValidHookMethod(method, methodName, param);
                }
            }
        } catch (HookFailedError e){
            param.setHooked(false);
            return;
        }

        if(null == beforeCall && null == afterReturn && null == afterThrow){
            failedToHook(methodName, param.getClassName(), this.getClass(),
                         "No hook method found.");
            param.setHooked(false);
            return;
        }

        ClassReader cr = new ClassReader(param.getClassFileBuffer());
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        String replaceName = methodName + "_" + hookClass.hashCode();
        Set<String> names = cn.methods.stream().map(mn -> mn.name).collect(Collectors.toSet());
        while(names.contains(replaceName)){
            replaceName = replaceName + "_";
        }

        final ClassLoader loader = param.getClassLoader();
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES){
            @Override
            protected String getCommonSuperClass(final String type1, final String type2) {
                Class<?> class1;
                try {
                    class1 = Class.forName(type1.replace('/', '.'), false, loader);
                } catch (Exception e) {
                    throw new TypeNotPresentException(type1, e);
                }
                Class<?> class2;
                try {
                    class2 = Class.forName(type2.replace('/', '.'), false, loader);
                } catch (Exception e) {
                    throw new TypeNotPresentException(type2, e);
                }
                if (class1.isAssignableFrom(class2)) {
                    return type1;
                }
                if (class2.isAssignableFrom(class1)) {
                    return type2;
                }
                if (class1.isInterface() || class2.isInterface()) {
                    return "java/lang/Object";
                } else {
                    do {
                        class1 = class1.getSuperclass();
                    } while (!class1.isAssignableFrom(class2));
                    return class1.getName().replace('.', '/');
                }
            }
        };

        AdviceWeaver weaver = new AdviceWeaver(cw, param, methodName, paramTypes, replaceName, beforeCall, afterReturn, afterThrow);
        cr.accept(weaver, 0);

        if(weaver.isSuccess()){
            param.setClassFileBuffer(cw.toByteArray());
            param.setHooked(true);
            success(methodName, param.getClassName(), this.getClass());
        } else {
            param.setHooked(false);
            failedToHook(methodName, param.getClassName(), this.getClass(),
                    "Unknown reason, please check the name and parameter types of target method.");
        }
    }

    private Method returnValidHookMethod(Method method, String methodName, LoadClassParam param) throws HookFailedError {
        if(checkHookMethod(method)) return method;
        else {
            failedToHook(methodName, param.getClassName(), this.getClass(),
                    "Hook method should be public static, accept and only accept one MethodHookParam as parameter.");
            throw new HookFailedError();
        }
    }

    private boolean checkHookMethod(Method hookMethod){
        int modifier = hookMethod.getModifiers();
        if((!Modifier.isPublic(modifier)) || (!Modifier.isStatic(modifier))){
            return false;
        }
        Class[] paramTypes = hookMethod.getParameterTypes();
        if(paramTypes.length != 1){
            return false;
        }
        if(!MethodHookParam.class.isAssignableFrom(paramTypes[0])){
            return false;
        }
        return true;
    }

    private void checkMultipleAssign(Method m, String annoName, String methodName, LoadClassParam param) throws HookFailedError {
        if(null != m) {
            failedToHook(methodName, param.getClassName(), this.getClass(),
                    "Multiple method annotated with @" + annoName + ".");
            throw new HookFailedError();
        }
    }
    private static class HookFailedError extends Exception {}
}
