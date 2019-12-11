package nl.mlatus.api;

import nl.mlatus.internal.AdviceManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HookLogger {
    private static Logger logger = null;

    public static void init(Level level){
        logger = Logger.getLogger("EvoMate");
        logger.setLevel(level);
    }

    public static void warning(String msg){
        if(null != logger) {
            logger.warning(msg);
        }
    }
    public static void info(String msg){
        if(null != logger) {
            logger.info(msg);
        }
    }
    public static void failedToHook(String method, String className, Class module, String msg){
        HookLogger.warning(String.format("Failed to hook method %s of class %s using module %s:\n %s",
                method, className, module.getName(), msg));
    }
    public static void success(String method, String className, Class module){
        HookLogger.info(String.format("Successfully inject into method %s of class %s using module %s",
                method, className, module.getName()));
    }
}
