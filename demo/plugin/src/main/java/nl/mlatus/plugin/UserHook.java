package nl.mlatus.plugin;

import nl.mlatus.api.MethodHookParam;
import nl.mlatus.api.annotations.AfterReturn;
import nl.mlatus.api.annotations.BeforeCall;
import nl.mlatus.example.User;

public class UserHook {
    @BeforeCall
    public static void prettify(MethodHookParam param){
        User target = (User)param.thisObject;
        //Replace the original toString with this method, produce better result
        param.setResult(String.format("Name:  %s\nEmail: %s\nAge:   %d",
                                      target.getName(), target.getEmail(), target.getAge()));
    }
}
