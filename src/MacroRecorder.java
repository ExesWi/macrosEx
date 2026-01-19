import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MacroRecorder {
    private Robot robot;
    private List<Action> actions;
    private long startTime;
    private boolean active;
    private Point lastMousePos;
    private boolean[] keyStates;
    private GlobalKeyboardListener keyboardListener;
    private GlobalMouseListener mouseListener;
    private boolean useNativeHook = false;

    public MacroRecorder() {
        try {
            robot = new Robot();
            robot.setAutoDelay(0);
            keyStates = new boolean[256];
            checkNativeHookAvailability();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private void checkNativeHookAvailability() {
        useNativeHook = NativeHookAdapter.isAvailable();
    }

    public boolean start() {
        return start(new RecordSettings());
    }
    
    public boolean start(RecordSettings settings) {
        return start(settings, null);
    }
    
    public boolean start(RecordSettings settings, Runnable stopCallback) {
        if (active) {
            stop();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        actions = new CopyOnWriteArrayList<>();
        startTime = System.currentTimeMillis();
        active = true;
        lastMousePos = MouseInfo.getPointerInfo().getLocation();
        
        if (!useNativeHook) {
            return false;
        }
        
        NativeHookAdapter.initialize(actions, startTime, keyStates, settings, stopCallback);
        boolean started = NativeHookAdapter.start();
        return started;
    }

    public List<Action> getActions() {
        return new ArrayList<>(actions);
    }

    public List<Action> stop() {
        active = false;
        if (useNativeHook) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}
            NativeHookAdapter.stop();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}
        } else {
            if (keyboardListener != null) {
                keyboardListener.stop();
            }
            if (mouseListener != null) {
                mouseListener.stop();
            }
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        return new ArrayList<>(actions);
    }


    private void startGlobalCapture() {
        keyboardListener = new GlobalKeyboardListener();
        mouseListener = new GlobalMouseListener();
        keyboardListener.start();
        mouseListener.start();
    }

    private class GlobalKeyboardListener {
        private KeyEventDispatcher dispatcher;
        private Thread pollThread;

        public void start() {
            dispatcher = new KeyEventDispatcher() {
                @Override
                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (!active) {
                        return false;
                    }
                    
                    long timestamp = System.currentTimeMillis() - startTime;
                    int keyCode = e.getKeyCode();
                    
                    if (keyCode >= 0 && keyCode < keyStates.length) {
                        boolean isPressed = e.getID() == KeyEvent.KEY_PRESSED;
                        
                        if (isPressed && !keyStates[keyCode]) {
                            Action action = new Action(Action.ActionType.KEY_PRESS, timestamp);
                            action.setKeyCode(keyCode);
                            action.setKeyChar(e.getKeyChar());
                            action.setModifiers(e.getModifiersEx());
                            synchronized (actions) {
                                actions.add(action);
                            }
                            keyStates[keyCode] = true;
                        } else if (!isPressed && keyStates[keyCode]) {
                            Action action = new Action(Action.ActionType.KEY_RELEASE, timestamp);
                            action.setKeyCode(keyCode);
                            action.setKeyChar(e.getKeyChar());
                            action.setModifiers(e.getModifiersEx());
                            synchronized (actions) {
                                actions.add(action);
                            }
                            keyStates[keyCode] = false;
                        }
                    }
                    
                    return false;
                }
            };
            
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
            
            pollThread = new Thread(() -> {
                while (active) {
                    try {
                        Toolkit.getDefaultToolkit().sync();
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            pollThread.setDaemon(true);
            pollThread.start();
        }

        public void stop() {
            if (dispatcher != null) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
            }
            if (pollThread != null) {
                pollThread.interrupt();
            }
        }
    }

    private class GlobalMouseListener {
        private Thread thread;
        private boolean running;

        public void start() {
            running = true;
            thread = new Thread(() -> {
                lastMousePos = MouseInfo.getPointerInfo().getLocation();
                
                long lastMoveTime = 0;
                while (active && running) {
                    try {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastMoveTime < 20) {
                            Thread.sleep(5);
                            continue;
                        }
                        
                        Point currentPos = MouseInfo.getPointerInfo().getLocation();
                        
                        if (lastMousePos == null || !currentPos.equals(lastMousePos)) {
                            Action moveAction = new Action(Action.ActionType.MOUSE_MOVE, currentTime - startTime);
                            moveAction.setX(currentPos.x);
                            moveAction.setY(currentPos.y);
                            synchronized (actions) {
                                actions.add(moveAction);
                            }
                            lastMousePos = currentPos;
                            lastMoveTime = currentTime;
                        }
                        
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                    }
                }
            });
            
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }

        public void stop() {
            running = false;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }
}