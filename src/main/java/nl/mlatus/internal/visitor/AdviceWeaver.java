package nl.mlatus.internal.visitor;

import nl.mlatus.api.LoadClassParam;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import static nl.mlatus.internal.Constant.*;

public class AdviceWeaver extends ClassVisitor {
    private int beforeCallLocal;
    private int afterReturnLocal;
    private int afterThrowLocal;

    private boolean success;
    private LoadClassParam loadClassParam;
    private Type targetClassType;
    private String targetMethodName;
    private Type[] targetMethodParamTypes;
    private String replaceName;
    private Method realMethod;
    private java.lang.reflect.Method beforeCall;
    private java.lang.reflect.Method afterReturn;
    private java.lang.reflect.Method afterThrow;

    private int access;
    private String descriptor;
    private String signature;
    private String[] exceptions;

    public AdviceWeaver(ClassVisitor classVisitor,
                        LoadClassParam loadClassParam,
                        String targetMethodName,
                        Type[] targetMethodParamTypes,
                        String replaceName,
                        java.lang.reflect.Method beforeCall,
                        java.lang.reflect.Method afterReturn,
                        java.lang.reflect.Method afterThrow) {
        super(API_VERSION, classVisitor);
        this.success = false;

        this.loadClassParam = loadClassParam;
        this.targetMethodName = targetMethodName;
        this.targetMethodParamTypes = targetMethodParamTypes;
        this.replaceName = replaceName;
        this.beforeCall = beforeCall;
        this.afterReturn = afterReturn;
        this.afterThrow = afterThrow;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("L");
        String name = loadClassParam.getClassName();
        int nameLength = name.length();
        for (int i = 0; i < nameLength; ++i) {
            char car = name.charAt(i);
            stringBuilder.append(car == '.' ? '/' : car);
        }
        stringBuilder.append(';');
        this.targetClassType = Type.getType(stringBuilder.toString());

        this.access = -1;
        this.descriptor = null;
        this.signature = null;
        this.exceptions = null;
        this.realMethod = null;

        this.beforeCallLocal = -1;
        this.afterReturnLocal = -1;
        this.afterThrowLocal = -1;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if(name.equals(targetMethodName)){
            Type[] paramTypes = Type.getArgumentTypes(descriptor);
            if(paramTypes.length != targetMethodParamTypes.length){
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }

            for(int i = 0; i < paramTypes.length; i++){
                if(!paramTypes[i].equals(targetMethodParamTypes[i])){
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }

            this.access = access;
            this.descriptor = descriptor;
            this.signature = signature;
            this.exceptions = exceptions;
            this.realMethod = new Method(replaceName, Type.getReturnType(descriptor), paramTypes);

            int privateAccess = access & (~(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED));
            privateAccess = privateAccess|Opcodes.ACC_PRIVATE;
            return super.visitMethod(privateAccess, replaceName, descriptor, signature, exceptions);
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    @Override
    public void visitEnd() {
        if (access == -1 || descriptor == null || realMethod == null){
            success = false;
        } else {
            try {
                MethodVisitor mv = super.visitMethod(access, targetMethodName, descriptor, signature, exceptions);
                GeneratorAdapter generator = new GeneratorAdapter(mv, access, targetMethodName, descriptor);

                Label tryLabel = new Label();
                Label catchLabel = new Label();
                Label finallyLabel = new Label();


                if (null != beforeCall) {
                    Label checkThrow = new Label();
                    Label invokeOriginal = new Label();
                    beforeCallLocal = generator.newLocal(METHOD_HOOK_PARAM_TYPE);

                    generator.newInstance(METHOD_HOOK_PARAM_TYPE);
                    //Lparam;
                    generator.dup();
                    //Lparam;Lparam;
                    generator.dup();
                    //Lparam;Lparam;Lparam;
                    loadThisOrNull(generator);
                    //Lparam;Lparam;Lparam;thisOrNull;
                    generator.loadArgArray();
                    //Lparam;Lparam;Lparam;thisOrNull;[Args
                    generator.invokeConstructor(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_CONSTRUCTOR);
                    //Lparam;Lparam;
                    generator.storeLocal(beforeCallLocal);
                    //Lparam;
                    generator.invokeStatic(Type.getType(beforeCall.getDeclaringClass()), Method.getMethod(beforeCall));
                    //;

                    generator.loadLocal(beforeCallLocal);
                    //Lparam;
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_IS_RETURN_EARLY);
                    //trueOrFalse;
                    generator.push(true);
                    //trueOrFalse;true;
                    generator.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, checkThrow);
                    //;
                    generator.loadLocal(beforeCallLocal);
                    //Lparam;
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_GET_RESULT);
                    //result;
                    generator.checkCast(Type.getReturnType(descriptor));
                    //result;
                    generator.returnValue();

                    generator.mark(checkThrow);
                    //;
                    generator.loadLocal(beforeCallLocal);
                    //Lparam;
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_IS_THROW_EARLY);
                    //trueOrFalse;
                    generator.push(true);
                    //trueOrFalse;true;
                    generator.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, invokeOriginal);
                    //;
                    generator.loadLocal(beforeCallLocal);
                    //Lparam;
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_GET_THROWABLE);
                    //throwable;
                    generator.throwException();

                    generator.mark(invokeOriginal);
                }

                if (null != afterThrow) {
                    generator.mark(tryLabel);
                }

                if ((access & Opcodes.ACC_STATIC) != 0) {
                    loadArgsFromHook(generator);
                    generator.invokeStatic(targetClassType, realMethod);
                } else {
                    generator.loadThis();
                    loadArgsFromHook(generator);
                    generator.invokeVirtual(targetClassType, realMethod);
                }

                if (Type.getReturnType(descriptor).equals(Type.VOID_TYPE)) {
                    generator.push((Type) null);
                }

                if (null != afterReturn) {
                    Label returnLabel = new Label();
                    afterReturnLocal = generator.newLocal(METHOD_HOOK_PARAM_TYPE);

                    //returnValue;
                    generator.newInstance(METHOD_HOOK_PARAM_TYPE);
                    //returnValue;arParam;
                    generator.dupX1();
                    //arParam;returnValue;arParam;
                    generator.swap();
                    //arParam;arParam;returnValue;
                    loadThisOrNull(generator);
                    //arParam;arParam;returnValue;thisOrNull;
                    loadArgArrayFromHook(generator);
                    //arParam;arParam;returnValue;thisOrNull;[args
                    generator.invokeConstructor(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_CONSTRUCTOR_WITH_RESULT);
                    //arParam;
                    generator.dup();
                    //arParam;arParam;
                    generator.storeLocal(afterReturnLocal);
                    //arParam;
                    generator.invokeStatic(Type.getType(afterReturn.getDeclaringClass()), Method.getMethod(afterReturn));
                    //;

                    generator.loadLocal(afterReturnLocal);
                    //arParam;
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_IS_THROW_EARLY);
                    //trueOrFalse;
                    generator.push(true);
                    //trueOrFalse;true
                    generator.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, returnLabel);
                    //;
                    generator.loadLocal(afterReturnLocal);
                    //arParam;
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_GET_THROWABLE);
                    //throwable;
                    generator.throwException();

                    generator.mark(returnLabel);
                    //;
                    generator.loadLocal(afterReturnLocal);
                    //arParam;
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_GET_RESULT);
                    //result;
                    generator.checkCast(Type.getReturnType(descriptor));
                }

                generator.returnValue();
                generator.goTo(finallyLabel);

                if (null != afterThrow) {
                    Label finishLabel = new Label();
                    afterThrowLocal = generator.newLocal(METHOD_HOOK_PARAM_TYPE);

                    generator.mark(catchLabel);
                    generator.catchException(tryLabel, catchLabel, Type.getType(Throwable.class));
                    //throwable;
                    generator.newInstance(METHOD_HOOK_PARAM_TYPE);
                    //throwable;atParam;
                    generator.dupX1();
                    //atParam;throwable;atParam;
                    generator.swap();
                    //atParam;atParam;throwable;
                    loadThisOrNull(generator);
                    //atParam;atParam;throwable;thisOrNull;
                    loadArgArrayFromHook(generator);
                    //atParam;atParam;throwable;thisOrNull;[args;
                    generator.invokeConstructor(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_CONSTRUCTOR_WITH_THROWABLE);
                    //atParam;
                    generator.dup();
                    //atParam;atParam;
                    generator.storeLocal(afterThrowLocal);
                    //atParam;
                    generator.invokeStatic(Type.getType(afterThrow.getDeclaringClass()), Method.getMethod(afterThrow));
                    //;

                    generator.loadLocal(afterThrowLocal);
                    //atParam;
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_IS_RETURN_EARLY);
                    //trueOrFalse;
                    generator.push(true);
                    //trueOrFalse;true;
                    generator.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, finishLabel);
                    //;
                    generator.loadLocal(afterThrowLocal);
                    //atParam;
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_GET_RESULT);
                    //result;
                    generator.checkCast(Type.getReturnType(descriptor));
                    generator.returnValue();

                    generator.mark(finishLabel);
                    generator.loadLocal(afterThrowLocal);
                    generator.invokeVirtual(METHOD_HOOK_PARAM_TYPE, METHOD_HOOK_PARAM_GET_THROWABLE);
                    generator.throwException();
                }

                generator.mark(finallyLabel);
                generator.endMethod();

                success = true;
            }catch (Throwable t){
                t.printStackTrace();
            }
        }
        super.visitEnd();
    }

    private void loadThisOrNull(GeneratorAdapter generator){
        if((access & Opcodes.ACC_STATIC) != 0){
            generator.push((Type)null);
        } else {
            generator.loadThis();
        }
    }

    private void loadArgsFromHook(GeneratorAdapter generator){
        if(beforeCallLocal < 0){
            generator.loadArgs();
            return;
        }
        generator.loadLocal(beforeCallLocal, METHOD_HOOK_PARAM_TYPE);
        generator.getField(METHOD_HOOK_PARAM_TYPE, "args", Type.getType(Object[].class));
        for(int i = 0; i < targetMethodParamTypes.length; i++){
            generator.dup();
            generator.push(i);
            generator.arrayLoad(Type.getType(Object.class));
            generator.unbox(targetMethodParamTypes[i]);
            generator.swap();
        }
        generator.pop();
    }

    private void loadArgArrayFromHook(GeneratorAdapter generator){
        if(beforeCallLocal < 0){
            generator.loadArgArray();
            return;
        }

        generator.loadLocal(beforeCallLocal, METHOD_HOOK_PARAM_TYPE);
        generator.getField(METHOD_HOOK_PARAM_TYPE, "args", Type.getType(Object[].class));
    }

}
