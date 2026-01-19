import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class AppLauncher {
    private Robot robot;

    public AppLauncher() {
        try {
            robot = new Robot();
            robot.setAutoDelay(50);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void launchApp(String appName) {
        try {
            if (appName.toLowerCase().endsWith(".exe")) {
                ProcessBuilder pb = new ProcessBuilder(appName);
                pb.start();
                Thread.sleep(1000);
            } else {
                String command = getAppCommand(appName);
                if (command != null) {
                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.start();
                    Thread.sleep(1000);
                } else {
                    openViaStartMenu(appName);
                }
            }
        } catch (Exception e) {
            openViaStartMenu(appName);
        }
    }

    public void openViaStartMenu(String appName) {
        try {
            int windowsKey = 524;
            try {
                robot.keyPress(windowsKey);
                robot.delay(200);
                robot.keyRelease(windowsKey);
            } catch (Exception e) {
                try {
                    java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                        new java.awt.event.KeyEvent(
                            new java.awt.Button(),
                            KeyEvent.KEY_PRESSED,
                            System.currentTimeMillis(),
                            0,
                            windowsKey,
                            (char)windowsKey
                        )
                    );
                    Thread.sleep(200);
                } catch (Exception ex) {}
            }
            
            Thread.sleep(300);

            typeString(appName);
            Thread.sleep(1000);

            robot.keyPress(KeyEvent.VK_ENTER);
            robot.delay(50);
            robot.keyRelease(KeyEvent.VK_ENTER);
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void activateWindow(String windowTitle) {
        try {
            robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.delay(100);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_ALT);
            Thread.sleep(500);

            robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.delay(100);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_ALT);
            Thread.sleep(300);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clickAt(int x, int y) {
        robot.mouseMove(x, y);
        robot.delay(100);
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(50);
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(100);
    }

    public void typeString(String text) {
        for (char c : text.toCharArray()) {
            typeChar(c);
            robot.delay(20);
        }
    }

    private void typeChar(char character) {
        boolean shift = Character.isUpperCase(character);
        
        if (shift) {
            robot.keyPress(KeyEvent.VK_SHIFT);
        }

        int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
        if (keyCode != KeyEvent.VK_UNDEFINED) {
            robot.keyPress(keyCode);
            robot.delay(10);
            robot.keyRelease(keyCode);
        }

        if (shift) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
    }

    private String getAppCommand(String appName) {
        String lower = appName.toLowerCase();
        if (lower.contains("notepad")) return "notepad.exe";
        if (lower.contains("calc")) return "calc.exe";
        if (lower.contains("paint")) return "mspaint.exe";
        if (lower.contains("cmd")) return "cmd.exe";
        if (lower.contains("explorer")) return "explorer.exe";
        if (lower.contains("chrome")) return "chrome.exe";
        if (lower.contains("firefox")) return "firefox.exe";
        if (lower.contains("word")) return "winword.exe";
        if (lower.contains("excel")) return "excel.exe";
        return null;
    }
}