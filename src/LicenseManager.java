import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Calendar;

public class LicenseManager {
    private static final String LICENSE_FOLDER_NAME = "MacrosLicense";
    private static final String LICENSE_FILE = "license.dat";
    private static final String SYSTEM_FILE = "system.dat";
    private static final int TRIAL_DAYS = 7;
    
    private Properties props;
    private Properties systemProps;
    private File licenseDir;
    private File licenseFile;
    private File systemFile;
    
    public enum LicenseType {
        TRIAL,
        TEST_5SEC,
        MONTHLY,
        YEARLY,
        LIFETIME
    }
    
    public LicenseManager() {
        initializeLicenseDirectory();
        props = new Properties();
        systemProps = new Properties();
        loadLicenseData();
        loadSystemData();
    }
    
    private void initializeLicenseDirectory() {
        String appDataPath = System.getenv("PROGRAMDATA");
        File testDir = null;
        
        if (appDataPath != null && !appDataPath.isEmpty()) {
            testDir = new File(appDataPath, LICENSE_FOLDER_NAME);
            if (!testDir.exists()) {
                if (!testDir.mkdirs()) {
                    testDir = null;
                }
            } else {
                File testFile = new File(testDir, ".test");
                try {
                    testFile.createNewFile();
                    testFile.delete();
                } catch (Exception e) {
                    testDir = null;
                }
            }
        }
        
        if (testDir == null || !testDir.canWrite()) {
            appDataPath = System.getProperty("user.home");
        }
        
        licenseDir = new File(appDataPath, LICENSE_FOLDER_NAME);
        if (!licenseDir.exists()) {
            licenseDir.mkdirs();
        }
        
        try {
            java.nio.file.Files.setAttribute(licenseDir.toPath(), "dos:hidden", true);
        } catch (Exception e) {
            // Ignore if can't set hidden attribute
        }
        
        licenseFile = new File(licenseDir, LICENSE_FILE);
        systemFile = new File(licenseDir, SYSTEM_FILE);
    }
    
    public void loadLicenseData() {
        if (licenseFile.exists()) {
            try (FileInputStream fis = new FileInputStream(licenseFile)) {
                props.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void saveLicenseData() {
        try {
            if (!licenseDir.exists()) {
                if (!licenseDir.mkdirs()) {
                    throw new IOException("Не удалось создать директорию: " + licenseDir.getAbsolutePath());
                }
            }
            
            if (!licenseDir.canWrite()) {
                throw new IOException("Нет прав на запись в директорию: " + licenseDir.getAbsolutePath());
            }
            
            if (licenseFile.exists()) {
                if (!licenseFile.canWrite()) {
                    try {
                        licenseFile.setWritable(true);
                    } catch (Exception e) {
                        throw new IOException("Не удалось установить права на запись для файла: " + licenseFile.getAbsolutePath(), e);
                    }
                }
                try {
                    java.nio.file.Files.setAttribute(licenseFile.toPath(), "dos:hidden", false);
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            try (FileOutputStream fos = new FileOutputStream(licenseFile, false)) {
                props.store(fos, "License data");
                fos.flush();
            }
            
            try {
                java.nio.file.Files.setAttribute(licenseFile.toPath(), "dos:hidden", true);
            } catch (Exception e) {
                // Ignore if can't set hidden attribute
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении лицензии: " + e.getMessage(), e);
        }
    }
    
    private void loadSystemData() {
        if (systemFile.exists()) {
            try (FileInputStream fis = new FileInputStream(systemFile)) {
                systemProps.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void saveSystemData() {
        try {
            if (!licenseDir.exists()) {
                if (!licenseDir.mkdirs()) {
                    return;
                }
            }
            
            if (!licenseDir.canWrite()) {
                return;
            }
            
            if (systemFile.exists()) {
                if (!systemFile.canWrite()) {
                    try {
                        systemFile.setWritable(true);
                    } catch (Exception e) {
                        return;
                    }
                }
                try {
                    java.nio.file.Files.setAttribute(systemFile.toPath(), "dos:hidden", false);
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            try (FileOutputStream fos = new FileOutputStream(systemFile, false)) {
                systemProps.store(fos, "System installation data");
                fos.flush();
            }
            
            try {
                java.nio.file.Files.setAttribute(systemFile.toPath(), "dos:hidden", true);
            } catch (Exception e) {
                // Ignore if can't set hidden attribute
            }
        } catch (IOException e) {
            // Ignore system data save errors
        }
    }
    
    public boolean isLicenseValid() {
        String licenseKey = getLicenseKey();
        if (licenseKey != null && !licenseKey.trim().isEmpty()) {
            if (validateLicenseKey(licenseKey)) {
                LicenseType type = parseLicenseType(licenseKey);
                return isLicenseTypeValid(type, licenseKey);
            }
        }
        
        return isTrialValid();
    }
    
    private boolean isLicenseTypeValid(LicenseType type, String key) {
        switch (type) {
            case LIFETIME:
                return true;
                
            case TEST_5SEC:
                String activationDate = props.getProperty("activationDate_" + key);
                if (activationDate == null || activationDate.isEmpty()) {
                    setActivationDate(key);
                    return true;
                }
                return isDateWithinPeriodSeconds(activationDate, 5);
                
            case MONTHLY:
                activationDate = props.getProperty("activationDate_" + key);
                if (activationDate == null || activationDate.isEmpty()) {
                    setActivationDate(key);
                    return true;
                }
                return isDateWithinPeriod(activationDate, 30);
                
            case YEARLY:
                activationDate = props.getProperty("activationDate_" + key);
                if (activationDate == null || activationDate.isEmpty()) {
                    setActivationDate(key);
                    return true;
                }
                return isDateWithinPeriod(activationDate, 365);
                
            case TRIAL:
            default:
                return isTrialValid();
        }
    }
    
    private boolean isDateWithinPeriod(String startDateStr, int days) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = sdf.parse(startDateStr);
            Date now = new Date();
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DAY_OF_MONTH, days);
            Date endDate = cal.getTime();
            
            return now.before(endDate);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isDateWithinPeriodSeconds(String startDateStr, int seconds) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date startDate = sdf.parse(startDateStr);
            Date now = new Date();
            
            long diffInMillis = now.getTime() - startDate.getTime();
            long diffInSeconds = diffInMillis / 1000;
            
            return diffInSeconds < seconds;
        } catch (Exception e) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date startDate = sdf.parse(startDateStr);
                setActivationDate(startDateStr.replace(" ", " 00:00:00"));
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
    
    public boolean isTrialValid() {
        String firstSystemRun = systemProps.getProperty("firstSystemRun");
        if (firstSystemRun == null || firstSystemRun.isEmpty()) {
            setFirstSystemRun();
            return true;
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date firstRun = sdf.parse(firstSystemRun);
            Date now = new Date();
            
            long diffInMillis = now.getTime() - firstRun.getTime();
            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
            
            return diffInDays < TRIAL_DAYS;
        } catch (Exception e) {
            return true;
        }
    }
    
    private void setFirstSystemRun() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        systemProps.setProperty("firstSystemRun", sdf.format(new Date()));
        saveSystemData();
    }
    
    public int getRemainingTrialDays() {
        String firstSystemRun = systemProps.getProperty("firstSystemRun");
        if (firstSystemRun == null || firstSystemRun.isEmpty()) {
            return TRIAL_DAYS;
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date firstRun = sdf.parse(firstSystemRun);
            Date now = new Date();
            
            long diffInMillis = now.getTime() - firstRun.getTime();
            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
            
            int remaining = TRIAL_DAYS - (int) diffInDays;
            return Math.max(0, remaining);
        } catch (Exception e) {
            return TRIAL_DAYS;
        }
    }
    
    private void setActivationDate(String key) {
        LicenseType type = parseLicenseType(key);
        String format;
        if (type == LicenseType.TEST_5SEC) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            props.setProperty("activationDate_" + key, sdf.format(new Date()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            props.setProperty("activationDate_" + key, sdf.format(new Date()));
        }
        saveLicenseData();
    }
    
    public String getLicenseKey() {
        return props.getProperty("licenseKey", "");
    }
    
    public void setLicenseKey(String key) {
        String oldKey = getLicenseKey();
        String cleanKey = key.trim().replace("-", "").replace(" ", "").toUpperCase();
        
        if (validateLicenseKey(cleanKey)) {
            if (oldKey != null && !oldKey.isEmpty() && !oldKey.equals(cleanKey)) {
                props.remove("activationDate_" + oldKey);
            }
            
            props.setProperty("licenseKey", cleanKey);
            
            LicenseType type = parseLicenseType(cleanKey);
            if (type != LicenseType.TRIAL && type != LicenseType.LIFETIME) {
                setActivationDate(cleanKey);
            }
            
            saveLicenseData();
            loadLicenseData();
        } else {
            if (oldKey != null && !oldKey.isEmpty() && !oldKey.equals(cleanKey)) {
                return;
            }
            props.remove("licenseKey");
            saveLicenseData();
        }
    }
    
    private boolean validateLicenseKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        String cleanKey = key.trim().replace("-", "").replace(" ", "").toUpperCase();
        
        if (cleanKey.length() < 16) {
            return false;
        }
        
        LicenseType type = parseLicenseType(cleanKey);
        int typeMultiplier = getTypeMultiplier(type);
        
        String keyWithoutChecksum = cleanKey.substring(0, cleanKey.length() - 4);
        String checksum = cleanKey.substring(cleanKey.length() - 4);
        
        int sum = 0;
        for (char c : keyWithoutChecksum.toCharArray()) {
            if (Character.isDigit(c)) {
                sum += Character.getNumericValue(c) * typeMultiplier;
            } else if (Character.isLetter(c)) {
                sum += (c - 'A' + 1) * typeMultiplier;
            }
        }
        
        int calculatedChecksum = (sum % 9999) + 1000;
        String expectedChecksum = String.format("%04d", calculatedChecksum);
        
        return expectedChecksum.equals(checksum);
    }
    
    private int getTypeMultiplier(LicenseType type) {
        switch (type) {
            case TRIAL:
                return 7;
            case MONTHLY:
                return 13;
            case YEARLY:
                return 19;
            case LIFETIME:
                return 31;
            default:
                return 1;
        }
    }
    
    private LicenseType parseLicenseType(String key) {
        if (key == null || key.length() < 1) {
            return LicenseType.TRIAL;
        }
        
        char prefix = key.toUpperCase().charAt(0);
        switch (prefix) {
            case 'T':
                return LicenseType.TRIAL;
            case 'S':
                return LicenseType.TEST_5SEC;
            case 'M':
                return LicenseType.MONTHLY;
            case 'Y':
                return LicenseType.YEARLY;
            case 'L':
                return LicenseType.LIFETIME;
            default:
                return LicenseType.TRIAL;
        }
    }
    
    public LicenseType getLicenseType() {
        String key = getLicenseKey();
        if (key == null || key.isEmpty()) {
            return LicenseType.TRIAL;
        }
        return parseLicenseType(key);
    }
    
    public String getLicenseTypeName() {
        LicenseType type = getLicenseType();
        switch (type) {
            case TRIAL:
                return "Пробный период";
            case TEST_5SEC:
                return "Тестовая лицензия (5 секунд)";
            case MONTHLY:
                return "Месячная лицензия";
            case YEARLY:
                return "Годовая лицензия";
            case LIFETIME:
                return "Бессрочная лицензия";
            default:
                return "Неизвестный тип";
        }
    }
    
    public int getRemainingLicenseDays() {
        String key = getLicenseKey();
        if (key == null || key.isEmpty()) {
            return getRemainingTrialDays();
        }
        
        LicenseType type = parseLicenseType(key);
        if (type == LicenseType.LIFETIME) {
            return Integer.MAX_VALUE;
        }
        
        String activationDate = props.getProperty("activationDate_" + key);
        if (activationDate == null || activationDate.isEmpty()) {
            if (type == LicenseType.TEST_5SEC) {
                return 0;
            }
            return type == LicenseType.MONTHLY ? 30 : (type == LicenseType.YEARLY ? 365 : 0);
        }
        
        if (type == LicenseType.TEST_5SEC) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date startDate = sdf.parse(activationDate);
                Date now = new Date();
                long diffInMillis = now.getTime() - startDate.getTime();
                long diffInSeconds = diffInMillis / 1000;
                int remaining = 5 - (int) diffInSeconds;
                return Math.max(0, remaining);
            } catch (Exception e) {
                return 0;
            }
        }
        
        int periodDays = type == LicenseType.MONTHLY ? 30 : 365;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = sdf.parse(activationDate);
            Date now = new Date();
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DAY_OF_MONTH, periodDays);
            Date endDate = cal.getTime();
            
            if (now.after(endDate)) {
                return 0;
            }
            
            long diffInMillis = endDate.getTime() - now.getTime();
            return (int) (diffInMillis / (1000 * 60 * 60 * 24));
        } catch (Exception e) {
            return 0;
        }
    }
    
    public boolean hasLicenseKey() {
        String key = getLicenseKey();
        return key != null && !key.trim().isEmpty();
    }
    
    public String getLicenseDirectoryPath() {
        return licenseDir != null ? licenseDir.getAbsolutePath() : "";
    }
}
