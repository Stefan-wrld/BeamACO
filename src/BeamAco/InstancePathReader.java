package BeamAco;

import java.io.File;

public class InstancePathReader {
    public static String[] getFileNames(String folder) {
       File dirPath = new File(folder);
       String[] tsptwFiles = dirPath.list();
       return tsptwFiles;
    }
}
