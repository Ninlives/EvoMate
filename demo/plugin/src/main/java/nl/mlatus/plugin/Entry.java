package nl.mlatus.plugin;

import nl.mlatus.api.ClassHook;
import nl.mlatus.api.HookLogger;
import nl.mlatus.api.LoadClassParam;

public class Entry extends ClassHook {
    @Override
    public void handleLoadClass(LoadClassParam param) {
        switch (param.getClassName()){
            case "nl.mlatus.example.Data":
                hookMethod(param, DataHook.class, "getUser", int.class);
                break;
            case "nl.mlatus.example.User":
                hookMethod(param, UserHook.class, "toString");
                break;
            case "nl.mlatus.example.Main":
                hookMethod(param, MainHook.class, "main", String[].class);
        }
    }
}

