import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MacroManager {
    private Map<String, List<Action>> macros;
    private Map<String, String> macroFolders;
    private Map<String, String> macroDescriptions;
    private Map<String, String> macroComments;
    private Map<String, String> macroSoftware;
    private String macrosDirectory = "macros";
    
    public MacroManager() {
        macros = new HashMap<>();
        macroFolders = new HashMap<>();
        macroDescriptions = new HashMap<>();
        macroComments = new HashMap<>();
        macroSoftware = new HashMap<>();
        
        File dir = new File(macrosDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public void createMacro(String name, String folderPath) {
        macros.put(name, new ArrayList<>());
        if (folderPath != null && !folderPath.isEmpty()) {
            macroFolders.put(name, folderPath);
        }
    }
    
    public void deleteMacro(String name) {
        macros.remove(name);
        macroFolders.remove(name);
    }
    
    public void saveMacro(String name, List<Action> actions) {
        saveMacro(name, null, null, null, actions);
    }
    
    public void saveMacro(String name, String description, String comment, String software, List<Action> actions) {
        macros.put(name, new ArrayList<>(actions));
        if (description != null) macroDescriptions.put(name, description);
        if (comment != null) macroComments.put(name, comment);
        if (software != null) macroSoftware.put(name, software);
        
        String folderPath = macroFolders.getOrDefault(name, "");
        String fileName = name;
        if (name.contains("/")) {
            fileName = name.substring(name.lastIndexOf("/") + 1);
        }
        fileName = fileName + ".amc";
        
        File targetDir;
        if (folderPath != null && !folderPath.isEmpty()) {
            targetDir = new File(macrosDirectory, folderPath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
        } else {
            targetDir = new File(macrosDirectory);
        }
        
        String displayName = name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name;
        String filePath = new File(targetDir, fileName).getAbsolutePath();
        MacroFileManager.saveMacro(filePath, displayName, 
            description != null ? description : "",
            comment != null ? comment : "",
            software != null ? software : "",
            actions);
    }
    
    public List<Action> getMacro(String name) {
        return new ArrayList<>(macros.getOrDefault(name, new ArrayList<>()));
    }
    
    public List<String> getAllMacros() {
        return new ArrayList<>(macros.keySet());
    }
    
    public String getMacroFolder(String name) {
        return macroFolders.getOrDefault(name, "");
    }
    
    public List<String> getMacrosInFolder(String folderPath) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : macroFolders.entrySet()) {
            if (folderPath.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    public List<String> getFolders() {
        List<String> folders = new ArrayList<>();
        for (String folder : macroFolders.values()) {
            if (folder != null && !folder.isEmpty() && !folders.contains(folder)) {
                folders.add(folder);
            }
        }
        return folders;
    }
    
    public boolean macroExists(String name) {
        return macros.containsKey(name);
    }
    
    public void loadMacrosFromFiles() {
        File dir = new File(macrosDirectory);
        if (!dir.exists()) {
            return;
        }
        
        loadMacrosRecursively(dir, "");
    }
    
    private void loadMacrosRecursively(File directory, String relativePath) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                String folderPath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                loadMacrosRecursively(file, folderPath);
            } else if (file.isFile() && file.getName().endsWith(".amc")) {
                try {
                    String name = file.getName().replace(".amc", "");
                    String fullName = relativePath.isEmpty() ? name : relativePath + "/" + name;
                    MacroFileManager.MacroData data = MacroFileManager.loadMacro(file.getAbsolutePath());
                    macros.put(fullName, data.getActions());
                    macroDescriptions.put(fullName, data.getDescription());
                    macroComments.put(fullName, data.getComment());
                    macroSoftware.put(fullName, data.getSoftware());
                    if (!relativePath.isEmpty()) {
                        macroFolders.put(fullName, relativePath);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при загрузке макроса " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    public String getMacroDescription(String name) {
        return macroDescriptions.getOrDefault(name, "");
    }
    
    public String getMacroComment(String name) {
        return macroComments.getOrDefault(name, "");
    }
    
    public String getMacroSoftware(String name) {
        return macroSoftware.getOrDefault(name, "");
    }
    
    public void deleteMacroFile(String name) {
        String folderPath = macroFolders.getOrDefault(name, "");
        String fileName = name;
        if (name.contains("/")) {
            fileName = name.substring(name.lastIndexOf("/") + 1);
        }
        fileName = fileName + ".amc";
        
        File file;
        if (folderPath != null && !folderPath.isEmpty()) {
            file = new File(macrosDirectory, folderPath + "/" + fileName);
        } else {
            file = new File(macrosDirectory, fileName);
        }
        
        if (file.exists()) {
            file.delete();
        }
    }
}
