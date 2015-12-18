/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass;

import hep.dataforge.data.BinaryData;
import hep.dataforge.data.FileData;
import hep.dataforge.io.BasicIOManager;
import hep.dataforge.meta.Meta;
import hep.dataforge.names.Name;
import inr.numass.data.NumassDataReader;
import inr.numass.data.NumassPawReader;
import inr.numass.data.RawNMFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.output.TeeOutputStream;

/**
 *
 * @author Darksnake
 */
public class NumassIO extends BasicIOManager {

    public static final String NUMASS_OUTPUT_CONTEXT_KEY = "numass.outputDir";
    
    @Override
    public OutputStream out(Name stage, Name name) {
        List<String> tokens = new ArrayList<>();
        if (getContext().hasValue("numass.path")) {
            String path = getContext().getString("numass.path");
            if (path.contains(".")) {
                tokens.addAll(Arrays.asList(path.split(".")));
            } else {
                tokens.add(path);
            }
        }

        if (stage != null) {
            tokens.addAll(Arrays.asList(stage.asArray()));
        }

        String dirName = String.join(File.separator, tokens);
        String fileName = name.removeNameSpace().toString() + ".out";
        return buildOut(getOutputDir(), dirName, fileName);
    }

    private File getOutputDir() {
        String outputDirPath = getContext().getString(NUMASS_OUTPUT_CONTEXT_KEY, ".dataforge");
        File res = new File(getRootDirectory(), outputDirPath);
        if (!res.exists()) {
            res.mkdir();
        }
        return res;

    }

    protected OutputStream buildOut(File parentDir, String dirName, String fileName) {
        File outputFile;

        if (!parentDir.exists()) {
            throw new RuntimeException("Working directory does not exist");
        }
        if (dirName != null && !dirName.isEmpty()) {
            parentDir = new File(parentDir, dirName);
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
        }

//        String output = source.meta().getString("output", this.meta().getString("output", fileName + ".out"));
        outputFile = new File(parentDir, fileName);
        try {
            if (getContext().getBoolean("numass.consoleOutput", false)) {
                return new TeeOutputStream(new FileOutputStream(outputFile), System.out);
            } else {
                return new FileOutputStream(outputFile);
            }
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static RawNMFile readAsDat(BinaryData source, Meta config) {
        try {
            return new NumassDataReader(source, config).read();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static RawNMFile readAsPaw(BinaryData source) {
        try {
            return new NumassPawReader().readPaw(source.getInputStream(), source.getName());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static RawNMFile getNumassData(FileData source, Meta config) {
        RawNMFile dataFile;
        switch (source.getExtension()) {
            case "paw":
                dataFile = readAsPaw(source);
                break;
            case "dat":
                dataFile = readAsDat(source, config);
                break;
            default:
                throw new RuntimeException("Wrong file format");
        }
        return dataFile;
    }
}