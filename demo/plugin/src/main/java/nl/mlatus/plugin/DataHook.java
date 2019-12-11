package nl.mlatus.plugin;

import nl.mlatus.api.MethodHookParam;
import nl.mlatus.api.annotations.AfterThrow;
import nl.mlatus.api.annotations.BeforeCall;

public class DataHook {
    @BeforeCall
    public static void processBeforeExecution(MethodHookParam param){
        System.out.printf("Retrieving information for user number %d...%n", (int)param.args[0]);
        param.args[0] = (int)param.args[0] - 1;
    }

    @AfterThrow
    public static void handelIndexOutOfBound(MethodHookParam param){
        if(param.getThrowable() instanceof ArrayIndexOutOfBoundsException){
            System.err.println("Warning: No such user in the database!");
            param.returnEarly();
        }
    }
}
