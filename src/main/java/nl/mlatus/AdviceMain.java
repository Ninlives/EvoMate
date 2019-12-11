package nl.mlatus;

import nl.mlatus.api.HookLogger;
import nl.mlatus.internal.AdviceManager;
import nl.mlatus.internal.Transformer;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdviceMain {

    public static void premain(String args, Instrumentation inst) {
        Logger earlyLogger = Logger.getLogger("EvoMate");
        Map<String, String> option = getOpt(args);
        Level level = Level.WARNING;
        if(option.containsKey("logLevel")){
            try {
                level = Level.parse(option.get("logLevel"));
            } catch (IllegalArgumentException e){
                earlyLogger.warning("Invalid value of option 'logLevel', use default value.");
            }
        }
        HookLogger.init(level);

        URL agentURL = ClassLoader.
                getSystemClassLoader().
                getResource(AdviceMain.class.getCanonicalName().replace('.', '/') + ".class");
        if(null == agentURL){
            HookLogger.warning("Can't find the lib jar.");
            return;
        }

        try {
            String urlString = agentURL.getFile();
            URL file = new URL(urlString.substring(0, urlString.indexOf('!')));
            JarFile libJar = new JarFile(new File(file.toURI()));
            //inst.appendToBootstrapClassLoaderSearch(libJar);
        } catch (IOException | URISyntaxException e) {
            HookLogger.warning("Can't load the lib jar.");
            return;
        }
        AdviceManager.init(option, inst);
        inst.addTransformer(new Transformer(inst));
    }


    private static Map<String, String> getOpt(String args) {
        Map<String, String> opts = new HashMap<>();

        if(args != null) {
            for (String key_value : args.split(",")) {
                String[] kv = key_value.split("=");
                if (kv.length == 2) {
                    opts.put(kv[0], kv[1]);
                }
            }
        }

        return opts;
    }
}
