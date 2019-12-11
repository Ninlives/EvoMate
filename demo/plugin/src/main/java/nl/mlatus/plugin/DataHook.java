package nl.mlatus.plugin;

import nl.mlatus.api.MethodHookParam;
import nl.mlatus.api.annotations.AfterThrow;
import nl.mlatus.api.annotations.BeforeCall;

public class DataHook {
    @BeforeCall
    //This method will be called before printUser is executed
    public static void processBeforeExecution(MethodHookParam param){
        //Display a message
        System.out.printf("Retrieving information for user number %d...%n", (int)param.args[0]);
        //Change the parameter passed to printUser
        param.args[0] = (int)param.args[0] - 1;
    }

    @AfterThrow
    //This method will be called when printUser throws an exception
    public static void handelIndexOutOfBound(MethodHookParam param){
        if(param.getThrowable() instanceof ArrayIndexOutOfBoundsException){
            //Print a warning
            System.err.println("Warning: No such user in the database!");
            //Instead of throwing an exception and crash, return normally
            param.returnEarly();
        }
    }
}
