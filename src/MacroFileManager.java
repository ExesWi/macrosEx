import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MacroFileManager {
    
    public static void saveMacro(String filePath, String macroName, String description, 
                                 String comment, String software, List<Action> actions) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            
            Element rootElement = doc.createElement("Root");
            doc.appendChild(rootElement);
            
            Element defaultMacro = doc.createElement("DefaultMacro");
            rootElement.appendChild(defaultMacro);
            
            Element major = doc.createElement("Major");
            major.appendChild(doc.createTextNode(""));
            defaultMacro.appendChild(major);
            
            Element desc = doc.createElement("Description");
            desc.appendChild(doc.createTextNode(description != null ? description : ""));
            defaultMacro.appendChild(desc);
            
            Element comm = doc.createElement("Comment");
            comm.appendChild(doc.createTextNode(comment != null ? comment : ""));
            defaultMacro.appendChild(comm);
            
            Element guiOption = doc.createElement("GUIOption");
            Element repeatType = doc.createElement("RepeatType");
            repeatType.appendChild(doc.createTextNode("0"));
            guiOption.appendChild(repeatType);
            defaultMacro.appendChild(guiOption);
            
            Element keyUp = doc.createElement("KeyUp");
            Element syntaxUp = doc.createElement("Syntax");
            syntaxUp.appendChild(doc.createTextNode(""));
            keyUp.appendChild(syntaxUp);
            defaultMacro.appendChild(keyUp);
            
            Element keyDown = doc.createElement("KeyDown");
            Element syntaxDown = doc.createElement("Syntax");
            syntaxDown.appendChild(doc.createTextNode(buildSyntax(actions)));
            keyDown.appendChild(syntaxDown);
            defaultMacro.appendChild(keyDown);
            
            Element soft = doc.createElement("Software");
            soft.appendChild(doc.createTextNode(software != null ? software : ""));
            defaultMacro.appendChild(soft);
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка при сохранении макроса: " + e.getMessage());
        }
    }
    
    public static MacroData loadMacro(String filePath) {
        try {
            File file = new File(filePath);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(file);
            
            doc.getDocumentElement().normalize();
            
            NodeList defaultMacroList = doc.getElementsByTagName("DefaultMacro");
            if (defaultMacroList.getLength() == 0) {
                throw new RuntimeException("Не найден элемент DefaultMacro");
            }
            
            Node defaultMacroNode = defaultMacroList.item(0);
            if (defaultMacroNode.getNodeType() != Node.ELEMENT_NODE) {
                throw new RuntimeException("Некорректный формат XML");
            }
            
            Element defaultMacro = (Element) defaultMacroNode;
            
            String description = getElementText(defaultMacro, "Description");
            String comment = getElementText(defaultMacro, "Comment");
            String software = getElementText(defaultMacro, "Software");
            
            Element keyDown = (Element) defaultMacro.getElementsByTagName("KeyDown").item(0);
            if (keyDown == null) {
                throw new RuntimeException("Не найден элемент KeyDown");
            }
            
            Element syntax = (Element) keyDown.getElementsByTagName("Syntax").item(0);
            if (syntax == null) {
                throw new RuntimeException("Не найден элемент Syntax");
            }
            
            String syntaxText = syntax.getTextContent();
            List<Action> actions = parseSyntax(syntaxText);
            
            return new MacroData(description, comment, software, actions);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка при загрузке макроса: " + e.getMessage());
        }
    }
    
    private static String getElementText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            Node node = list.item(0);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return node.getTextContent();
            }
        }
        return "";
    }
    
    private static String buildSyntax(List<Action> actions) {
        StringBuilder sb = new StringBuilder();
        long lastTimestamp = 0;
        
        for (Action action : actions) {
            long delay = action.getTimestamp() - lastTimestamp;
            if (delay > 0) {
                sb.append("Delay ").append(delay).append(" ms\n");
            }
            
            switch (action.getType()) {
                case MOUSE_MOVE:
                    sb.append("MoveTo ").append(action.getX()).append(" ").append(action.getY()).append("\n");
                    break;
                    
                case MOUSE_PRESS:
                    if (action.getButton() == java.awt.event.InputEvent.BUTTON1_DOWN_MASK) {
                        sb.append("LeftDown 1\n");
                    } else if (action.getButton() == java.awt.event.InputEvent.BUTTON2_DOWN_MASK) {
                        sb.append("MiddleDown 1\n");
                    } else if (action.getButton() == java.awt.event.InputEvent.BUTTON3_DOWN_MASK) {
                        sb.append("RightDown 1\n");
                    }
                    break;
                    
                case MOUSE_RELEASE:
                    if (action.getButton() == java.awt.event.InputEvent.BUTTON1_DOWN_MASK) {
                        sb.append("LeftUp 1\n");
                    } else if (action.getButton() == java.awt.event.InputEvent.BUTTON2_DOWN_MASK) {
                        sb.append("MiddleUp 1\n");
                    } else if (action.getButton() == java.awt.event.InputEvent.BUTTON3_DOWN_MASK) {
                        sb.append("RightUp 1\n");
                    }
                    break;
                    
                case KEY_PRESS:
                    int modifiers = action.getModifiers();
                    int modFlag = 0;
                    if ((modifiers & java.awt.event.KeyEvent.CTRL_DOWN_MASK) != 0) modFlag |= 1;
                    if ((modifiers & java.awt.event.KeyEvent.ALT_DOWN_MASK) != 0) modFlag |= 2;
                    if ((modifiers & java.awt.event.KeyEvent.SHIFT_DOWN_MASK) != 0) modFlag |= 4;
                    if ((modifiers & java.awt.event.KeyEvent.META_DOWN_MASK) != 0) modFlag |= 8;
                    
                    sb.append("KeyDown ").append(action.getKeyCode()).append(" ").append(modFlag).append("\n");
                    break;
                    
                case KEY_RELEASE:
                    modifiers = action.getModifiers();
                    modFlag = 0;
                    if ((modifiers & java.awt.event.KeyEvent.CTRL_DOWN_MASK) != 0) modFlag |= 1;
                    if ((modifiers & java.awt.event.KeyEvent.ALT_DOWN_MASK) != 0) modFlag |= 2;
                    if ((modifiers & java.awt.event.KeyEvent.SHIFT_DOWN_MASK) != 0) modFlag |= 4;
                    if ((modifiers & java.awt.event.KeyEvent.META_DOWN_MASK) != 0) modFlag |= 8;
                    
                    sb.append("KeyUp ").append(action.getKeyCode()).append(" ").append(modFlag).append("\n");
                    break;
                    
                case DELAY:
                    sb.append("Delay ").append(action.getDelay()).append(" ms\n");
                    break;
            }
            
            lastTimestamp = action.getTimestamp();
        }
        
        return sb.toString();
    }
    
    private static List<Action> parseSyntax(String syntax) {
        List<Action> actions = new ArrayList<>();
        String[] lines = syntax.split("\n");
        long currentTime = 0;
        int loopStartLine = -1;
        int loopCount = 1;
        int lastMouseX = 0;
        int lastMouseY = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("\\s+");
            if (parts.length == 0) continue;
            
            String command = parts[0];
            
            if (command.equals("GoWhile")) {
                if (parts.length >= 3) {
                    loopStartLine = Integer.parseInt(parts[1]) - 1;
                    loopCount = Integer.parseInt(parts[2]);
                }
                break;
            }
            
            switch (command) {
                case "Delay":
                    if (parts.length >= 3 && parts[2].equals("ms")) {
                        long delay = Long.parseLong(parts[1]);
                        currentTime += delay;
                        Action delayAction = new Action(Action.ActionType.DELAY, currentTime);
                        delayAction.setDelay(delay);
                        actions.add(delayAction);
                    }
                    break;
                    
                case "MoveTo":
                    if (parts.length >= 3) {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        lastMouseX = x;
                        lastMouseY = y;
                        Action action = new Action(Action.ActionType.MOUSE_MOVE, currentTime);
                        action.setX(x);
                        action.setY(y);
                        actions.add(action);
                    }
                    break;
                    
                case "LeftDown":
                    Action leftDown = new Action(Action.ActionType.MOUSE_PRESS, currentTime);
                    leftDown.setButton(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                    leftDown.setX(lastMouseX);
                    leftDown.setY(lastMouseY);
                    actions.add(leftDown);
                    break;
                    
                case "LeftUp":
                    Action leftUp = new Action(Action.ActionType.MOUSE_RELEASE, currentTime);
                    leftUp.setButton(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                    leftUp.setX(lastMouseX);
                    leftUp.setY(lastMouseY);
                    actions.add(leftUp);
                    break;
                    
                case "RightDown":
                    Action rightDown = new Action(Action.ActionType.MOUSE_PRESS, currentTime);
                    rightDown.setButton(java.awt.event.InputEvent.BUTTON3_DOWN_MASK);
                    rightDown.setX(lastMouseX);
                    rightDown.setY(lastMouseY);
                    actions.add(rightDown);
                    break;
                    
                case "RightUp":
                    Action rightUp = new Action(Action.ActionType.MOUSE_RELEASE, currentTime);
                    rightUp.setButton(java.awt.event.InputEvent.BUTTON3_DOWN_MASK);
                    rightUp.setX(lastMouseX);
                    rightUp.setY(lastMouseY);
                    actions.add(rightUp);
                    break;
                    
                case "KeyDown":
                    if (parts.length >= 3) {
                        int keyCode = Integer.parseInt(parts[1]);
                        int modFlag = Integer.parseInt(parts[2]);
                        int modifiers = 0;
                        if ((modFlag & 1) != 0) modifiers |= java.awt.event.KeyEvent.CTRL_DOWN_MASK;
                        if ((modFlag & 2) != 0) modifiers |= java.awt.event.KeyEvent.ALT_DOWN_MASK;
                        if ((modFlag & 4) != 0) modifiers |= java.awt.event.KeyEvent.SHIFT_DOWN_MASK;
                        if ((modFlag & 8) != 0) modifiers |= java.awt.event.KeyEvent.META_DOWN_MASK;
                        
                        Action keyDown = new Action(Action.ActionType.KEY_PRESS, currentTime);
                        keyDown.setKeyCode(keyCode);
                        keyDown.setModifiers(modifiers);
                        actions.add(keyDown);
                    }
                    break;
                    
                case "KeyUp":
                    if (parts.length >= 3) {
                        int keyCode = Integer.parseInt(parts[1]);
                        int modFlag = Integer.parseInt(parts[2]);
                        int modifiers = 0;
                        if ((modFlag & 1) != 0) modifiers |= java.awt.event.KeyEvent.CTRL_DOWN_MASK;
                        if ((modFlag & 2) != 0) modifiers |= java.awt.event.KeyEvent.ALT_DOWN_MASK;
                        if ((modFlag & 4) != 0) modifiers |= java.awt.event.KeyEvent.SHIFT_DOWN_MASK;
                        if ((modFlag & 8) != 0) modifiers |= java.awt.event.KeyEvent.META_DOWN_MASK;
                        
                        Action keyUp = new Action(Action.ActionType.KEY_RELEASE, currentTime);
                        keyUp.setKeyCode(keyCode);
                        keyUp.setModifiers(modifiers);
                        actions.add(keyUp);
                    }
                    break;
            }
        }
        
        if (loopStartLine >= 0 && loopCount > 1 && loopStartLine < actions.size()) {
            List<Action> loopActions = new ArrayList<>(actions.subList(loopStartLine, actions.size()));
            long baseTime = actions.isEmpty() ? 0 : actions.get(actions.size() - 1).getTimestamp();
            
            for (int i = 0; i < loopCount - 1; i++) {
                long timeOffset = baseTime + (i + 1) * 1000;
                for (Action action : loopActions) {
                    Action newAction = new Action(action.getType(), timeOffset + action.getTimestamp());
                    newAction.setX(action.getX());
                    newAction.setY(action.getY());
                    newAction.setButton(action.getButton());
                    newAction.setKeyCode(action.getKeyCode());
                    newAction.setModifiers(action.getModifiers());
                    newAction.setKeyChar(action.getKeyChar());
                    newAction.setStringValue(action.getStringValue());
                    newAction.setDelay(action.getDelay());
                    actions.add(newAction);
                }
                baseTime = actions.get(actions.size() - 1).getTimestamp();
            }
        }
        
        return actions;
    }
    
    public static class MacroData {
        private String description;
        private String comment;
        private String software;
        private List<Action> actions;
        
        public MacroData(String description, String comment, String software, List<Action> actions) {
            this.description = description;
            this.comment = comment;
            this.software = software;
            this.actions = actions;
        }
        
        public String getDescription() { return description; }
        public String getComment() { return comment; }
        public String getSoftware() { return software; }
        public List<Action> getActions() { return actions; }
    }
}
