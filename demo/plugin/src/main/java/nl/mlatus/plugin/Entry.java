package nl.mlatus.plugin;

import nl.mlatus.api.ClassHook;
import nl.mlatus.api.LoadClassParam;

public class Entry extends ClassHook {
    @Override
    public void handleLoadClass(LoadClassParam param) {
        switch (param.getClassName()){
            case "nl.mlatus.example.Data":
                hookMethod(param, DataHook.class, "printUser", int.class);
                break;
            case "nl.mlatus.example.User":
                hookMethod(param, UserHook.class, "toString");
                break;
        }
    }
}

