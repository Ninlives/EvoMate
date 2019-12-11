package nl.mlatus.plugin;

import nl.mlatus.api.MethodHookParam;
import nl.mlatus.api.annotations.AfterThrow;
import nl.mlatus.api.annotations.BeforeCall;
import nl.mlatus.example.Main;

public class PrintHook {
    @BeforeCall
    public static void hook(MethodHookParam param){
        System.out.println("Hooked");
    }
}
