package nl.mlatus.plugin;

import nl.mlatus.api.ClassHook;
import nl.mlatus.api.LoadClassParam;

//This is a plugin that can be loaded by EvoMate
public class Entry extends ClassHook {
    @Override
    //This method will be called every time a class is being loaded in the target program
    public void handleLoadClass(LoadClassParam param) {
        switch (param.getClassName()){
            case "nl.mlatus.example.Data":
                //Inject the printUser method of Data class
                hookMethod(param, DataHook.class, "printUser", int.class);
                break;
            case "nl.mlatus.example.User":
                //Inject the toString method of User class
                hookMethod(param, UserHook.class, "toString");
                break;
        }
    }
}

