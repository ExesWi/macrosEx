public class Action {
    public enum ActionType {
        MOUSE_MOVE,
        MOUSE_CLICK,
        MOUSE_PRESS,
        MOUSE_RELEASE,
        KEY_PRESS,
        KEY_RELEASE,
        KEY_TYPE,
        KEY_COMBO,
        APP_LAUNCH,
        WINDOW_ACTIVATE,
        DELAY
    }

    private ActionType type;
    private long timestamp;
    private int x;
    private int y;
    private int button;
    private int keyCode;
    private char keyChar;
    private String stringValue;
    private long delay;
    private int modifiers;

    public Action(ActionType type, long timestamp) {
        this.type = type;
        this.timestamp = timestamp;
    }

    public ActionType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getButton() {
        return button;
    }

    public void setButton(int button) {
        this.button = button;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public char getKeyChar() {
        return keyChar;
    }

    public void setKeyChar(char keyChar) {
        this.keyChar = keyChar;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public int getModifiers() {
        return modifiers;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }
}