package nl.mlatus.plugin;

import nl.mlatus.api.MethodHookParam;
import nl.mlatus.api.annotations.BeforeCall;

public class DataHook {
    @BeforeCall
    public static void useNaturalSerialNumber(MethodHookParam param){
        param.args[0] = (int)param.args[0] - 1;
    }
}
