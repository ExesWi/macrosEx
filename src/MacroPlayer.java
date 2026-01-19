import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

public class MacroPlayer {
    private Robot robot;
    private AppLauncher appLauncher;
    private Action.ActionType lastActionType = null;

    public MacroPlayer() {
        try {
            robot = new Robot();
            robot.setAutoDelay(0);
            robot.setAutoWaitForIdle(false);
            appLauncher = new AppLauncher();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void play(List<Action> actions) {
        if (actions.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        long lastTimestamp = 0;
        lastActionType = null;
        Point lastMousePosition = null;

        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            
            long delay = action.getTimestamp() - lastTimestamp;
            if (delay > 0 && delay < 5000) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    break;
                }
            }

            if (action.getType() == Action.ActionType.KEY_PRESS && 
                isKeyCombo(action.getKeyCode(), action.getModifiers()) &&
                !isModifierKey(action.getKeyCode())) {
                int comboEndIndex = findComboRelease(actions, i, action.getKeyCode());
                if (comboEndIndex > i && comboEndIndex < actions.size()) {
                    pressKeyCombo(action.getKeyCode(), action.getModifiers());
                    lastTimestamp = actions.get(comboEndIndex).getTimestamp();
                    i = comboEndIndex;
                    continue;
                }
            }

            switch (action.getType()) {
                case MOUSE_MOVE:
                    smoothMouseMove(lastMousePosition, action.getX(), action.getY(), delay);
                    if (lastMousePosition == null) {
                        lastMousePosition = new Point();
                    }
                    lastMousePosition.x = action.getX();
                    lastMousePosition.y = action.getY();
                    break;

                case MOUSE_CLICK:
                    smoothMoveToPosition(lastMousePosition, action.getX(), action.getY());
                    robot.delay(100);
                    robot.mousePress(action.getButton());
                    if (action.getButton() == java.awt.event.InputEvent.BUTTON3_DOWN_MASK) {
                        robot.delay(100);
                    } else {
                        robot.delay(50);
                    }
                    robot.mouseRelease(action.getButton());
                    if (action.getButton() == java.awt.event.InputEvent.BUTTON3_DOWN_MASK) {
                        robot.delay(300);
                    } else {
                        robot.delay(100);
                    }
                    if (lastMousePosition == null) {
                        lastMousePosition = new Point();
                    }
                    lastMousePosition.x = action.getX();
                    lastMousePosition.y = action.getY();
                    break;

                case MOUSE_PRESS:
                    smoothMoveToPosition(lastMousePosition, action.getX(), action.getY());
                    robot.delay(100);
                    robot.mousePress(action.getButton());
                    if (action.getButton() == java.awt.event.InputEvent.BUTTON3_DOWN_MASK) {
                        robot.delay(100);
                    } else {
                        robot.delay(50);
                    }
                    if (lastMousePosition == null) {
                        lastMousePosition = new Point();
                    }
                    lastMousePosition.x = action.getX();
                    lastMousePosition.y = action.getY();
                    break;

                case MOUSE_RELEASE:
                    smoothMoveToPosition(lastMousePosition, action.getX(), action.getY());
                    robot.delay(50);
                    robot.mouseRelease(action.getButton());
                    if (action.getButton() == java.awt.event.InputEvent.BUTTON3_DOWN_MASK) {
                        robot.delay(200);
                    } else {
                        robot.delay(100);
                    }
                    if (lastMousePosition == null) {
                        lastMousePosition = new Point();
                    }
                    lastMousePosition.x = action.getX();
                    lastMousePosition.y = action.getY();
                    break;

                case KEY_PRESS:
                    if (isModifierKey(action.getKeyCode())) {
                        robot.keyPress(action.getKeyCode());
                        robot.delay(10);
                    } else {
                        pressKeyWithModifiers(action.getKeyCode(), action.getModifiers());
                        robot.delay(15);
                    }
                    break;

                case KEY_RELEASE:
                    if (isModifierKey(action.getKeyCode())) {
                        robot.keyRelease(action.getKeyCode());
                        robot.delay(10);
                    } else {
                        releaseKeyWithModifiers(action.getKeyCode(), action.getModifiers());
                        robot.delay(15);
                    }
                    break;

                case KEY_COMBO:
                    pressKeyCombo(action.getKeyCode(), action.getModifiers());
                    robot.delay(50);
                    break;

                case KEY_TYPE:
                    if (action.getKeyChar() != 0) {
                        typeChar(action.getKeyChar());
                    } else {
                        robot.keyPress(action.getKeyCode());
                        robot.delay(20);
                        robot.keyRelease(action.getKeyCode());
                        robot.delay(20);
                    }
                    break;

                case APP_LAUNCH:
                    if (action.getStringValue() != null) {
                        appLauncher.launchApp(action.getStringValue());
                    }
                    break;

                case WINDOW_ACTIVATE:
                    if (action.getStringValue() != null) {
                        appLauncher.activateWindow(action.getStringValue());
                    }
                    break;

                case DELAY:
                    robot.delay((int) action.getDelay());
                    break;
            }

            lastActionType = action.getType();
            lastTimestamp = action.getTimestamp();
        }
    }

    private void smoothMouseMove(Point startPos, int targetX, int targetY, long timeDelta) {
        Point currentPos = MouseInfo.getPointerInfo().getLocation();
        int startX = (startPos != null) ? startPos.x : currentPos.x;
        int startY = (startPos != null) ? startPos.y : currentPos.y;
        
        int dx = targetX - startX;
        int dy = targetY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 2) {
            robot.mouseMove(targetX, targetY);
            return;
        }
        
        int steps = (int) Math.max(1, Math.min(distance / 2, Math.max(timeDelta / 3, 5)));
        if (steps < 1) steps = 1;
        if (steps > 50) steps = 50;
        
        double stepX = (double) dx / steps;
        double stepY = (double) dy / steps;
        long stepDelay = Math.max(1, Math.min(timeDelta / steps, 10));
        
        for (int i = 1; i <= steps; i++) {
            int x = (int) Math.round(startX + stepX * i);
            int y = (int) Math.round(startY + stepY * i);
            robot.mouseMove(x, y);
            
            if (i < steps && stepDelay > 0) {
                try {
                    Thread.sleep(stepDelay);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        robot.delay(5);
    }

    private void smoothMoveToPosition(Point lastPos, int targetX, int targetY) {
        Point currentPos = MouseInfo.getPointerInfo().getLocation();
        int startX = (lastPos != null) ? lastPos.x : currentPos.x;
        int startY = (lastPos != null) ? lastPos.y : currentPos.y;
        
        int dx = targetX - startX;
        int dy = targetY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 2) {
            robot.mouseMove(targetX, targetY);
            robot.delay(20);
            return;
        }
        
        int steps = (int) Math.max(1, Math.min(distance / 3, 30));
        
        double stepX = (double) dx / steps;
        double stepY = (double) dy / steps;
        
        for (int i = 1; i <= steps; i++) {
            int x = (int) Math.round(startX + stepX * i);
            int y = (int) Math.round(startY + stepY * i);
            robot.mouseMove(x, y);
            robot.delay(2);
        }
        
        robot.delay(20);
    }

    private void ensureMousePosition(int x, int y) {
        Point currentPos = MouseInfo.getPointerInfo().getLocation();
        if (currentPos.x != x || currentPos.y != y) {
            int dx = x - currentPos.x;
            int dy = y - currentPos.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 10) {
                int steps = (int) Math.min(distance / 5, 20);
                double stepX = (double) dx / steps;
                double stepY = (double) dy / steps;
                
                for (int i = 1; i <= steps; i++) {
                    int stepXPos = (int) Math.round(currentPos.x + stepX * i);
                    int stepYPos = (int) Math.round(currentPos.y + stepY * i);
                    robot.mouseMove(stepXPos, stepYPos);
                    robot.delay(2);
                }
            } else {
                robot.mouseMove(x, y);
            }
            robot.delay(20);
        }
    }

    private void waitForMousePosition(int x, int y) {
        int attempts = 0;
        while (attempts < 10) {
            Point currentPos = MouseInfo.getPointerInfo().getLocation();
            if (Math.abs(currentPos.x - x) < 2 && Math.abs(currentPos.y - y) < 2) {
                break;
            }
            robot.delay(10);
            attempts++;
        }
    }

    private boolean isModifierKey(int keyCode) {
        return keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_ALT || 
               keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_META || keyCode == 524;
    }
    
    private boolean isKeyCombo(int keyCode, int modifiers) {
        if (isModifierKey(keyCode)) {
            return false;
        }
        return (modifiers & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | 
                            KeyEvent.SHIFT_DOWN_MASK | KeyEvent.META_DOWN_MASK)) != 0;
    }
    
    private int findComboRelease(List<Action> actions, int startIndex, int keyCode) {
        for (int i = startIndex + 1; i < actions.size() && i < startIndex + 15; i++) {
            Action action = actions.get(i);
            if (action.getType() == Action.ActionType.KEY_RELEASE && 
                action.getKeyCode() == keyCode) {
                return i;
            }
            if (action.getType() == Action.ActionType.KEY_PRESS && 
                !isModifierKey(action.getKeyCode()) &&
                action.getKeyCode() != keyCode) {
                break;
            }
        }
        return -1;
    }

    private void pressKeyWithModifiers(int keyCode, int modifiers) {
        if (isModifierKey(keyCode)) {
            robot.keyPress(keyCode);
            robot.delay(10);
            return;
        }
        
        if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.delay(5);
        }
        if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_ALT);
            robot.delay(5);
        }
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.delay(5);
        }
        if ((modifiers & KeyEvent.META_DOWN_MASK) != 0) {
            try {
                robot.keyPress(KeyEvent.VK_META);
                robot.delay(5);
            } catch (Exception e) {
                try {
                    robot.keyPress(524);
                    robot.delay(5);
                } catch (Exception ex) {
                }
            }
        }
        
        robot.delay(10);
        robot.keyPress(keyCode);
        robot.delay(15);
    }

    private void releaseKeyWithModifiers(int keyCode, int modifiers) {
        if (isModifierKey(keyCode)) {
            robot.keyRelease(keyCode);
            robot.delay(10);
            return;
        }
        
        robot.delay(10);
        robot.keyRelease(keyCode);
        robot.delay(10);
        
        if ((modifiers & KeyEvent.META_DOWN_MASK) != 0) {
            try {
                robot.keyRelease(KeyEvent.VK_META);
                robot.delay(5);
            } catch (Exception e) {
                try {
                    robot.keyRelease(524);
                    robot.delay(5);
                } catch (Exception ex) {
                }
            }
        }
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
            robot.delay(5);
        }
        if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_ALT);
            robot.delay(5);
        }
        if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.delay(5);
        }
    }

    private void pressKeyCombo(int keyCode, int modifiers) {
        if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.delay(10);
        }
        if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_ALT);
            robot.delay(10);
        }
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.delay(10);
        }
        if ((modifiers & KeyEvent.META_DOWN_MASK) != 0) {
            try {
                robot.keyPress(KeyEvent.VK_META);
                robot.delay(10);
            } catch (Exception e) {
                try {
                    robot.keyPress(524);
                    robot.delay(10);
                } catch (Exception ex) {
                }
            }
        }
        
        robot.delay(15);
        robot.keyPress(keyCode);
        robot.delay(20);
        robot.keyRelease(keyCode);
        robot.delay(10);
        
        if ((modifiers & KeyEvent.META_DOWN_MASK) != 0) {
            try {
                robot.keyRelease(KeyEvent.VK_META);
                robot.delay(5);
            } catch (Exception e) {
                try {
                    robot.keyRelease(524);
                    robot.delay(5);
                } catch (Exception ex) {
                }
            }
        }
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
            robot.delay(5);
        }
        if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_ALT);
            robot.delay(5);
        }
        if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.delay(5);
        }
        
        robot.delay(10);
    }

    private void typeChar(char character) {
        boolean shift = Character.isUpperCase(character);
        
        if (shift) {
            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.delay(10);
        }

        int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
        if (keyCode != KeyEvent.VK_UNDEFINED) {
            robot.keyPress(keyCode);
            robot.delay(20);
            robot.keyRelease(keyCode);
            robot.delay(10);
        }

        if (shift) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
            robot.delay(10);
        }
    }
}