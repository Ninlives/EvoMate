package nl.mlatus.api;

import nl.mlatus.internal.AdviceManager;

public class HookLogger {
    public static void waring(String msg){
        AdviceManager.getLogger().warning(msg);
    }
    public static void info(String msg){
        AdviceManager.getLogger().info(msg);
    }
    public static void failedToHook(String method, String className, Class module, String msg){
        HookLogger.waring(String.format("Failed to hook method %s of class %s using module %s:\n %s",
                method, className, module.getName(), msg));
    }
}
