public class RecordSettings {
    private boolean recordKeyboard;
    private boolean recordMouseButtons;
    private boolean recordAbsoluteMouseMovement;
    private boolean recordRelativeMouseMovement;
    private boolean insertLongPresses;
    
    public RecordSettings() {
        this.recordKeyboard = true;
        this.recordMouseButtons = true;
        this.recordAbsoluteMouseMovement = true;
        this.recordRelativeMouseMovement = false;
        this.insertLongPresses = true;
    }
    
    public RecordSettings(boolean recordKeyboard, boolean recordMouseButtons, 
                         boolean recordAbsoluteMouseMovement, boolean recordRelativeMouseMovement,
                         boolean insertLongPresses) {
        this.recordKeyboard = recordKeyboard;
        this.recordMouseButtons = recordMouseButtons;
        this.recordAbsoluteMouseMovement = recordAbsoluteMouseMovement;
        this.recordRelativeMouseMovement = recordRelativeMouseMovement;
        this.insertLongPresses = insertLongPresses;
    }
    
    public boolean isRecordKeyboard() {
        return recordKeyboard;
    }
    
    public void setRecordKeyboard(boolean recordKeyboard) {
        this.recordKeyboard = recordKeyboard;
    }
    
    public boolean isRecordMouseButtons() {
        return recordMouseButtons;
    }
    
    public void setRecordMouseButtons(boolean recordMouseButtons) {
        this.recordMouseButtons = recordMouseButtons;
    }
    
    public boolean isRecordAbsoluteMouseMovement() {
        return recordAbsoluteMouseMovement;
    }
    
    public void setRecordAbsoluteMouseMovement(boolean recordAbsoluteMouseMovement) {
        this.recordAbsoluteMouseMovement = recordAbsoluteMouseMovement;
    }
    
    public boolean isRecordRelativeMouseMovement() {
        return recordRelativeMouseMovement;
    }
    
    public void setRecordRelativeMouseMovement(boolean recordRelativeMouseMovement) {
        this.recordRelativeMouseMovement = recordRelativeMouseMovement;
    }
    
    public boolean isInsertLongPresses() {
        return insertLongPresses;
    }
    
    public void setInsertLongPresses(boolean insertLongPresses) {
        this.insertLongPresses = insertLongPresses;
    }
}
