package nl.mlatus.plugin;

import nl.mlatus.api.MethodHookParam;
import nl.mlatus.api.annotations.BeforeCall;
import nl.mlatus.example.User;

public class UserHook {
    @BeforeCall
    public static void prettify(MethodHookParam param){
        User target = (User)param.thisObject;
        param.setResult(String.format("Name: %s\nRole: %s\nAge: %d",
                                      target.getName(), target.getRole(), target.getAge()));
    }
}
