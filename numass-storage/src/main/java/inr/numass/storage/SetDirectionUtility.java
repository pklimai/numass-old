/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.storage;

import hep.dataforge.context.Context;
import hep.dataforge.context.GlobalContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A temporary utility to store set directions to avoid multiple file reading
 *
 * @author Alexander Nozik
 */
public class SetDirectionUtility {

    private static final String FILE_NAME = "numass_set_direction.map";

    private static Map<String, Boolean> directionMap = new HashMap<>();

    private static boolean isLoaded = false;

    static synchronized boolean isReversed(String setName, Function<String, Boolean> provider) {
        if (!isLoaded) {
            load(GlobalContext.instance());
        }
        return directionMap.computeIfAbsent(setName, provider);
    }

    public static File cacheFile(Context context) {
        return new File(context.io().getTmpDirectory(), FILE_NAME);
    }

    public static synchronized void load(Context context) {
        context.getLogger().info("Loading set direction utility");
        File file = cacheFile(context);
        if (file.exists()) {
            try (ObjectInputStream st = new ObjectInputStream(new FileInputStream(file))) {
                directionMap = (Map<String, Boolean>) st.readObject();
                context.getLogger().info("Set directions successfully loaded from file");
            } catch (ClassNotFoundException | IOException ex) {
                context.getLogger().error("Failed to load numass direction mapping", ex);
            }
        }

        isLoaded = true;
    }

    public static synchronized void save(Context context) {
        try {
            File file = cacheFile(context);
            if (!file.exists()) {
                file.createNewFile();
            }
            try (ObjectOutputStream st = new ObjectOutputStream(new FileOutputStream(file))) {
                st.writeObject(directionMap);
                context.getLogger().info("Set directions successfully saved to file");
            }
        } catch (IOException ex) {
            context.getLogger().error("Failed to save numass direction mapping", ex);
        }
    }
}
