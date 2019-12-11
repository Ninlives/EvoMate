package nl.mlatus.internal;

import nl.mlatus.api.HookLogger;
import nl.mlatus.api.MethodHookParam;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;

public class Constant {
    public static int API_VERSION = Opcodes.ASM6;
    public static Type METHOD_HOOK_PARAM_TYPE = Type.getType(MethodHookParam.class);
    public static Method METHOD_HOOK_PARAM_CONSTRUCTOR;
    public static Method METHOD_HOOK_PARAM_CONSTRUCTOR_WITH_RESULT;
    public static Method METHOD_HOOK_PARAM_CONSTRUCTOR_WITH_THROWABLE;
    public static Method METHOD_HOOK_PARAM_IS_RETURN_EARLY;
    public static Method METHOD_HOOK_PARAM_IS_THROW_EARLY;
    public static Method METHOD_HOOK_PARAM_GET_RESULT;
    public static Method METHOD_HOOK_PARAM_GET_THROWABLE;
    public static final Map<String, String> PRIMITIVE_TYPE_DESCRIPTORS;

    static {

        HashMap<String, String> descriptors = new HashMap<String, String>();
        descriptors.put("void", "V");
        descriptors.put("byte", "B");
        descriptors.put("char", "C");
        descriptors.put("double", "D");
        descriptors.put("float", "F");
        descriptors.put("int", "I");
        descriptors.put("long", "J");
        descriptors.put("short", "S");
        descriptors.put("boolean", "Z");
        PRIMITIVE_TYPE_DESCRIPTORS = descriptors;

        try {
            METHOD_HOOK_PARAM_IS_RETURN_EARLY = Method.getMethod(MethodHookParam.class.getMethod("isReturnEarly"));
            METHOD_HOOK_PARAM_IS_THROW_EARLY = Method.getMethod(MethodHookParam.class.getMethod("isThrowEarly"));
            METHOD_HOOK_PARAM_GET_RESULT = Method.getMethod(MethodHookParam.class.getMethod("getResult"));
            METHOD_HOOK_PARAM_GET_THROWABLE = Method.getMethod(MethodHookParam.class.getMethod("getThrowable"));
            METHOD_HOOK_PARAM_CONSTRUCTOR = Method.getMethod(MethodHookParam.class.getDeclaredConstructor(Object.class, Object[].class));
            METHOD_HOOK_PARAM_CONSTRUCTOR_WITH_RESULT = Method.getMethod(MethodHookParam.class.getDeclaredConstructor(Object.class, Object.class, Object[].class));
            METHOD_HOOK_PARAM_CONSTRUCTOR_WITH_THROWABLE = Method.getMethod(MethodHookParam.class.getDeclaredConstructor(Throwable.class, Object.class, Object[].class));
        } catch (NoSuchMethodException e) {
            HookLogger.warning("Failed to find constructor or get result method for MethodHookParam.");
            System.exit(1);
        }
    }
}
