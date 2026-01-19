import java.util.Scanner;

public class LicenseKeyGenerator {
    public enum LicenseType {
        TRIAL,
        TEST_5SEC,
        MONTHLY,
        YEARLY,
        LIFETIME
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Генератор лицензионных ключей ===");
        System.out.println();
        System.out.println("Выберите тип лицензии:");
        System.out.println("1 - Пробный период (7 дней)");
        System.out.println("2 - Тестовая лицензия (5 секунд)");
        System.out.println("3 - Месячная лицензия");
        System.out.println("4 - Годовая лицензия");
        System.out.println("5 - Бессрочная лицензия");
        System.out.print("Ваш выбор: ");
        
        int choice = scanner.nextInt();
        LicenseType type;
        
        switch (choice) {
            case 1:
                type = LicenseType.TRIAL;
                break;
            case 2:
                type = LicenseType.TEST_5SEC;
                break;
            case 3:
                type = LicenseType.MONTHLY;
                break;
            case 4:
                type = LicenseType.YEARLY;
                break;
            case 5:
                type = LicenseType.LIFETIME;
                break;
            default:
                System.out.println("Неверный выбор!");
                return;
        }
        
        String key = generateLicenseKey(type);
        
        System.out.println();
        System.out.println("=== Сгенерированный ключ ===");
        System.out.println("Тип: " + getTypeName(type));
        System.out.println("Ключ: " + formatKey(key));
        System.out.println();
        System.out.println("Сохраните этот ключ в безопасном месте!");
    }
    
    public static String generateLicenseKey(LicenseType type) {
        StringBuilder key = new StringBuilder();
        String typePrefix = getTypePrefix(type);
        
        key.append(typePrefix);
        key.append(generateRandomSegment());
        key.append("-");
        key.append(generateRandomSegment());
        key.append("-");
        key.append(generateRandomSegment());
        key.append("-");
        key.append(generateRandomSegment());
        
        String fullKey = key.toString();
        
        String checksum = calculateChecksum(fullKey, type);
        return fullKey + "-" + checksum;
    }
    
    private static String getTypePrefix(LicenseType type) {
        switch (type) {
            case TRIAL:
                return "T";
            case TEST_5SEC:
                return "S";
            case MONTHLY:
                return "M";
            case YEARLY:
                return "Y";
            case LIFETIME:
                return "L";
            default:
                return "X";
        }
    }
    
    private static String getTypeName(LicenseType type) {
        switch (type) {
            case TRIAL:
                return "Пробный период (7 дней)";
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
    
    private static String generateRandomSegment() {
        StringBuilder segment = new StringBuilder();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        
        for (int i = 0; i < 4; i++) {
            int index = (int) (Math.random() * chars.length());
            segment.append(chars.charAt(index));
        }
        
        return segment.toString();
    }
    
    private static String calculateChecksum(String key, LicenseType type) {
        int sum = 0;
        int typeMultiplier = getTypeMultiplier(type);
        
        for (char c : key.toCharArray()) {
            if (Character.isDigit(c)) {
                sum += Character.getNumericValue(c) * typeMultiplier;
            } else if (Character.isLetter(c)) {
                sum += (c - 'A' + 1) * typeMultiplier;
            }
        }
        
        int checksum = (sum % 9999) + 1000;
        return String.format("%04d", checksum);
    }
    
    private static int getTypeMultiplier(LicenseType type) {
        switch (type) {
            case TRIAL:
                return 7;
            case TEST_5SEC:
                return 11;
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
    
    private static String formatKey(String key) {
        return key.toUpperCase();
    }
    
    public static LicenseType parseLicenseType(String key) {
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
}
