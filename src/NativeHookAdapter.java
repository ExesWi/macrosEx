import java.awt.Point;
import java.util.List;
import javax.swing.SwingUtilities;

public class NativeHookAdapter {
    private static boolean available = false;
    private static List<Action> actions;
    private static long startTime;
    private static volatile boolean active;
    private static Point lastMousePos;
    private static Point firstMousePos;
    private static boolean[] keyStates;
    private static long lastMouseMoveTime = 0;
    private static final long MOUSE_MOVE_INTERVAL = 20;
    private static RecordSettings settings;
    
    private static Object keyListener;
    private static Object mouseListener;
    private static Object mouseMotionListener;
    private static Runnable stopRecordingCallback;

    static {
        try {
            Class.forName("com.github.kwhat.jnativehook.GlobalScreen");
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static void initialize(List<Action> actionsList, long start, boolean[] states) {
        initialize(actionsList, start, states, new RecordSettings());
    }
    
    public static void initialize(List<Action> actionsList, long start, boolean[] states, RecordSettings recordSettings) {
        initialize(actionsList, start, states, recordSettings, null);
    }
    
    public static void initialize(List<Action> actionsList, long start, boolean[] states, RecordSettings recordSettings, Runnable stopCallback) {
        actions = actionsList;
        startTime = start;
        keyStates = states;
        active = true;
        lastMousePos = null;
        firstMousePos = null;
        lastMouseMoveTime = 0;
        settings = recordSettings;
        stopRecordingCallback = stopCallback;
    }

    public static boolean start() {
        if (!available || !active) {
            return false;
        }

        try {
            Class<?> globalScreenClass = Class.forName("com.github.kwhat.jnativehook.GlobalScreen");
            Object globalScreen = null;
            
            try {
                globalScreen = globalScreenClass.getMethod("getInstance").invoke(null);
            } catch (NoSuchMethodException e) {
            }
            
            boolean alreadyRegistered = false;
            try {
                try {
                    alreadyRegistered = (Boolean) globalScreenClass.getMethod("isNativeHookRegistered").invoke(null);
                } catch (NoSuchMethodException e) {
                    if (globalScreen != null) {
                        alreadyRegistered = (Boolean) globalScreen.getClass().getMethod("isNativeHookRegistered").invoke(globalScreen);
                    }
                }
            } catch (Exception e) {
            }
            
            if (alreadyRegistered) {
                stop();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                active = true;
                
                try {
                    globalScreen = globalScreenClass.getMethod("getInstance").invoke(null);
                } catch (NoSuchMethodException e) {
                }
            }
            
            boolean registered = false;
            try {
                globalScreenClass.getMethod("registerNativeHook").invoke(null);
                registered = true;
            } catch (NoSuchMethodException e) {
                try {
                    if (globalScreen == null) {
                        globalScreen = globalScreenClass.getMethod("getInstance").invoke(null);
                    }
                    globalScreen.getClass().getMethod("registerNativeHook").invoke(globalScreen);
                    registered = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("already registered")) {
                    registered = true;
                } else {
                    e.printStackTrace();
                    return false;
                }
            }

            if (!registered) {
                return false;
            }
            
            keyListener = createKeyListener();
            if (keyListener == null) {
                return false;
            }
            
            Class<?> keyListenerClass = Class.forName("com.github.kwhat.jnativehook.keyboard.NativeKeyListener");
            
            try {
                globalScreenClass.getMethod("addNativeKeyListener", keyListenerClass).invoke(null, keyListener);
            } catch (NoSuchMethodException e) {
                try {
                    if (globalScreen == null) {
                        globalScreen = globalScreenClass.getMethod("getInstance").invoke(null);
                    }
                    globalScreen.getClass().getMethod("addNativeKeyListener", keyListenerClass).invoke(globalScreen, keyListener);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            
            mouseListener = createMouseListener();
            if (mouseListener == null) {
                return false;
            }
            
            Class<?> mouseListenerClass = Class.forName("com.github.kwhat.jnativehook.mouse.NativeMouseListener");
            
            try {
                globalScreenClass.getMethod("addNativeMouseListener", mouseListenerClass).invoke(null, mouseListener);
            } catch (NoSuchMethodException e) {
                try {
                    if (globalScreen == null) {
                        globalScreen = globalScreenClass.getMethod("getInstance").invoke(null);
                    }
                    globalScreen.getClass().getMethod("addNativeMouseListener", mouseListenerClass).invoke(globalScreen, mouseListener);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            mouseMotionListener = createMouseMotionListener();
            if (mouseMotionListener == null) {
                return false;
            }
            
            Class<?> mouseMotionClass = Class.forName("com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener");
            
            try {
                globalScreenClass.getMethod("addNativeMouseMotionListener", mouseMotionClass).invoke(null, mouseMotionListener);
            } catch (NoSuchMethodException e) {
                try {
                    if (globalScreen == null) {
                        globalScreen = globalScreenClass.getMethod("getInstance").invoke(null);
                    }
                    globalScreen.getClass().getMethod("addNativeMouseMotionListener", mouseMotionClass).invoke(globalScreen, mouseMotionListener);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void stop() {
        if (!available) {
            return;
        }

        try {
            Class<?> globalScreenClass = Class.forName("com.github.kwhat.jnativehook.GlobalScreen");
            Object globalScreen = null;
            
            try {
                globalScreen = globalScreenClass.getMethod("getInstance").invoke(null);
            } catch (NoSuchMethodException e) {
            }
            
            boolean hookRegistered = false;
            try {
                try {
                    hookRegistered = (Boolean) globalScreenClass.getMethod("isNativeHookRegistered").invoke(null);
                } catch (NoSuchMethodException e) {
                    if (globalScreen != null) {
                        hookRegistered = (Boolean) globalScreen.getClass().getMethod("isNativeHookRegistered").invoke(globalScreen);
                    } else {
                        hookRegistered = true;
                    }
                }
            } catch (Exception e) {
                hookRegistered = true;
            }

            if (hookRegistered) {
                try {
                    Class<?> keyListenerClass = Class.forName("com.github.kwhat.jnativehook.keyboard.NativeKeyListener");
                    Class<?> mouseListenerClass = Class.forName("com.github.kwhat.jnativehook.mouse.NativeMouseListener");
                    Class<?> mouseMotionClass = Class.forName("com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener");
                    
                    if (keyListener != null) {
                        try {
                            if (globalScreen != null) {
                                globalScreen.getClass().getMethod("removeNativeKeyListener", keyListenerClass).invoke(globalScreen, keyListener);
                            } else {
                                globalScreenClass.getMethod("removeNativeKeyListener", keyListenerClass).invoke(null, keyListener);
                            }
                        } catch (Exception e) {
                        }
                    }
                    
                    if (mouseListener != null) {
                        try {
                            if (globalScreen != null) {
                                globalScreen.getClass().getMethod("removeNativeMouseListener", mouseListenerClass).invoke(globalScreen, mouseListener);
                            } else {
                                globalScreenClass.getMethod("removeNativeMouseListener", mouseListenerClass).invoke(null, mouseListener);
                            }
                        } catch (Exception e) {
                        }
                    }
                    
                    if (mouseMotionListener != null) {
                        try {
                            if (globalScreen != null) {
                                globalScreen.getClass().getMethod("removeNativeMouseMotionListener", mouseMotionClass).invoke(globalScreen, mouseMotionListener);
                            } else {
                                globalScreenClass.getMethod("removeNativeMouseMotionListener", mouseMotionClass).invoke(null, mouseMotionListener);
                            }
                        } catch (Exception e) {
                        }
                    }
                    
                    Thread.sleep(500);
                    
                    keyListener = null;
                    mouseListener = null;
                    mouseMotionListener = null;
                } catch (Exception e) {
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                active = false;

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                try {
                    boolean stillRegistered = false;
                    try {
                        try {
                            stillRegistered = (Boolean) globalScreenClass.getMethod("isNativeHookRegistered").invoke(null);
                        } catch (NoSuchMethodException e) {
                            if (globalScreen != null) {
                                stillRegistered = (Boolean) globalScreen.getClass().getMethod("isNativeHookRegistered").invoke(globalScreen);
                            }
                        }
                    } catch (Exception e) {
                    }
                    
                    if (stillRegistered) {
                        if (globalScreen != null) {
                            globalScreen.getClass().getMethod("unregisterNativeHook").invoke(globalScreen);
                        } else {
                            globalScreenClass.getMethod("unregisterNativeHook").invoke(null);
                        }
                        
                        Thread.sleep(500);
                    }
                } catch (NoSuchMethodException e) {
                    try {
                        boolean stillRegistered = false;
                        try {
                            if (globalScreen != null) {
                                stillRegistered = (Boolean) globalScreen.getClass().getMethod("isNativeHookRegistered").invoke(globalScreen);
                            }
                        } catch (Exception ex) {
                        }
                        
                        if (stillRegistered) {
                            if (globalScreen != null) {
                                globalScreen.getClass().getMethod("unregisterNativeHook").invoke(globalScreen);
                            } else {
                                globalScreenClass.getMethod("unregisterNativeHook").invoke(null);
                            }
                            Thread.sleep(500);
                        }
                    } catch (Exception ex) {
                    }
                } catch (Exception e) {
                    String msg = e.getMessage();
                    if (msg == null || (!msg.contains("not registered") && !msg.contains("Terminated") && !msg.contains("already unregistered"))) {
                    }
                }
            } else {
                active = false;
            }
        } catch (Exception e) {
            active = false;
        }
    }

    private static Object createKeyListener() {
        try {
            Class<?> listenerClass = Class.forName("com.github.kwhat.jnativehook.keyboard.NativeKeyListener");
            return java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class[]{listenerClass},
                (proxy, method, args) -> {
                    try {
                        if (!active || args == null || args.length == 0) {
                            return null;
                        }
                        
                        if (!settings.isRecordKeyboard()) {
                            return null;
                        }
                        
                        String methodName = method.getName();
                        Object event = args[0];

                        if (methodName.equals("nativeKeyPressed")) {
                            int nativeKeyCode = getKeyCode(event);
                            int rawCode = getRawCode(event);
                            long timestamp = System.currentTimeMillis() - startTime;
                            
                            int modifiers = 0;
                            if (keyStates != null) {
                                if (keyStates.length > java.awt.event.KeyEvent.VK_CONTROL && 
                                    keyStates[java.awt.event.KeyEvent.VK_CONTROL]) {
                                    modifiers |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
                                }
                                if (keyStates.length > java.awt.event.KeyEvent.VK_ALT && 
                                    keyStates[java.awt.event.KeyEvent.VK_ALT]) {
                                    modifiers |= java.awt.event.InputEvent.ALT_DOWN_MASK;
                                }
                                if (keyStates.length > java.awt.event.KeyEvent.VK_SHIFT && 
                                    keyStates[java.awt.event.KeyEvent.VK_SHIFT]) {
                                    modifiers |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;
                                }
                                if (keyStates.length > 524 && keyStates[524]) {
                                    modifiers |= java.awt.event.InputEvent.META_DOWN_MASK;
                                }
                            }
                            
                            int eventModifiersFromEvent = getModifiers(event);
                            if (eventModifiersFromEvent != 0) {
                                modifiers |= eventModifiersFromEvent;
                            }

                            boolean isF11 = false;
                            try {
                                Class<?> nativeKeyEventClass = Class.forName("com.github.kwhat.jnativehook.keyboard.NativeKeyEvent");
                                java.lang.reflect.Field[] fields = nativeKeyEventClass.getFields();
                                for (java.lang.reflect.Field field : fields) {
                                    if (field.getName().equals("VC_F11") && field.getType() == int.class) {
                                        int vcF11 = field.getInt(null);
                                        if (nativeKeyCode == vcF11) {
                                            isF11 = true;
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                            }
                            
                            if (!isF11 && rawCode == 87) {
                                isF11 = true;
                            }
                            
                            if (isF11) {
                                boolean hasShift = (modifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0;
                                
                                if (!hasShift && (rawCode & 0x10) != 0) {
                                    hasShift = true;
                                }
                                
                                if (!hasShift && keyStates != null) {
                                    int shiftVK = java.awt.event.KeyEvent.VK_SHIFT;
                                    if (shiftVK < keyStates.length && keyStates[shiftVK]) {
                                        hasShift = true;
                                    }
                                }

                                if (hasShift) {
                                    if (stopRecordingCallback != null) {
                                        active = false;
                                        SwingUtilities.invokeLater(() -> {
                                            try {
                                                stopRecordingCallback.run();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        });
                                    }
                                    return null;
                                }
                            }

                            int keyCode = convertNativeKeyCodeToJava(nativeKeyCode, rawCode, modifiers, event);

                            if (keyCode <= 0 || keyCode >= keyStates.length) {
                                return null;
                            }

                            if (isModifierKey(keyCode)) {
                                keyStates[keyCode] = true;
                                return null;
                            }

                            if (!keyStates[keyCode]) {
                                int finalModifiers = 0;
                                if (keyStates != null) {
                                    if (keyStates.length > java.awt.event.KeyEvent.VK_CONTROL && 
                                        keyStates[java.awt.event.KeyEvent.VK_CONTROL]) {
                                        finalModifiers |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
                                    }
                                    if (keyStates.length > java.awt.event.KeyEvent.VK_ALT && 
                                        keyStates[java.awt.event.KeyEvent.VK_ALT]) {
                                        finalModifiers |= java.awt.event.InputEvent.ALT_DOWN_MASK;
                                    }
                                    if (keyStates.length > java.awt.event.KeyEvent.VK_SHIFT && 
                                        keyStates[java.awt.event.KeyEvent.VK_SHIFT]) {
                                        finalModifiers |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;
                                    }
                                    if (keyStates.length > 524 && keyStates[524]) {
                                        finalModifiers |= java.awt.event.InputEvent.META_DOWN_MASK;
                                    }
                                }
                                
                                int eventModsFromEvent = getModifiers(event);
                                if (eventModsFromEvent != 0) {
                                    finalModifiers |= eventModsFromEvent;
                                }
                                
                                Action action = new Action(Action.ActionType.KEY_PRESS, timestamp);
                                action.setKeyCode(keyCode);
                                action.setModifiers(finalModifiers);
                                if (actions != null) {
                                    actions.add(action);
                                }
                                keyStates[keyCode] = true;
                            }
                        } else if (methodName.equals("nativeKeyReleased")) {
                            int nativeKeyCode = getKeyCode(event);
                            int rawCode = getRawCode(event);
                            long timestamp = System.currentTimeMillis() - startTime;
                            
                            int modifiers = 0;
                            if (keyStates != null) {
                                if (keyStates.length > java.awt.event.KeyEvent.VK_CONTROL && 
                                    keyStates[java.awt.event.KeyEvent.VK_CONTROL]) {
                                    modifiers |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
                                }
                                if (keyStates.length > java.awt.event.KeyEvent.VK_ALT && 
                                    keyStates[java.awt.event.KeyEvent.VK_ALT]) {
                                    modifiers |= java.awt.event.InputEvent.ALT_DOWN_MASK;
                                }
                                if (keyStates.length > java.awt.event.KeyEvent.VK_SHIFT && 
                                    keyStates[java.awt.event.KeyEvent.VK_SHIFT]) {
                                    modifiers |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;
                                }
                                if (keyStates.length > 524 && keyStates[524]) {
                                    modifiers |= java.awt.event.InputEvent.META_DOWN_MASK;
                                }
                            }
                            
                            int eventModifiers = getModifiers(event);
                            if (eventModifiers != 0) {
                                modifiers |= eventModifiers;
                            }

                            int keyCode = convertNativeKeyCodeToJava(nativeKeyCode, rawCode, modifiers, event);

                            if (keyCode <= 0 || keyCode >= keyStates.length) {
                                return null;
                            }

                            if (keyStates[keyCode]) {
                                long pressTime = 0;
                                if (settings.isInsertLongPresses()) {
                                    for (int i = actions.size() - 1; i >= 0; i--) {
                                        Action a = actions.get(i);
                                        if (a.getType() == Action.ActionType.KEY_PRESS && a.getKeyCode() == keyCode) {
                                            pressTime = timestamp - a.getTimestamp();
                                            break;
                                        }
                                    }
                                    
                                    if (pressTime > 500) {
                                        Action delayAction = new Action(Action.ActionType.DELAY, timestamp - pressTime + 500);
                                        delayAction.setDelay(pressTime - 500);
                                        if (actions != null) {
                                            actions.add(delayAction);
                                        }
                                    }
                                }
                                
                                if (isModifierKey(keyCode)) {
                                    keyStates[keyCode] = false;
                                    return null;
                                }
                                
                                Action action = new Action(Action.ActionType.KEY_RELEASE, timestamp);
                                action.setKeyCode(keyCode);
                                
                                int finalModifiers = modifiers;
                                if (keyStates != null) {
                                    if (keyStates.length > java.awt.event.KeyEvent.VK_CONTROL && 
                                        keyStates[java.awt.event.KeyEvent.VK_CONTROL]) {
                                        finalModifiers |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
                                    }
                                    if (keyStates.length > java.awt.event.KeyEvent.VK_ALT && 
                                        keyStates[java.awt.event.KeyEvent.VK_ALT]) {
                                        finalModifiers |= java.awt.event.InputEvent.ALT_DOWN_MASK;
                                    }
                                    if (keyStates.length > java.awt.event.KeyEvent.VK_SHIFT && 
                                        keyStates[java.awt.event.KeyEvent.VK_SHIFT]) {
                                        finalModifiers |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;
                                    }
                                    if (keyStates.length > 524 && keyStates[524]) {
                                        finalModifiers |= java.awt.event.InputEvent.META_DOWN_MASK;
                                    }
                                }
                                
                                action.setModifiers(finalModifiers);
                                if (actions != null) {
                                    actions.add(action);
                                }
                                keyStates[keyCode] = false;
                            }
                        }
                    } catch (java.util.concurrent.RejectedExecutionException e) {
                    } catch (Exception e) {
                    }
                    return null;
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Object createMouseListener() {
        try {
            Class<?> listenerClass = Class.forName("com.github.kwhat.jnativehook.mouse.NativeMouseListener");
            return java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class[]{listenerClass},
                (proxy, method, args) -> {
                    try {
                        if (!active || args == null || args.length == 0) {
                            return null;
                        }
                        
                        if (!settings.isRecordMouseButtons()) {
                            return null;
                        }
                        
                        String methodName = method.getName();
                        Object event = args[0];

                        int button = getButton(event);
                        int x = getX(event);
                        int y = getY(event);
                        long timestamp = System.currentTimeMillis() - startTime;

                        if (methodName.equals("nativeMousePressed")) {
                            Action action = new Action(Action.ActionType.MOUSE_PRESS, timestamp);
                            action.setX(x);
                            action.setY(y);
                            action.setButton(buttonToMask(button));
                            if (actions != null) {
                                actions.add(action);
                            }
                        } else if (methodName.equals("nativeMouseReleased")) {
                            long pressTime = 0;
                            if (settings.isInsertLongPresses()) {
                                for (int i = actions.size() - 1; i >= 0; i--) {
                                    Action a = actions.get(i);
                                    if (a.getType() == Action.ActionType.MOUSE_PRESS && a.getButton() == buttonToMask(button)) {
                                        pressTime = timestamp - a.getTimestamp();
                                        break;
                                    }
                                }
                                
                                if (pressTime > 500) {
                                    Action delayAction = new Action(Action.ActionType.DELAY, timestamp - pressTime + 500);
                                    delayAction.setDelay(pressTime - 500);
                                    if (actions != null) {
                                        actions.add(delayAction);
                                    }
                                }
                            }
                            
                            Action action = new Action(Action.ActionType.MOUSE_RELEASE, timestamp);
                            action.setX(x);
                            action.setY(y);
                            action.setButton(buttonToMask(button));
                            if (actions != null) {
                                actions.add(action);
                            }
                        }
                    } catch (java.util.concurrent.RejectedExecutionException e) {
                    } catch (Exception e) {
                    }
                    return null;
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Object createMouseMotionListener() {
        try {
            Class<?> listenerClass = Class.forName("com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener");
            return java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class[]{listenerClass},
                (proxy, method, args) -> {
                    try {
                        if (!active || args == null || args.length == 0) {
                            return null;
                        }
                        
                        if (method.getName().equals("nativeMouseMoved")) {
                            if (!settings.isRecordAbsoluteMouseMovement() && !settings.isRecordRelativeMouseMovement()) {
                                return null;
                            }
                            
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastMouseMoveTime < MOUSE_MOVE_INTERVAL) {
                                return null;
                            }

                            Object event = args[0];
                            int x = getX(event);
                            int y = getY(event);

                            if (firstMousePos == null) {
                                firstMousePos = new Point(x, y);
                            }

                            if (lastMousePos == null || x != lastMousePos.x || y != lastMousePos.y) {
                                long timestamp = currentTime - startTime;
                                
                                if (settings.isRecordAbsoluteMouseMovement()) {
                                    Action moveAction = new Action(Action.ActionType.MOUSE_MOVE, timestamp);
                                    moveAction.setX(x);
                                    moveAction.setY(y);
                                    if (actions != null) {
                                        actions.add(moveAction);
                                    }
                                }
                                
                                if (settings.isRecordRelativeMouseMovement() && lastMousePos != null) {
                                    int dx = x - lastMousePos.x;
                                    int dy = y - lastMousePos.y;
                                    Action moveAction = new Action(Action.ActionType.MOUSE_MOVE, timestamp);
                                    moveAction.setX(dx);
                                    moveAction.setY(dy);
                                    if (actions != null) {
                                        actions.add(moveAction);
                                    }
                                }
                                
                                lastMousePos = new Point(x, y);
                                lastMouseMoveTime = currentTime;
                            }
                        }
                    } catch (java.util.concurrent.RejectedExecutionException e) {
                    } catch (Exception e) {
                    }
                    return null;
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int getKeyCode(Object event) {
        try {
            return (Integer) event.getClass().getMethod("getKeyCode").invoke(event);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getRawCode(Object event) {
        try {
            return (Integer) event.getClass().getMethod("getRawCode").invoke(event);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static char getKeyChar(Object event) {
        try {
            Object keyChar = event.getClass().getMethod("getKeyChar").invoke(event);
            if (keyChar instanceof Character) {
                return ((Character) keyChar).charValue();
            }
            if (keyChar instanceof Integer) {
                return (char) ((Integer) keyChar).intValue();
            }
        } catch (Exception e) {
        }
        return 0;
    }
    
    private static String getKeyTextFromNativeEvent(Object event, int keyCode) {
        try {
            Class<?> nativeKeyEventClass = Class.forName("com.github.kwhat.jnativehook.keyboard.NativeKeyEvent");
            try {
                java.lang.reflect.Method getKeyTextMethod = nativeKeyEventClass.getMethod("getKeyText", int.class);
                Object result = getKeyTextMethod.invoke(null, keyCode);
                if (result instanceof String) {
                    return (String) result;
                }
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static boolean isModifierKey(int keyCode) {
        return keyCode == java.awt.event.KeyEvent.VK_CONTROL || 
               keyCode == java.awt.event.KeyEvent.VK_ALT || 
               keyCode == java.awt.event.KeyEvent.VK_SHIFT || 
               keyCode == java.awt.event.KeyEvent.VK_META || 
               keyCode == 524;
    }

    private static boolean isNoConvertKey(int nativeKeyCode, int rawCode) {
        try {
            Class<?> nativeKeyEventClass = Class.forName("com.github.kwhat.jnativehook.keyboard.NativeKeyEvent");
            
            java.lang.reflect.Field[] fields = nativeKeyEventClass.getFields();
            for (java.lang.reflect.Field field : fields) {
                String fieldName = field.getName();
                if ((fieldName.equals("VC_NO_CONVERT") || 
                     fieldName.equals("VC_UNDEFINED") ||
                     fieldName.contains("NO_CONVERT") ||
                     fieldName.contains("UNDEFINED")) && 
                    field.getType() == int.class) {
                    try {
                        int vcValue = field.getInt(null);
                        if (vcValue == nativeKeyCode || vcValue == rawCode) {
                            return true;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
        
        return false;
    }

    private static int convertNativeKeyCodeToJava(int nativeKeyCode, int rawCode, int modifiers, Object event) {
        if (nativeKeyCode <= 0 && rawCode <= 0) {
            return 0;
        }
        
        try {
            Class<?> nativeKeyEventClass = Class.forName("com.github.kwhat.jnativehook.keyboard.NativeKeyEvent");
            
            String vcName = null;
            java.lang.reflect.Field[] fields = nativeKeyEventClass.getFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getName().startsWith("VC_") && field.getType() == int.class) {
                    try {
                        int vcValue = field.getInt(null);
                        if (vcValue == nativeKeyCode) {
                            vcName = field.getName();
                            
                            if (vcName.contains("NO_CONVERT") || vcName.contains("UNDEFINED")) {
                                int vkFromRawForVC = rawCodeToVK(rawCode);
                                if (vkFromRawForVC > 0) {
                                    return vkFromRawForVC;
                                }
                                if ((modifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
                                    return java.awt.event.KeyEvent.VK_CONTROL;
                                }
                                continue;
                            }
                            
                            if (vcName.equals("VC_ENTER")) {
                                return java.awt.event.KeyEvent.VK_ENTER;
                            }
                            
                            int vkCode = mapVCToVK(vcName);
                            if (vkCode > 0) {
                                return vkCode;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
        
        int vkFromRaw = rawCodeToVK(rawCode);
        if (vkFromRaw > 0) {
            return vkFromRaw;
        }
        
        if (event != null) {
            char keyChar = getKeyChar(event);
            if (keyChar > 0 && keyChar != 0xFFFF) {
                if (keyChar == '\n' || keyChar == '\r') {
                    return java.awt.event.KeyEvent.VK_ENTER;
                }
                int vkFromChar = charToVK(keyChar, modifiers);
                if (vkFromChar > 0) {
                    return vkFromChar;
                }
            }
        }
        
        int codeToConvert = rawCode > 0 ? rawCode : nativeKeyCode;
        
        if (codeToConvert <= 0) {
            return 0;
        }
        
        if (codeToConvert >= 97 && codeToConvert <= 122) {
            return codeToConvert - 32;
        }
        
        if (codeToConvert >= 65 && codeToConvert <= 90) {
            return codeToConvert;
        }
        
        if (codeToConvert >= 48 && codeToConvert <= 57) {
            return codeToConvert;
        }
        
        switch (codeToConvert) {
            case 59: return java.awt.event.KeyEvent.VK_SEMICOLON;
            case 61: return java.awt.event.KeyEvent.VK_EQUALS;
            case 44: return java.awt.event.KeyEvent.VK_COMMA;
            case 45: return java.awt.event.KeyEvent.VK_MINUS;
            case 46: return java.awt.event.KeyEvent.VK_PERIOD;
            case 47: return java.awt.event.KeyEvent.VK_SLASH;
            case 91: return java.awt.event.KeyEvent.VK_OPEN_BRACKET;
            case 92: return java.awt.event.KeyEvent.VK_BACK_SLASH;
            case 93: return java.awt.event.KeyEvent.VK_CLOSE_BRACKET;
            case 39: return java.awt.event.KeyEvent.VK_QUOTE;
            case 96: return java.awt.event.KeyEvent.VK_BACK_QUOTE;
            case 32: return java.awt.event.KeyEvent.VK_SPACE;
            case 13: return java.awt.event.KeyEvent.VK_ENTER;
            case 8: return java.awt.event.KeyEvent.VK_BACK_SPACE;
            case 9: return java.awt.event.KeyEvent.VK_TAB;
            case 27: return java.awt.event.KeyEvent.VK_ESCAPE;
            default:
                if (codeToConvert > 0 && codeToConvert < 1000) {
                    return codeToConvert;
                }
                return 0;
        }
    }
    
    private static int rawCodeToVK(int rawCode) {
        if (rawCode <= 0) {
            return 0;
        }
        
        java.util.Map<Integer, Integer> rawToVK = new java.util.HashMap<>();
        
        rawToVK.put(29, java.awt.event.KeyEvent.VK_CONTROL);
        rawToVK.put(42, java.awt.event.KeyEvent.VK_SHIFT);
        rawToVK.put(54, java.awt.event.KeyEvent.VK_SHIFT);
        rawToVK.put(56, java.awt.event.KeyEvent.VK_ALT);
        rawToVK.put(58, java.awt.event.KeyEvent.VK_ALT);
        rawToVK.put(91, 524);
        rawToVK.put(92, 524);
        
        rawToVK.put(6, java.awt.event.KeyEvent.VK_C);
        rawToVK.put(46, java.awt.event.KeyEvent.VK_C);
        rawToVK.put(47, java.awt.event.KeyEvent.VK_V);
        rawToVK.put(45, java.awt.event.KeyEvent.VK_X);
        rawToVK.put(30, java.awt.event.KeyEvent.VK_A);
        rawToVK.put(44, java.awt.event.KeyEvent.VK_Z);
        rawToVK.put(21, java.awt.event.KeyEvent.VK_Y);
        rawToVK.put(48, java.awt.event.KeyEvent.VK_B);
        rawToVK.put(32, java.awt.event.KeyEvent.VK_D);
        rawToVK.put(18, java.awt.event.KeyEvent.VK_E);
        rawToVK.put(33, java.awt.event.KeyEvent.VK_F);
        rawToVK.put(34, java.awt.event.KeyEvent.VK_G);
        rawToVK.put(35, java.awt.event.KeyEvent.VK_H);
        rawToVK.put(23, java.awt.event.KeyEvent.VK_I);
        rawToVK.put(36, java.awt.event.KeyEvent.VK_J);
        rawToVK.put(37, java.awt.event.KeyEvent.VK_K);
        rawToVK.put(38, java.awt.event.KeyEvent.VK_L);
        rawToVK.put(50, java.awt.event.KeyEvent.VK_M);
        rawToVK.put(49, java.awt.event.KeyEvent.VK_N);
        rawToVK.put(24, java.awt.event.KeyEvent.VK_O);
        rawToVK.put(25, java.awt.event.KeyEvent.VK_P);
        rawToVK.put(16, java.awt.event.KeyEvent.VK_Q);
        rawToVK.put(19, java.awt.event.KeyEvent.VK_R);
        rawToVK.put(31, java.awt.event.KeyEvent.VK_S);
        rawToVK.put(20, java.awt.event.KeyEvent.VK_T);
        rawToVK.put(22, java.awt.event.KeyEvent.VK_U);
        rawToVK.put(17, java.awt.event.KeyEvent.VK_W);
        
        rawToVK.put(2, java.awt.event.KeyEvent.VK_1);
        rawToVK.put(3, java.awt.event.KeyEvent.VK_2);
        rawToVK.put(4, java.awt.event.KeyEvent.VK_3);
        rawToVK.put(5, java.awt.event.KeyEvent.VK_4);
        rawToVK.put(7, java.awt.event.KeyEvent.VK_5);
        rawToVK.put(8, java.awt.event.KeyEvent.VK_6);
        rawToVK.put(9, java.awt.event.KeyEvent.VK_7);
        rawToVK.put(10, java.awt.event.KeyEvent.VK_8);
        rawToVK.put(11, java.awt.event.KeyEvent.VK_9);
        rawToVK.put(12, java.awt.event.KeyEvent.VK_0);
        
        rawToVK.put(72, java.awt.event.KeyEvent.VK_UP);
        rawToVK.put(80, java.awt.event.KeyEvent.VK_DOWN);
        rawToVK.put(75, java.awt.event.KeyEvent.VK_LEFT);
        rawToVK.put(77, java.awt.event.KeyEvent.VK_RIGHT);
        
        rawToVK.put(82, java.awt.event.KeyEvent.VK_INSERT);
        rawToVK.put(83, java.awt.event.KeyEvent.VK_DELETE);
        rawToVK.put(71, java.awt.event.KeyEvent.VK_HOME);
        rawToVK.put(79, java.awt.event.KeyEvent.VK_END);
        rawToVK.put(73, java.awt.event.KeyEvent.VK_PAGE_UP);
        rawToVK.put(81, java.awt.event.KeyEvent.VK_PAGE_DOWN);
        
        rawToVK.put(55, java.awt.event.KeyEvent.VK_MULTIPLY);
        rawToVK.put(74, java.awt.event.KeyEvent.VK_SUBTRACT);
        rawToVK.put(78, java.awt.event.KeyEvent.VK_ADD);
        
        rawToVK.put(53, java.awt.event.KeyEvent.VK_DIVIDE);
        rawToVK.put(76, java.awt.event.KeyEvent.VK_NUMPAD5);
        
        rawToVK.put(52, java.awt.event.KeyEvent.VK_PERIOD);
        rawToVK.put(51, java.awt.event.KeyEvent.VK_COMMA);
        rawToVK.put(53, java.awt.event.KeyEvent.VK_SLASH);
        rawToVK.put(39, java.awt.event.KeyEvent.VK_SEMICOLON);
        rawToVK.put(12, java.awt.event.KeyEvent.VK_MINUS);
        rawToVK.put(26, java.awt.event.KeyEvent.VK_OPEN_BRACKET);
        rawToVK.put(27, java.awt.event.KeyEvent.VK_CLOSE_BRACKET);
        rawToVK.put(43, java.awt.event.KeyEvent.VK_BACK_SLASH);
        rawToVK.put(40, java.awt.event.KeyEvent.VK_QUOTE);
        rawToVK.put(41, java.awt.event.KeyEvent.VK_BACK_QUOTE);
        rawToVK.put(57, java.awt.event.KeyEvent.VK_SPACE);
        rawToVK.put(28, java.awt.event.KeyEvent.VK_ENTER);
        rawToVK.put(96, java.awt.event.KeyEvent.VK_ENTER);
        rawToVK.put(14, java.awt.event.KeyEvent.VK_BACK_SPACE);
        rawToVK.put(15, java.awt.event.KeyEvent.VK_TAB);
        rawToVK.put(1, java.awt.event.KeyEvent.VK_ESCAPE);
        
        rawToVK.put(59, java.awt.event.KeyEvent.VK_F1);
        rawToVK.put(60, java.awt.event.KeyEvent.VK_F2);
        rawToVK.put(61, java.awt.event.KeyEvent.VK_F3);
        rawToVK.put(62, java.awt.event.KeyEvent.VK_F4);
        rawToVK.put(63, java.awt.event.KeyEvent.VK_F5);
        rawToVK.put(64, java.awt.event.KeyEvent.VK_F6);
        rawToVK.put(65, java.awt.event.KeyEvent.VK_F7);
        rawToVK.put(66, java.awt.event.KeyEvent.VK_F8);
        rawToVK.put(67, java.awt.event.KeyEvent.VK_F9);
        rawToVK.put(68, java.awt.event.KeyEvent.VK_F10);
        rawToVK.put(87, java.awt.event.KeyEvent.VK_F11);
        rawToVK.put(88, java.awt.event.KeyEvent.VK_F12);
        
        if (rawToVK.containsKey(rawCode)) {
            return rawToVK.get(rawCode);
        }
        
        return 0;
    }
    
    private static int charToVK(char keyChar, int modifiers) {
        if (keyChar >= 'A' && keyChar <= 'Z') {
            return java.awt.event.KeyEvent.VK_A + (keyChar - 'A');
        }
        if (keyChar >= 'a' && keyChar <= 'z') {
            return java.awt.event.KeyEvent.VK_A + (keyChar - 'a');
        }
        if (keyChar >= '0' && keyChar <= '9') {
            return java.awt.event.KeyEvent.VK_0 + (keyChar - '0');
        }
        
        switch (keyChar) {
            case ' ': return java.awt.event.KeyEvent.VK_SPACE;
            case '\n': case '\r': return java.awt.event.KeyEvent.VK_ENTER;
            case '\b': return java.awt.event.KeyEvent.VK_BACK_SPACE;
            case '\t': return java.awt.event.KeyEvent.VK_TAB;
            case 27: return java.awt.event.KeyEvent.VK_ESCAPE;
            case '.': return java.awt.event.KeyEvent.VK_PERIOD;
            case ',': return java.awt.event.KeyEvent.VK_COMMA;
            case '/': return java.awt.event.KeyEvent.VK_SLASH;
            case ';': return java.awt.event.KeyEvent.VK_SEMICOLON;
            case '=': return java.awt.event.KeyEvent.VK_EQUALS;
            case '-': return java.awt.event.KeyEvent.VK_MINUS;
            case '[': return java.awt.event.KeyEvent.VK_OPEN_BRACKET;
            case ']': return java.awt.event.KeyEvent.VK_CLOSE_BRACKET;
            case '\\': return java.awt.event.KeyEvent.VK_BACK_SLASH;
            case '\'': return java.awt.event.KeyEvent.VK_QUOTE;
            case '`': return java.awt.event.KeyEvent.VK_BACK_QUOTE;
        }
        
        return 0;
    }
    
    private static int mapVCToVK(String vcName) {
        if (vcName == null) return 0;
        
        if (vcName.contains("NO_CONVERT") || vcName.contains("UNDEFINED")) {
            return 0;
        }
        
        if (vcName.equals("VC_C")) return java.awt.event.KeyEvent.VK_C;
        if (vcName.equals("VC_V")) return java.awt.event.KeyEvent.VK_V;
        if (vcName.equals("VC_X")) return java.awt.event.KeyEvent.VK_X;
        if (vcName.equals("VC_A")) return java.awt.event.KeyEvent.VK_A;
        if (vcName.equals("VC_Z")) return java.awt.event.KeyEvent.VK_Z;
        if (vcName.equals("VC_Y")) return java.awt.event.KeyEvent.VK_Y;
        if (vcName.equals("VC_B")) return java.awt.event.KeyEvent.VK_B;
        if (vcName.equals("VC_D")) return java.awt.event.KeyEvent.VK_D;
        if (vcName.equals("VC_E")) return java.awt.event.KeyEvent.VK_E;
        if (vcName.equals("VC_F")) return java.awt.event.KeyEvent.VK_F;
        if (vcName.equals("VC_G")) return java.awt.event.KeyEvent.VK_G;
        if (vcName.equals("VC_H")) return java.awt.event.KeyEvent.VK_H;
        if (vcName.equals("VC_I")) return java.awt.event.KeyEvent.VK_I;
        if (vcName.equals("VC_J")) return java.awt.event.KeyEvent.VK_J;
        if (vcName.equals("VC_K")) return java.awt.event.KeyEvent.VK_K;
        if (vcName.equals("VC_L")) return java.awt.event.KeyEvent.VK_L;
        if (vcName.equals("VC_M")) return java.awt.event.KeyEvent.VK_M;
        if (vcName.equals("VC_N")) return java.awt.event.KeyEvent.VK_N;
        if (vcName.equals("VC_O")) return java.awt.event.KeyEvent.VK_O;
        if (vcName.equals("VC_P")) return java.awt.event.KeyEvent.VK_P;
        if (vcName.equals("VC_Q")) return java.awt.event.KeyEvent.VK_Q;
        if (vcName.equals("VC_R")) return java.awt.event.KeyEvent.VK_R;
        if (vcName.equals("VC_S")) return java.awt.event.KeyEvent.VK_S;
        if (vcName.equals("VC_T")) return java.awt.event.KeyEvent.VK_T;
        if (vcName.equals("VC_U")) return java.awt.event.KeyEvent.VK_U;
        if (vcName.equals("VC_W")) return java.awt.event.KeyEvent.VK_W;
        
        if (vcName.equals("VC_PERIOD") || vcName.equals("VC_DOT")) return java.awt.event.KeyEvent.VK_PERIOD;
        if (vcName.equals("VC_COMMA")) return java.awt.event.KeyEvent.VK_COMMA;
        if (vcName.equals("VC_SLASH")) return java.awt.event.KeyEvent.VK_SLASH;
        if (vcName.equals("VC_SEMICOLON")) return java.awt.event.KeyEvent.VK_SEMICOLON;
        if (vcName.equals("VC_EQUALS")) return java.awt.event.KeyEvent.VK_EQUALS;
        if (vcName.equals("VC_MINUS")) return java.awt.event.KeyEvent.VK_MINUS;
        if (vcName.equals("VC_OPEN_BRACKET")) return java.awt.event.KeyEvent.VK_OPEN_BRACKET;
        if (vcName.equals("VC_CLOSE_BRACKET")) return java.awt.event.KeyEvent.VK_CLOSE_BRACKET;
        if (vcName.equals("VC_BACK_SLASH")) return java.awt.event.KeyEvent.VK_BACK_SLASH;
        if (vcName.equals("VC_QUOTE")) return java.awt.event.KeyEvent.VK_QUOTE;
        if (vcName.equals("VC_BACK_QUOTE")) return java.awt.event.KeyEvent.VK_BACK_QUOTE;
        if (vcName.equals("VC_SPACE")) return java.awt.event.KeyEvent.VK_SPACE;
        if (vcName.equals("VC_ENTER")) return java.awt.event.KeyEvent.VK_ENTER;
        if (vcName.equals("VC_BACKSPACE") || vcName.equals("VC_BACK_SPACE")) return java.awt.event.KeyEvent.VK_BACK_SPACE;
        if (vcName.equals("VC_TAB")) return java.awt.event.KeyEvent.VK_TAB;
        if (vcName.equals("VC_ESCAPE")) return java.awt.event.KeyEvent.VK_ESCAPE;
        if (vcName.equals("VC_F1")) return java.awt.event.KeyEvent.VK_F1;
        if (vcName.equals("VC_F2")) return java.awt.event.KeyEvent.VK_F2;
        if (vcName.equals("VC_F3")) return java.awt.event.KeyEvent.VK_F3;
        if (vcName.equals("VC_F4")) return java.awt.event.KeyEvent.VK_F4;
        if (vcName.equals("VC_F5")) return java.awt.event.KeyEvent.VK_F5;
        if (vcName.equals("VC_F6")) return java.awt.event.KeyEvent.VK_F6;
        if (vcName.equals("VC_F7")) return java.awt.event.KeyEvent.VK_F7;
        if (vcName.equals("VC_F8")) return java.awt.event.KeyEvent.VK_F8;
        if (vcName.equals("VC_F9")) return java.awt.event.KeyEvent.VK_F9;
        if (vcName.equals("VC_F10")) return java.awt.event.KeyEvent.VK_F10;
        if (vcName.equals("VC_F11")) return java.awt.event.KeyEvent.VK_F11;
        if (vcName.equals("VC_F12")) return java.awt.event.KeyEvent.VK_F12;
        
        if (vcName.equals("VC_UP")) return java.awt.event.KeyEvent.VK_UP;
        if (vcName.equals("VC_DOWN")) return java.awt.event.KeyEvent.VK_DOWN;
        if (vcName.equals("VC_LEFT")) return java.awt.event.KeyEvent.VK_LEFT;
        if (vcName.equals("VC_RIGHT")) return java.awt.event.KeyEvent.VK_RIGHT;
        
        if (vcName.equals("VC_INSERT")) return java.awt.event.KeyEvent.VK_INSERT;
        if (vcName.equals("VC_DELETE")) return java.awt.event.KeyEvent.VK_DELETE;
        if (vcName.equals("VC_HOME")) return java.awt.event.KeyEvent.VK_HOME;
        if (vcName.equals("VC_END")) return java.awt.event.KeyEvent.VK_END;
        if (vcName.equals("VC_PAGE_UP")) return java.awt.event.KeyEvent.VK_PAGE_UP;
        if (vcName.equals("VC_PAGE_DOWN")) return java.awt.event.KeyEvent.VK_PAGE_DOWN;
        
        if (vcName.equals("VC_CONTROL") || vcName.equals("VC_CONTROL_L") || vcName.equals("VC_CONTROL_R")) return java.awt.event.KeyEvent.VK_CONTROL;
        if (vcName.equals("VC_ALT") || vcName.equals("VC_ALT_L") || vcName.equals("VC_ALT_R")) return java.awt.event.KeyEvent.VK_ALT;
        if (vcName.equals("VC_SHIFT") || vcName.equals("VC_SHIFT_L") || vcName.equals("VC_SHIFT_R")) return java.awt.event.KeyEvent.VK_SHIFT;
        if (vcName.equals("VC_META") || vcName.equals("VC_META_L") || vcName.equals("VC_META_R")) return java.awt.event.KeyEvent.VK_META;
        
        for (char c = 'A'; c <= 'Z'; c++) {
            if (vcName.equals("VC_" + c)) {
                return java.awt.event.KeyEvent.VK_A + (c - 'A');
            }
        }
        
        for (char c = '0'; c <= '9'; c++) {
            if (vcName.equals("VC_" + c)) {
                return java.awt.event.KeyEvent.VK_0 + (c - '0');
            }
        }
        
        return 0;
    }

    private static int getModifiers(Object event) {
        try {
            int modifiers = 0;
            
            try {
                Object modifiersObj = event.getClass().getMethod("getModifiers").invoke(event);
                if (modifiersObj instanceof Integer) {
                    modifiers = ((Integer) modifiersObj).intValue();
                }
            } catch (NoSuchMethodException e) {
            } catch (Exception e) {
            }
            
            if (modifiers == 0) {
                try {
                    Class<?> nativeKeyEventClass = Class.forName("com.github.kwhat.jnativehook.keyboard.NativeKeyEvent");
                    java.lang.reflect.Field[] fields = nativeKeyEventClass.getFields();
                    
                    int ctrlMask = 0;
                    int altMask = 0;
                    int shiftMask = 0;
                    int metaMask = 0;
                    
                    for (java.lang.reflect.Field field : fields) {
                        String fieldName = field.getName();
                        if (field.getType() == int.class) {
                            try {
                                int value = field.getInt(null);
                                if (fieldName.contains("CTRL") || fieldName.contains("CONTROL")) {
                                    if (ctrlMask == 0) ctrlMask = value;
                                } else if (fieldName.contains("ALT")) {
                                    if (altMask == 0) altMask = value;
                                } else if (fieldName.contains("SHIFT")) {
                                    if (shiftMask == 0) shiftMask = value;
                                } else if (fieldName.contains("META")) {
                                    if (metaMask == 0) metaMask = value;
                                }
                            } catch (Exception ex) {
                            }
                        }
                    }
                    
                    if ((modifiers & (ctrlMask != 0 ? ctrlMask : 1)) != 0) {
                        modifiers |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
                    }
                    if ((modifiers & (altMask != 0 ? altMask : 2)) != 0) {
                        modifiers |= java.awt.event.InputEvent.ALT_DOWN_MASK;
                    }
                    if ((modifiers & (shiftMask != 0 ? shiftMask : 4)) != 0) {
                        modifiers |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;
                    }
                    if ((modifiers & (metaMask != 0 ? metaMask : 8)) != 0) {
                        modifiers |= java.awt.event.InputEvent.META_DOWN_MASK;
                    }
                } catch (Exception e) {
                }
            }
            
            if (modifiers == 0) {
                try {
                    Object isControlDown = event.getClass().getMethod("isControlDown").invoke(event);
                    Object isAltDown = event.getClass().getMethod("isAltDown").invoke(event);
                    Object isShiftDown = event.getClass().getMethod("isShiftDown").invoke(event);
                    Object isMetaDown = event.getClass().getMethod("isMetaDown").invoke(event);
                    
                    if (isControlDown instanceof Boolean && ((Boolean) isControlDown).booleanValue()) {
                        modifiers |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
                    }
                    if (isAltDown instanceof Boolean && ((Boolean) isAltDown).booleanValue()) {
                        modifiers |= java.awt.event.InputEvent.ALT_DOWN_MASK;
                    }
                    if (isShiftDown instanceof Boolean && ((Boolean) isShiftDown).booleanValue()) {
                        modifiers |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;
                    }
                    if (isMetaDown instanceof Boolean && ((Boolean) isMetaDown).booleanValue()) {
                        modifiers |= java.awt.event.InputEvent.META_DOWN_MASK;
                    }
                } catch (Exception e) {
                }
            }
            
            if (modifiers == 0) {
                boolean ctrlDown = keyStates != null && keyStates.length > java.awt.event.KeyEvent.VK_CONTROL && 
                                   keyStates[java.awt.event.KeyEvent.VK_CONTROL];
                boolean altDown = keyStates != null && keyStates.length > java.awt.event.KeyEvent.VK_ALT && 
                                  keyStates[java.awt.event.KeyEvent.VK_ALT];
                boolean shiftDown = keyStates != null && keyStates.length > java.awt.event.KeyEvent.VK_SHIFT && 
                                    keyStates[java.awt.event.KeyEvent.VK_SHIFT];
                boolean metaDown = keyStates != null && keyStates.length > 524 && keyStates[524];
                
                if (ctrlDown) modifiers |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
                if (altDown) modifiers |= java.awt.event.InputEvent.ALT_DOWN_MASK;
                if (shiftDown) modifiers |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;
                if (metaDown) modifiers |= java.awt.event.InputEvent.META_DOWN_MASK;
            }
            
            return modifiers;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getButton(Object event) {
        try {
            return (Integer) event.getClass().getMethod("getButton").invoke(event);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getX(Object event) {
        try {
            return (Integer) event.getClass().getMethod("getX").invoke(event);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getY(Object event) {
        try {
            return (Integer) event.getClass().getMethod("getY").invoke(event);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int buttonToMask(int button) {
        if (button == 1) return java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
        if (button == 2) return java.awt.event.InputEvent.BUTTON2_DOWN_MASK;
        if (button == 3) return java.awt.event.InputEvent.BUTTON3_DOWN_MASK;
        return java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
    }
}