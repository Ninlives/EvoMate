package nl.mlatus.plugin;

import nl.mlatus.api.MethodHookParam;
import nl.mlatus.api.annotations.AfterThrow;
import nl.mlatus.example.Main;

public class MainHook {
    @AfterThrow
    public static void handleIndexOutofRange(MethodHookParam param){
        if(param.getThrowable() instanceof ArrayIndexOutOfBoundsException) {
            System.err.println("Warning: serial number out of range!");
            Main.main((String[]) param.args[0]);
        }
    }
}
