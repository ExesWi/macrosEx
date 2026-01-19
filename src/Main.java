import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static List<Action> currentActions = new ArrayList<>();
    private static boolean recording = false;
    private static MacroRecorder recorder;
    private static MacroPlayer player;
    private static MacroManager macroManager;
    private static LicenseManager licenseManager;
    
    private static JFrame frame;
    private static JButton recordButton;
    private static JButton stopButton;
    private static JButton playButton;
    private static JList<Action> actionsList;
    private static DefaultListModel<Action> listModel;
    private static JTree macroTree;
    private static DefaultTreeModel treeModel;
    private static DefaultMutableTreeNode rootNode;
    private static String currentMacroName = null;

    public static void main(String[] args) {
        System.setErr(new java.io.PrintStream(System.err) {
            @Override
            public void println(String x) {
                if (x != null && (x.contains("ClassNotFoundException") && 
                    x.contains("EditorCopyPasteHelperImpl"))) {
                    return;
                }
                super.println(x);
            }
        });
        
        SwingUtilities.invokeLater(() -> {
            licenseManager = new LicenseManager();
            
            if (!licenseManager.isLicenseValid()) {
                if (!showLicenseDialog()) {
                    System.exit(0);
                    return;
                }
            }
            
            recorder = new MacroRecorder();
            player = new MacroPlayer();
            macroManager = new MacroManager();
            macroManager.loadMacrosFromFiles();
            createGUI();
        });
    }

    private static void createGUI() {
        frame = new JFrame("Редактор макросов");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        
        createMenuBar();
        
        Color darkBg = new Color(24, 24, 30);
        Color sidebarBg = new Color(32, 32, 40);
        frame.getContentPane().setBackground(darkBg);
        
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(darkBg);
        
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftSplit.setDividerLocation(250);
        leftSplit.setDividerSize(3);
        leftSplit.setBorder(null);
        
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplit.setDividerLocation(900);
        rightSplit.setDividerSize(3);
        rightSplit.setBorder(null);
        
        mainPanel.add(createLeftPanel(), BorderLayout.WEST);
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER);
        mainPanel.add(createRightPanel(), BorderLayout.EAST);
        
        frame.add(mainPanel);
        frame.setVisible(true);
        
        populateTreeFromFiles();
        
        startLicenseChecker();
        
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (licenseCheckTimer != null) {
                    licenseCheckTimer.stop();
                }
                if (recording) {
                    recorder.stop();
                }
                System.exit(0);
            }
        });
    }
    
    private static javax.swing.Timer licenseCheckTimer;
    private static JDialog blockingLicenseDialog;
    
    private static void startLicenseChecker() {
        licenseCheckTimer = new javax.swing.Timer(1000, e -> {
            if (frame != null && frame.isVisible()) {
                if (!licenseManager.isLicenseValid()) {
                    SwingUtilities.invokeLater(() -> {
                        if (blockingLicenseDialog == null || !blockingLicenseDialog.isVisible()) {
                            showBlockingLicenseDialog();
                        }
                    });
                }
            }
        });
        licenseCheckTimer.start();
    }
    
    private static void showBlockingLicenseDialog() {
        Color bgColor = new Color(30, 30, 35);
        Color textColor = new Color(240, 240, 245);
        Color buttonColor = new Color(70, 130, 230);
        Color buttonHover = new Color(90, 150, 255);
        
        blockingLicenseDialog = new JDialog(frame, "Лицензия истекла", true);
        blockingLicenseDialog.setSize(500, 350);
        blockingLicenseDialog.setLocationRelativeTo(frame);
        blockingLicenseDialog.getContentPane().setBackground(bgColor);
        blockingLicenseDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        blockingLicenseDialog.setModal(true);
        
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("<html><h2 style='text-align: center; color: #FF6B6B;'>⚠ Лицензия истекла</h2></html>");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(bgColor);
        
        JLabel infoLabel = new JLabel("<html><div style='text-align: center; color: #ccc; font-size: 13px;'><b>Срок действия лицензии истек</b></div><div style='text-align: center; color: #888; font-size: 12px; margin-top: 10px;'>Для продолжения работы необходимо ввести действующий лицензионный ключ</div></html>");
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(infoLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        
        JLabel keyLabel = new JLabel("Лицензионный ключ:");
        keyLabel.setForeground(textColor);
        keyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(keyLabel);
        centerPanel.add(Box.createVerticalStrut(5));
        
        JTextField keyField = new JTextField(30);
        keyField.setBackground(new Color(40, 40, 45));
        keyField.setForeground(textColor);
        keyField.setCaretColor(textColor);
        keyField.setBorder(new EmptyBorder(8, 10, 8, 10));
        keyField.setAlignmentX(Component.CENTER_ALIGNMENT);
        keyField.setMaximumSize(new Dimension(400, 35));
        centerPanel.add(keyField);
        centerPanel.add(Box.createVerticalStrut(20));
        
        keyField.addActionListener(e -> {
            String key = keyField.getText().trim();
            if (!key.isEmpty()) {
                licenseManager.setLicenseKey(key);
                if (licenseManager.isLicenseValid()) {
                    blockingLicenseDialog.dispose();
                    blockingLicenseDialog = null;
                    JOptionPane.showMessageDialog(frame, "Лицензия успешно активирована!", "Успех", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    keyField.setText("");
                    keyField.requestFocus();
                    JOptionPane.showMessageDialog(blockingLicenseDialog, "Неверный лицензионный ключ", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(bgColor);
        
        JButton activateButton = createModernButton("Активировать", buttonColor, buttonHover);
        activateButton.setPreferredSize(new Dimension(120, 35));
        activateButton.addActionListener(e -> {
            String key = keyField.getText().trim();
            if (key.isEmpty()) {
                JOptionPane.showMessageDialog(blockingLicenseDialog, "Введите лицензионный ключ", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            licenseManager.setLicenseKey(key);
            if (licenseManager.isLicenseValid()) {
                blockingLicenseDialog.dispose();
                blockingLicenseDialog = null;
                JOptionPane.showMessageDialog(frame, "Лицензия успешно активирована!", "Успех", JOptionPane.INFORMATION_MESSAGE);
            } else {
                keyField.setText("");
                keyField.requestFocus();
                JOptionPane.showMessageDialog(blockingLicenseDialog, "Неверный лицензионный ключ", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton websiteButton = createModernButton("Посетить сайт", new Color(90, 90, 110), new Color(110, 110, 130));
        websiteButton.setPreferredSize(new Dimension(120, 35));
        websiteButton.addActionListener(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://macros.github.io"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(blockingLicenseDialog, "Не удалось открыть сайт", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        buttonPanel.add(activateButton);
        buttonPanel.add(websiteButton);
        
        centerPanel.add(buttonPanel);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        blockingLicenseDialog.add(mainPanel);
        
        blockingLicenseDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (!licenseManager.isLicenseValid()) {
                    JOptionPane.showMessageDialog(blockingLicenseDialog, 
                        "Необходимо ввести действующий лицензионный ключ для продолжения работы.", 
                        "Лицензия обязательна", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        
        blockingLicenseDialog.setVisible(true);
        keyField.requestFocus();
    }
    
    private static void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(35, 38, 48));
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 55, 70)));
        
        JMenu helpMenu = new JMenu("Справка");
        helpMenu.setForeground(new Color(240, 240, 245));
        helpMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JMenuItem licenseItem = new JMenuItem("Лицензия");
        licenseItem.setForeground(new Color(240, 240, 245));
        licenseItem.setBackground(new Color(35, 38, 48));
        licenseItem.addActionListener(e -> showLicenseDialog());
        helpMenu.add(licenseItem);
        
        JMenuItem websiteItem = new JMenuItem("Посетить сайт");
        websiteItem.setForeground(new Color(240, 240, 245));
        websiteItem.setBackground(new Color(35, 38, 48));
        websiteItem.addActionListener(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://macros.github.io"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Не удалось открыть сайт", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
        helpMenu.add(websiteItem);
        
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);
    }

    private static JPanel createLeftPanel() {
        Color panelBg = new Color(35, 38, 48);
        Color textColor = new Color(240, 240, 245);
        
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(panelBg);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 2, new Color(50, 55, 70)),
            new EmptyBorder(15, 15, 10, 15)
        ));
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setMinimumSize(new Dimension(250, 0));
        panel.setMaximumSize(new Dimension(250, Integer.MAX_VALUE));
        
        JLabel titleLabel = new JLabel("<html><b>📁 Добавленные</b></html>");
        titleLabel.setForeground(textColor);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        Color selectionBg = new Color(70, 120, 220);
        Color hoverBg = new Color(50, 55, 70);
        
        rootNode = new DefaultMutableTreeNode("Макросы");
        treeModel = new DefaultTreeModel(rootNode);
        macroTree = new JTree(treeModel);
        macroTree.setBackground(panelBg);
        macroTree.setForeground(textColor);
        macroTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        macroTree.setCellRenderer(new DefaultTreeCellRenderer() {
            {
                setBackgroundNonSelectionColor(panelBg);
                setBackgroundSelectionColor(selectionBg);
                setTextSelectionColor(Color.WHITE);
                setTextNonSelectionColor(textColor);
                setFont(new Font("Segoe UI", Font.PLAIN, 13));
            }
            
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                setBackgroundNonSelectionColor(panelBg);
                setBackgroundSelectionColor(sel ? selectionBg : hoverBg);
                setTextSelectionColor(sel ? Color.WHITE : textColor);
                setTextNonSelectionColor(textColor);
                setFont(new Font("Segoe UI", Font.PLAIN, 13));
                return this;
            }
        });
        
        macroTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) macroTree.getLastSelectedPathComponent();
            if (node != null && node.isLeaf() && !node.equals(rootNode)) {
                String macroName = node.getUserObject().toString();
                loadMacro(macroName);
            } else if (node != null && !node.isLeaf() && !node.equals(rootNode)) {
                currentMacroName = null;
                currentActions.clear();
                updateActionsList();
            }
        });
        
        JScrollPane treeScroll = new JScrollPane(macroTree);
        treeScroll.setBackground(panelBg);
        treeScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, panelBg));
        treeScroll.getViewport().setBackground(panelBg);
        panel.add(treeScroll, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttonPanel.setBackground(panelBg);
        buttonPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        buttonPanel.setPreferredSize(new Dimension(220, 55));
        buttonPanel.setMinimumSize(new Dimension(220, 55));
        buttonPanel.setMaximumSize(new Dimension(220, 55));
        
        JButton addMacroButton = createModernButton("+ Макрос", new Color(70, 130, 230), new Color(90, 150, 255));
        addMacroButton.setPreferredSize(new Dimension(100, 35));
        addMacroButton.setMinimumSize(new Dimension(100, 35));
        addMacroButton.setMaximumSize(new Dimension(100, 35));
        addMacroButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addMacroButton.addActionListener(e -> showCreateMacroDialog());
        
        JButton addFolderButton = createModernButton("+ Папка", new Color(90, 90, 110), new Color(110, 110, 130));
        addFolderButton.setPreferredSize(new Dimension(100, 35));
        addFolderButton.setMinimumSize(new Dimension(100, 35));
        addFolderButton.setMaximumSize(new Dimension(100, 35));
        addFolderButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addFolderButton.addActionListener(e -> showCreateFolderDialog());
        
        buttonPanel.add(addMacroButton);
        buttonPanel.add(addFolderButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private static JPanel createCenterPanel() {
        Color panelBg = new Color(26, 28, 34);
        Color toolbarBg = new Color(38, 42, 52);
        Color textColor = new Color(240, 240, 245);
        
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(panelBg);
        
        JPanel topToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topToolbar.setBackground(toolbarBg);
        topToolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 55, 70)),
            new EmptyBorder(12, 15, 12, 15)
        ));
        topToolbar.setPreferredSize(new Dimension(0, 60));
        topToolbar.setMinimumSize(new Dimension(0, 60));
        topToolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        recordButton = createModernButton("🔴 Начать запись", new Color(230, 70, 70), new Color(255, 100, 100));
        recordButton.addActionListener(e -> startRecording());
        
        stopButton = createModernButton("⏹ Остановить", new Color(180, 60, 60), new Color(220, 80, 80));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopRecording());
        
        playButton = createModernButton("▶ Воспроизвести", new Color(70, 180, 80), new Color(90, 210, 100));
        playButton.addActionListener(e -> playMacro());
        
        JButton clearButton = createModernButton("🗑 Очистить", new Color(100, 100, 120), new Color(120, 120, 140));
        clearButton.addActionListener(e -> clearActions());
        
        JButton deleteButton = createModernButton("✕ Удалить", new Color(180, 60, 60), new Color(210, 80, 80));
        deleteButton.addActionListener(e -> deleteSelectedAction());
        
        JButton saveButton = createModernButton("💾 Сохранить", new Color(70, 130, 230), new Color(90, 150, 255));
        saveButton.addActionListener(e -> showSaveMacroDialog());
        
        JButton loadButton = createModernButton("📂 Загрузить", new Color(120, 170, 70), new Color(140, 200, 90));
        loadButton.addActionListener(e -> showLoadMacroDialog());
        
        topToolbar.add(recordButton);
        topToolbar.add(stopButton);
        topToolbar.add(playButton);
        topToolbar.add(clearButton);
        topToolbar.add(deleteButton);
        topToolbar.add(saveButton);
        topToolbar.add(loadButton);
        
        panel.add(topToolbar, BorderLayout.NORTH);
        
        Color listBg = new Color(30, 32, 38);
        Color textColor2 = new Color(240, 240, 245);
        Color selectionBg2 = new Color(70, 120, 220);
        
        listModel = new DefaultListModel<>();
        actionsList = new JList<>(listModel);
        actionsList.setBackground(listBg);
        actionsList.setForeground(textColor2);
        actionsList.setSelectionBackground(selectionBg2);
        actionsList.setSelectionForeground(Color.WHITE);
        actionsList.setFixedCellHeight(28);
        actionsList.setFont(new Font("Consolas", Font.PLAIN, 12));
        actionsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? selectionBg2 : listBg);
                setForeground(isSelected ? Color.WHITE : textColor2);
                setFont(new Font("Consolas", Font.PLAIN, 12));
                setBorder(new EmptyBorder(4, 8, 4, 8));
                
                if (value instanceof Action) {
                    Action action = (Action) value;
                    String text = formatAction(action);
                    setText(text);
                }
                return this;
            }
        });
        
        JScrollPane listScroll = new JScrollPane(actionsList);
        listScroll.setBackground(listBg);
        listScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, listBg));
        listScroll.getViewport().setBackground(listBg);
        panel.add(listScroll, BorderLayout.CENTER);
        
        return panel;
    }

    private static JPanel createRightPanel() {
        Color panelBg = new Color(35, 38, 48);
        
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(panelBg);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(50, 55, 70)),
            new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setPreferredSize(new Dimension(70, 0));
        panel.setMinimumSize(new Dimension(70, 0));
        panel.setMaximumSize(new Dimension(70, Integer.MAX_VALUE));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(panelBg);
        
        JButton loopButton = createModernIconButton("⟲", "Цикл", new Color(100, 160, 240), new Color(130, 190, 255));
        loopButton.addActionListener(e -> showLoopDialog());
        
        buttonPanel.add(loopButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        return panel;
    }

    private static JButton createModernButton(String text, Color normalColor, Color hoverColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color bgColor;
                if (!isEnabled()) {
                    bgColor = new Color(normalColor.getRed() / 2, normalColor.getGreen() / 2, normalColor.getBlue() / 2);
                } else if (getModel().isRollover() || getModel().isPressed()) {
                    bgColor = hoverColor;
                } else {
                    bgColor = normalColor;
                }
                
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setBackground(normalColor);
        button.setForeground(Color.WHITE);
        button.setBorder(null);
        button.setFocusPainted(false);
        Dimension buttonSize = new Dimension(140, 36);
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.getModel().setRollover(true);
                    button.repaint();
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.getModel().setRollover(false);
                button.repaint();
            }
        });
        
        return button;
    }
    
    private static JButton createToolbarButton(String text, Color color) {
        return createModernButton(text, color, color.brighter());
    }

    private static JButton createModernIconButton(String icon, String tooltip, Color normalColor, Color hoverColor) {
        JButton button = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isRollover() || getModel().isPressed()) {
                    g2.setColor(hoverColor);
                } else {
                    g2.setColor(normalColor);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                if (getModel().isRollover() || getModel().isPressed()) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setToolTipText(tooltip);
        button.setBackground(normalColor);
        button.setForeground(Color.WHITE);
        button.setBorder(null);
        button.setFocusPainted(false);
        Dimension iconSize = new Dimension(50, 50);
        button.setPreferredSize(iconSize);
        button.setMinimumSize(iconSize);
        button.setMaximumSize(iconSize);
        button.setFont(new Font("Segoe UI Symbol", Font.BOLD, 24));
        return button;
    }
    
    private static JButton createIconButton(String icon, String tooltip) {
        return createModernIconButton(icon, tooltip, new Color(50, 50, 60), new Color(70, 70, 80));
    }

    private static String formatAction(Action action) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><div style='padding: 2px;'>");
        sb.append("<span style='color: #888; font-size: 11px;'>");
        sb.append(String.format("[%dms]", action.getTimestamp()));
        sb.append("</span> ");
        
        switch (action.getType()) {
            case MOUSE_MOVE:
                sb.append("<span style='color: #6DD5FA;'>🖱 Мышь:</span> ");
                sb.append("<span style='color: #FFD700;'>движение</span> ");
                sb.append("<span style='color: #FF6B6B;'>→</span> ");
                sb.append(String.format("<span style='color: #98FB98;'>(%d, %d)</span>", action.getX(), action.getY()));
                break;
            case MOUSE_PRESS:
                String buttonName = getButtonName(action.getButton());
                sb.append("<span style='color: #6DD5FA;'>🖱 Мышь:</span> ");
                sb.append("<span style='color: #4ECDC4;'>нажатие</span> ");
                sb.append(String.format("<span style='color: #FFE66D;'>%s</span> ", buttonName));
                sb.append("<span style='color: #FF6B6B;'>→</span> ");
                sb.append(String.format("<span style='color: #98FB98;'>(%d, %d)</span>", action.getX(), action.getY()));
                break;
            case MOUSE_RELEASE:
                buttonName = getButtonName(action.getButton());
                sb.append("<span style='color: #6DD5FA;'>🖱 Мышь:</span> ");
                sb.append("<span style='color: #95E1D3;'>отпускание</span> ");
                sb.append(String.format("<span style='color: #FFE66D;'>%s</span> ", buttonName));
                sb.append("<span style='color: #FF6B6B;'>→</span> ");
                sb.append(String.format("<span style='color: #98FB98;'>(%d, %d)</span>", action.getX(), action.getY()));
                break;
            case KEY_PRESS:
                sb.append("<span style='color: #A8E6CF;'>⌨ Клавиша:</span> ");
                sb.append("<span style='color: #4ECDC4;'>нажатие</span> ");
                sb.append(String.format("<span style='color: #FFE66D; font-weight: bold;'>%s</span>", getKeyName(action.getKeyCode(), action.getModifiers())));
                break;
            case KEY_RELEASE:
                sb.append("<span style='color: #A8E6CF;'>⌨ Клавиша:</span> ");
                sb.append("<span style='color: #95E1D3;'>отпускание</span> ");
                sb.append(String.format("<span style='color: #FFE66D; font-weight: bold;'>%s</span>", getKeyName(action.getKeyCode(), action.getModifiers())));
                break;
            default:
                sb.append("<span style='color: #FFF;'>");
                sb.append(action.getType().toString());
                sb.append("</span>");
        }
        sb.append("</div></html>");
        return sb.toString();
    }

    private static String getButtonName(int button) {
        if (button == java.awt.event.InputEvent.BUTTON1_DOWN_MASK) return "Левая";
        if (button == java.awt.event.InputEvent.BUTTON2_DOWN_MASK) return "Средняя";
        if (button == java.awt.event.InputEvent.BUTTON3_DOWN_MASK) return "Правая";
        return "Кнопка";
    }

    private static String getKeyName(int keyCode, int modifiers) {
        StringBuilder sb = new StringBuilder();
        if ((modifiers & java.awt.event.KeyEvent.CTRL_DOWN_MASK) != 0) sb.append("Ctrl+");
        if ((modifiers & java.awt.event.KeyEvent.ALT_DOWN_MASK) != 0) sb.append("Alt+");
        if ((modifiers & java.awt.event.KeyEvent.SHIFT_DOWN_MASK) != 0) sb.append("Shift+");
        if ((modifiers & java.awt.event.KeyEvent.META_DOWN_MASK) != 0) sb.append("Win+");
        
        if (keyCode == 6) {
            keyCode = java.awt.event.KeyEvent.VK_C;
        }
        
        String keyText = java.awt.event.KeyEvent.getKeyText(keyCode);
        if (keyText == null || keyText.contains("Unknown") || keyText.isEmpty() || 
            keyText.equals("Kenji") || keyText.equals("Kanji") || keyText.contains("Kanji")) {
            if (keyCode == java.awt.event.KeyEvent.VK_C || keyCode == 6) {
                keyText = "C";
            } else if (keyCode == java.awt.event.KeyEvent.VK_V) {
                keyText = "V";
            } else if (keyCode == java.awt.event.KeyEvent.VK_X) {
                keyText = "X";
            } else if (keyCode == java.awt.event.KeyEvent.VK_A) {
                keyText = "A";
            } else if (keyCode == java.awt.event.KeyEvent.VK_Z) {
                keyText = "Z";
            } else if (keyCode == java.awt.event.KeyEvent.VK_Y) {
                keyText = "Y";
            } else if (keyCode == java.awt.event.KeyEvent.VK_KANJI || keyCode == 244) {
                keyText = "Kanji";
            } else if (keyCode == java.awt.event.KeyEvent.VK_UP) {
                keyText = "↑";
            } else if (keyCode == java.awt.event.KeyEvent.VK_DOWN) {
                keyText = "↓";
            } else if (keyCode == java.awt.event.KeyEvent.VK_LEFT) {
                keyText = "←";
            } else if (keyCode == java.awt.event.KeyEvent.VK_RIGHT) {
                keyText = "→";
            } else {
                keyText = "Key " + keyCode;
            }
        }
        
        if (keyCode == java.awt.event.KeyEvent.VK_CONTROL || 
            keyCode == java.awt.event.KeyEvent.VK_ALT || 
            keyCode == java.awt.event.KeyEvent.VK_SHIFT ||
            keyCode == java.awt.event.KeyEvent.VK_META ||
            keyCode == 524) {
            sb.append(keyText);
        } else {
            if (keyCode == java.awt.event.KeyEvent.VK_C && (modifiers & java.awt.event.KeyEvent.CTRL_DOWN_MASK) != 0) {
                sb.append("C (Копировать)");
            } else if (keyCode == java.awt.event.KeyEvent.VK_V && (modifiers & java.awt.event.KeyEvent.CTRL_DOWN_MASK) != 0) {
                sb.append("V (Вставить)");
            } else if (keyCode == java.awt.event.KeyEvent.VK_X && (modifiers & java.awt.event.KeyEvent.CTRL_DOWN_MASK) != 0) {
                sb.append("X (Вырезать)");
            } else if (keyCode == java.awt.event.KeyEvent.VK_A && (modifiers & java.awt.event.KeyEvent.CTRL_DOWN_MASK) != 0) {
                sb.append("A (Выделить все)");
            } else if (keyCode == java.awt.event.KeyEvent.VK_Z && (modifiers & java.awt.event.KeyEvent.CTRL_DOWN_MASK) != 0) {
                sb.append("Z (Отменить)");
            } else if (keyCode == java.awt.event.KeyEvent.VK_Y && (modifiers & java.awt.event.KeyEvent.CTRL_DOWN_MASK) != 0) {
                sb.append("Y (Повторить)");
            } else {
                sb.append(keyText);
            }
        }
        return sb.toString();
    }

    private static void showCreateFolderDialog() {
        JTextField folderField = new JTextField(20);
        folderField.setBackground(new Color(40, 40, 40));
        folderField.setForeground(Color.WHITE);
        folderField.setCaretColor(Color.WHITE);
        
        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
        panel.setBackground(new Color(30, 30, 30));
        
        JLabel label = new JLabel("Имя папки:");
        label.setForeground(Color.WHITE);
        panel.add(label);
        panel.add(folderField);
        
        int result = JOptionPane.showConfirmDialog(frame, panel, "Создать папку", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String folderName = folderField.getText().trim();
            if (!folderName.isEmpty()) {
                DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(folderName);
                treeModel.insertNodeInto(folderNode, rootNode, rootNode.getChildCount());
                macroTree.expandPath(new TreePath(rootNode.getPath()));
            }
        }
    }

    private static void showSaveMacroDialog() {
        if (currentMacroName == null || currentActions.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Нет макроса для сохранения", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.setBackground(new Color(30, 30, 30));
        
        JLabel descLabel = new JLabel("Описание:");
        descLabel.setForeground(Color.WHITE);
        JTextField descField = new JTextField(macroManager.getMacroDescription(currentMacroName), 30);
        descField.setBackground(new Color(40, 40, 40));
        descField.setForeground(Color.WHITE);
        descField.setCaretColor(Color.WHITE);
        
        JLabel commentLabel = new JLabel("Комментарий:");
        commentLabel.setForeground(Color.WHITE);
        JTextField commentField = new JTextField(macroManager.getMacroComment(currentMacroName), 30);
        commentField.setBackground(new Color(40, 40, 40));
        commentField.setForeground(Color.WHITE);
        commentField.setCaretColor(Color.WHITE);
        
        JLabel softwareLabel = new JLabel("Программа:");
        softwareLabel.setForeground(Color.WHITE);
        JTextField softwareField = new JTextField(macroManager.getMacroSoftware(currentMacroName), 30);
        softwareField.setBackground(new Color(40, 40, 40));
        softwareField.setForeground(Color.WHITE);
        softwareField.setCaretColor(Color.WHITE);
        
        panel.add(descLabel);
        panel.add(descField);
        panel.add(commentLabel);
        panel.add(commentField);
        panel.add(softwareLabel);
        panel.add(softwareField);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        
        int result = JOptionPane.showConfirmDialog(frame, panel, "Сохранить макрос", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            macroManager.saveMacro(currentMacroName, 
                descField.getText().trim(),
                commentField.getText().trim(),
                softwareField.getText().trim(),
                currentActions);
            JOptionPane.showMessageDialog(frame, "Макрос сохранен", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static void showLoadMacroDialog() {
        JFileChooser fileChooser = new JFileChooser("macros");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".amc");
            }
            
            @Override
            public String getDescription() {
                return "AMC файлы (*.amc)";
            }
        });
        
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String name = file.getName().replace(".amc", "");
                MacroFileManager.MacroData data = MacroFileManager.loadMacro(file.getAbsolutePath());
                
                currentMacroName = name;
                currentActions = data.getActions();
                
                macroManager.createMacro(name, null);
                macroManager.saveMacro(name, data.getDescription(), data.getComment(), data.getSoftware(), data.getActions());
                addMacroToTree(name, null);
                updateActionsList();
                
                JOptionPane.showMessageDialog(frame, "Макрос загружен", "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Ошибка при загрузке: " + e.getMessage(), 
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private static void showCreateMacroDialog() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(new Color(30, 30, 30));
        
        JPanel contentPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        contentPanel.setBackground(new Color(30, 30, 30));
        
        JLabel nameLabel = new JLabel("Имя макроса:");
        nameLabel.setForeground(Color.WHITE);
        JTextField nameField = new JTextField(20);
        nameField.setBackground(new Color(40, 40, 40));
        nameField.setForeground(Color.WHITE);
        nameField.setCaretColor(Color.WHITE);
        
        JCheckBox folderCheck = new JCheckBox("Создать в папке");
        folderCheck.setBackground(new Color(30, 30, 30));
        folderCheck.setForeground(Color.WHITE);
        
        JComboBox<String> folderCombo = new JComboBox<>();
        folderCombo.setEnabled(false);
        folderCombo.setBackground(new Color(40, 40, 40));
        folderCombo.setForeground(Color.WHITE);
        updateFolderCombo(folderCombo);
        
        folderCheck.addActionListener(e -> {
            folderCombo.setEnabled(folderCheck.isSelected());
            if (folderCheck.isSelected()) {
                updateFolderCombo(folderCombo);
            }
        });
        
        JTextField newFolderField = new JTextField(20);
        newFolderField.setEnabled(false);
        newFolderField.setBackground(new Color(40, 40, 40));
        newFolderField.setForeground(Color.WHITE);
        newFolderField.setCaretColor(Color.WHITE);
        newFolderField.setText("Или введите новую папку");
        
        folderCombo.addActionListener(e -> {
            if (folderCombo.getSelectedItem() != null && 
                folderCombo.getSelectedItem().toString().equals("+ Создать новую")) {
                newFolderField.setEnabled(true);
                newFolderField.setText("");
            } else {
                newFolderField.setEnabled(false);
                newFolderField.setText("Или введите новую папку");
            }
        });
        
        contentPanel.add(nameLabel);
        contentPanel.add(nameField);
        contentPanel.add(folderCheck);
        contentPanel.add(folderCombo);
        contentPanel.add(new JLabel(""));
        contentPanel.add(newFolderField);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        int result = JOptionPane.showConfirmDialog(frame, mainPanel, "Создать макрос", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                if (macroManager.macroExists(name)) {
                    JOptionPane.showMessageDialog(frame, "Макрос с таким именем уже существует", 
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String folderPath = null;
                if (folderCheck.isSelected()) {
                    if (newFolderField.isEnabled() && !newFolderField.getText().trim().isEmpty() && 
                        !newFolderField.getText().equals("Или введите новую папку")) {
                        folderPath = newFolderField.getText().trim();
                    } else if (folderCombo.getSelectedItem() != null) {
                        String selected = folderCombo.getSelectedItem().toString();
                        if (!selected.equals("+ Создать новую")) {
                            folderPath = selected;
                        }
                    }
                }
                
                macroManager.createMacro(name, folderPath);
                addMacroToTree(name, folderPath);
                currentMacroName = name;
                currentActions.clear();
                updateActionsList();
            }
        }
    }
    
    private static void updateFolderCombo(JComboBox<String> combo) {
        combo.removeAllItems();
        for (String folder : getTreeFolders()) {
            combo.addItem(folder);
        }
        combo.addItem("+ Создать новую");
    }
    
    private static List<String> getTreeFolders() {
        List<String> folders = new ArrayList<>();
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            if (!node.isLeaf()) {
                folders.add(node.getUserObject().toString());
            }
        }
        return folders;
    }

    private static void addMacroToTree(String macroName, String folderPath) {
        DefaultMutableTreeNode parent = rootNode;
        
        String displayName = macroName;
        if (macroName.contains("/")) {
            int lastSlash = macroName.lastIndexOf("/");
            displayName = macroName.substring(lastSlash + 1);
            String actualFolderPath = macroName.substring(0, lastSlash);
            if (folderPath == null || folderPath.isEmpty()) {
                folderPath = actualFolderPath;
            }
        }
        
        if (folderPath != null && !folderPath.isEmpty()) {
            DefaultMutableTreeNode folderNode = findOrCreateFolder(folderPath);
            parent = folderNode;
        }
        
        DefaultMutableTreeNode macroNode = new DefaultMutableTreeNode(displayName);
        macroNode.setUserObject(new MacroTreeNode(displayName, macroName));
        treeModel.insertNodeInto(macroNode, parent, parent.getChildCount());
        macroTree.expandPath(new javax.swing.tree.TreePath(parent.getPath()));
    }
    
    private static class MacroTreeNode {
        String displayName;
        String fullName;
        
        MacroTreeNode(String displayName, String fullName) {
            this.displayName = displayName;
            this.fullName = fullName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }

    private static DefaultMutableTreeNode findOrCreateFolder(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
            return rootNode;
        }
        
        String[] pathParts = folderPath.split("/");
        DefaultMutableTreeNode current = rootNode;
        
        for (String part : pathParts) {
            DefaultMutableTreeNode found = null;
            for (int i = 0; i < current.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) current.getChildAt(i);
                if (!node.isLeaf() && part.equals(node.getUserObject().toString())) {
                    found = node;
                    break;
                }
            }
            
            if (found == null) {
                found = new DefaultMutableTreeNode(part);
                treeModel.insertNodeInto(found, current, current.getChildCount());
            }
            current = found;
        }
        
        return current;
    }

    private static void loadMacro(String macroName) {
        currentMacroName = macroName;
        currentActions = macroManager.getMacro(macroName);
        if (currentActions.isEmpty()) {
            String folderPath = macroManager.getMacroFolder(macroName);
            String fileName = macroName + ".amc";
            File file;
            
            if (folderPath != null && !folderPath.isEmpty()) {
                file = new File("macros", folderPath + "/" + fileName);
            } else {
                file = new File("macros", fileName);
            }
            
            if (!file.exists()) {
                file = findMacroFile(macroName, new File("macros"));
            }
            
            if (file != null && file.exists()) {
                try {
                    MacroFileManager.MacroData data = MacroFileManager.loadMacro(file.getAbsolutePath());
                    currentActions = data.getActions();
                    String actualFolderPath = file.getParentFile().getAbsolutePath();
                    if (actualFolderPath.contains("macros")) {
                        actualFolderPath = actualFolderPath.substring(actualFolderPath.indexOf("macros") + 7);
                        actualFolderPath = actualFolderPath.replace("\\", "/");
                        if (actualFolderPath.startsWith("/")) {
                            actualFolderPath = actualFolderPath.substring(1);
                        }
                    }
                    macroManager.createMacro(macroName, actualFolderPath);
                    macroManager.saveMacro(macroName, data.getDescription(), data.getComment(), data.getSoftware(), data.getActions());
                } catch (Exception e) {
                    System.err.println("Ошибка при загрузке макроса: " + e.getMessage());
                }
            }
        }
        updateActionsList();
    }
    
    private static File findMacroFile(String macroName, File directory) {
        String searchName = macroName;
        if (macroName.contains("/")) {
            searchName = macroName.substring(macroName.lastIndexOf("/") + 1);
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                File found = findMacroFile(macroName, file);
                if (found != null) {
                    return found;
                }
            } else if (file.isFile() && file.getName().equals(searchName + ".amc")) {
                return file;
            }
        }
        return null;
    }

    private static void updateActionsList() {
        listModel.clear();
        for (Action action : currentActions) {
            listModel.addElement(action);
        }
    }

    private static void startRecording() {
        if (recording) {
            stopRecording();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (currentMacroName == null) {
            showCreateMacroDialog();
            if (currentMacroName == null) {
                return;
            }
        }
        
        RecordSettings settings = showRecordSettingsDialog();
        if (settings == null) {
            return;
        }
        
        recordButton.setEnabled(false);
        stopButton.setEnabled(false);
        playButton.setEnabled(false);
        
        new Thread(() -> {
            try {
                JOptionPane.showMessageDialog(frame, 
                    "Запись начнется через 3 секунды...", 
                    "Подготовка к записи", JOptionPane.INFORMATION_MESSAGE);
                
                Thread.sleep(3000);
                
                SwingUtilities.invokeLater(() -> {
                    recording = true;
                    currentActions.clear();
                    boolean started = recorder.start(settings, () -> stopRecording());
                    
                    if (!started) {
                        JOptionPane.showMessageDialog(frame, 
                            "Не удалось запустить запись. JNativeHook не зарегистрирован.", 
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                        recording = false;
                        recordButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        playButton.setEnabled(true);
                        return;
                    }
                    
                    recordButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    playButton.setEnabled(false);
                    updateActionsList();
                    
                    javax.swing.Timer timer = new javax.swing.Timer(200, e -> {
                        if (recording && recorder != null) {
                            currentActions = recorder.getActions();
                            updateActionsList();
                        }
                    });
                    timer.start();
                });
            } catch (InterruptedException e) {
                SwingUtilities.invokeLater(() -> {
                    recording = false;
                    recordButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    playButton.setEnabled(true);
                });
            }
        }).start();
    }
    
    private static RecordSettings showRecordSettingsDialog() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(30, 30, 30));
        
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
        checkBoxPanel.setBackground(new Color(30, 30, 30));
        checkBoxPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JCheckBox keyboardCheck = new JCheckBox("Записывать нажатия клавиш с клавиатуры", true);
        keyboardCheck.setBackground(new Color(30, 30, 30));
        keyboardCheck.setForeground(Color.WHITE);
        
        JCheckBox mouseButtonsCheck = new JCheckBox("Записывать нажатия кнопок мыши", true);
        mouseButtonsCheck.setBackground(new Color(30, 30, 30));
        mouseButtonsCheck.setForeground(Color.WHITE);
        
        JCheckBox absoluteMovementCheck = new JCheckBox("Записывать абсолютное перемещение курсора", true);
        absoluteMovementCheck.setBackground(new Color(30, 30, 30));
        absoluteMovementCheck.setForeground(Color.WHITE);
        
        JCheckBox relativeMovementCheck = new JCheckBox("Записывать относительное перемещение курсора", false);
        relativeMovementCheck.setBackground(new Color(30, 30, 30));
        relativeMovementCheck.setForeground(Color.WHITE);
        
        JCheckBox longPressesCheck = new JCheckBox("Вставлять длительные нажатия", true);
        longPressesCheck.setBackground(new Color(30, 30, 30));
        longPressesCheck.setForeground(Color.WHITE);
        
        checkBoxPanel.add(keyboardCheck);
        checkBoxPanel.add(Box.createVerticalStrut(5));
        checkBoxPanel.add(mouseButtonsCheck);
        checkBoxPanel.add(Box.createVerticalStrut(5));
        checkBoxPanel.add(absoluteMovementCheck);
        checkBoxPanel.add(Box.createVerticalStrut(5));
        checkBoxPanel.add(relativeMovementCheck);
        checkBoxPanel.add(Box.createVerticalStrut(5));
        checkBoxPanel.add(longPressesCheck);
        
        panel.add(checkBoxPanel, BorderLayout.CENTER);
        
        int result = JOptionPane.showConfirmDialog(frame, panel, "Настройки записи", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            return new RecordSettings(
                keyboardCheck.isSelected(),
                mouseButtonsCheck.isSelected(),
                absoluteMovementCheck.isSelected(),
                relativeMovementCheck.isSelected(),
                longPressesCheck.isSelected()
            );
        }
        
        return null;
    }

    private static void stopRecording() {
        if (recording) {
            recording = false;
            currentActions = recorder.stop();
            if (currentMacroName != null) {
                String desc = macroManager.getMacroDescription(currentMacroName);
                String comment = macroManager.getMacroComment(currentMacroName);
                String software = macroManager.getMacroSoftware(currentMacroName);
                macroManager.saveMacro(currentMacroName, desc, comment, software, currentActions);
            }
            updateActionsList();
            recordButton.setEnabled(true);
            stopButton.setEnabled(false);
            playButton.setEnabled(true);
        }
    }

    private static void playMacro() {
        if (currentActions.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Нет действий для воспроизведения", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(frame, 
            "Воспроизведение начнется через 3 секунды. Продолжить?", 
            "Воспроизведение", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    player.play(currentActions);
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(frame, "Ошибка: " + e.getMessage(), 
                            "Ошибка", JOptionPane.ERROR_MESSAGE));
                }
            }).start();
        }
    }

    private static void clearActions() {
        int result = JOptionPane.showConfirmDialog(frame, 
            "Очистить все действия?", "Очистка", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            currentActions.clear();
            if (currentMacroName != null) {
                String desc = macroManager.getMacroDescription(currentMacroName);
                String comment = macroManager.getMacroComment(currentMacroName);
                String software = macroManager.getMacroSoftware(currentMacroName);
                macroManager.saveMacro(currentMacroName, desc, comment, software, currentActions);
            }
            updateActionsList();
        }
    }

    private static void deleteSelectedAction() {
        int selectedIndex = actionsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            currentActions.remove(selectedIndex);
            if (currentMacroName != null) {
                String desc = macroManager.getMacroDescription(currentMacroName);
                String comment = macroManager.getMacroComment(currentMacroName);
                String software = macroManager.getMacroSoftware(currentMacroName);
                macroManager.saveMacro(currentMacroName, desc, comment, software, currentActions);
            }
            updateActionsList();
        }
    }
    
    private static void deleteLastActions() {
        if (currentActions.isEmpty()) {
            return;
        }
        
        String input = JOptionPane.showInputDialog(frame, 
            "Сколько последних действий удалить?", 
            "Удаление действий", 
            JOptionPane.QUESTION_MESSAGE);
        
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        
        try {
            int count = Integer.parseInt(input.trim());
            if (count <= 0) {
                JOptionPane.showMessageDialog(frame, 
                    "Количество должно быть больше 0", 
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (count > currentActions.size()) {
                count = currentActions.size();
            }
            
            for (int i = 0; i < count; i++) {
                currentActions.remove(currentActions.size() - 1);
            }
            
            if (currentMacroName != null) {
                String desc = macroManager.getMacroDescription(currentMacroName);
                String comment = macroManager.getMacroComment(currentMacroName);
                String software = macroManager.getMacroSoftware(currentMacroName);
                macroManager.saveMacro(currentMacroName, desc, comment, software, currentActions);
            }
            updateActionsList();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, 
                "Неверный формат числа", 
                "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void deleteMacro() {
        if (currentMacroName == null) {
            JOptionPane.showMessageDialog(frame, "Нет выбранного макроса", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(frame, 
            "Удалить макрос \"" + currentMacroName + "\"?", 
            "Удаление", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            macroManager.deleteMacro(currentMacroName);
            macroManager.deleteMacroFile(currentMacroName);
            
            DefaultMutableTreeNode node = findMacroNode(currentMacroName);
            if (node != null) {
                treeModel.removeNodeFromParent(node);
            }
            
            currentMacroName = null;
            currentActions.clear();
            updateActionsList();
        }
    }
    
    private static void populateTreeFromFiles() {
        File dir = new File("macros");
        if (!dir.exists()) {
            return;
        }
        
        loadMacrosRecursively(dir, "");
    }
    
    private static void loadMacrosRecursively(File directory, String relativePath) {
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
                    
                    if (!macroManager.macroExists(fullName)) {
                        MacroFileManager.MacroData data = MacroFileManager.loadMacro(file.getAbsolutePath());
                        macroManager.createMacro(fullName, relativePath);
                        macroManager.saveMacro(fullName, data.getDescription(), data.getComment(), data.getSoftware(), data.getActions());
                    }
                    
                    if (!macroNodeExists(fullName)) {
                        addMacroToTree(fullName, relativePath);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при загрузке макроса " + file.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static boolean macroNodeExists(String fullName) {
        String displayName = fullName;
        if (fullName.contains("/")) {
            displayName = fullName.substring(fullName.lastIndexOf("/") + 1);
        }
        
        return findMacroNodeRecursive(rootNode, fullName, displayName) != null;
    }
    
    private static DefaultMutableTreeNode findMacroNode(String macroName) {
        String displayName = macroName;
        if (macroName.contains("/")) {
            displayName = macroName.substring(macroName.lastIndexOf("/") + 1);
        }
        
        return findMacroNodeRecursive(rootNode, macroName, displayName);
    }
    
    private static DefaultMutableTreeNode findMacroNodeRecursive(DefaultMutableTreeNode parent, String fullName, String displayName) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (node.isLeaf()) {
                Object userObj = node.getUserObject();
                if (userObj instanceof MacroTreeNode) {
                    if (fullName.equals(((MacroTreeNode) userObj).fullName)) {
                        return node;
                    }
                } else if (displayName.equals(node.getUserObject().toString())) {
                    return node;
                }
            } else {
                DefaultMutableTreeNode found = findMacroNodeRecursive(node, fullName, displayName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean showLicenseDialog() {
        Color bgColor = new Color(30, 30, 35);
        Color textColor = new Color(240, 240, 245);
        Color buttonColor = new Color(70, 130, 230);
        Color buttonHover = new Color(90, 150, 255);
        
        JDialog dialog = new JDialog((JFrame) null, "Лицензия", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(null);
        dialog.getContentPane().setBackground(bgColor);
        
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("<html><h2 style='text-align: center; color: white;'>Лицензионное соглашение</h2></html>");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(bgColor);
        
        licenseManager.loadLicenseData();
        boolean hasKey = licenseManager.hasLicenseKey();
        int remainingDays = licenseManager.getRemainingTrialDays();
        int licenseDays = licenseManager.getRemainingLicenseDays();
        LicenseManager.LicenseType licenseType = licenseManager.getLicenseType();
        
        JLabel infoLabel;
        if (hasKey && licenseManager.isLicenseValid()) {
            String typeName = licenseManager.getLicenseTypeName();
            String timeText;
            if (licenseDays == Integer.MAX_VALUE) {
                timeText = "бессрочно";
            } else if (licenseType == LicenseManager.LicenseType.TEST_5SEC) {
                timeText = licenseDays + " сек.";
            } else {
                timeText = licenseDays + " дн.";
            }
            infoLabel = new JLabel("<html><div style='text-align: center; color: #98FB98; font-size: 14px;'><b>✅ Лицензия активирована</b></div><div style='text-align: center; color: #ccc; font-size: 12px; margin-top: 10px;'>" + typeName + "<br>Осталось: " + timeText + "</div></html>");
        } else if (remainingDays > 0) {
            infoLabel = new JLabel("<html><div style='text-align: center; color: #FFD700; font-size: 14px;'><b>Пробный период: " + remainingDays + " дн.</b></div><div style='text-align: center; color: #ccc; font-size: 12px; margin-top: 10px;'>Осталось дней бесплатного использования</div></html>");
        } else {
            infoLabel = new JLabel("<html><div style='text-align: center; color: #FF6B6B; font-size: 14px;'><b>⚠ Пробный период истек</b></div><div style='text-align: center; color: #ccc; font-size: 12px; margin-top: 10px;'>Для продолжения работы введите лицензионный ключ</div></html>");
        }
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(infoLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        
        JLabel keyLabel = new JLabel("Лицензионный ключ:");
        keyLabel.setForeground(textColor);
        keyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(keyLabel);
        centerPanel.add(Box.createVerticalStrut(5));
        
        JTextField keyField = new JTextField(30);
        keyField.setBackground(new Color(40, 40, 45));
        keyField.setForeground(textColor);
        keyField.setCaretColor(textColor);
        keyField.setBorder(new EmptyBorder(8, 10, 8, 10));
        keyField.setAlignmentX(Component.CENTER_ALIGNMENT);
        keyField.setMaximumSize(new Dimension(400, 35));
        if (hasKey) {
            keyField.setText(licenseManager.getLicenseKey());
        }
        centerPanel.add(keyField);
        centerPanel.add(Box.createVerticalStrut(20));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(bgColor);
        
        JButton activateButton = createModernButton("Активировать", buttonColor, buttonHover);
        activateButton.setPreferredSize(new Dimension(120, 35));
        activateButton.addActionListener(e -> {
            String key = keyField.getText().trim();
            if (key.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Введите лицензионный ключ", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                licenseManager.setLicenseKey(key);
                licenseManager.loadLicenseData();
                Thread.sleep(100);
                if (licenseManager.isLicenseValid()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dialog, "Лицензия успешно активирована!", "Успех", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        SwingUtilities.invokeLater(() -> {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            showLicenseDialog();
                        });
                    });
                } else {
                    JOptionPane.showMessageDialog(dialog, "Неверный лицензионный ключ", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Ошибка при сохранении лицензии: " + ex.getMessage() + "\nПроверьте права доступа к файлам.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton websiteButton = createModernButton("Посетить сайт", new Color(90, 90, 110), new Color(110, 110, 130));
        websiteButton.setPreferredSize(new Dimension(120, 35));
        websiteButton.addActionListener(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://macros.github.io"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Не удалось открыть сайт", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        buttonPanel.add(activateButton);
        buttonPanel.add(websiteButton);
        
        centerPanel.add(buttonPanel);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(bgColor);
        
        if (remainingDays > 0) {
            JButton continueButton = createModernButton("Продолжить пробный период", new Color(70, 180, 80), new Color(90, 210, 100));
            continueButton.setPreferredSize(new Dimension(200, 35));
            continueButton.addActionListener(e -> dialog.dispose());
            bottomPanel.add(continueButton);
        } else {
            JButton exitButton = createModernButton("Выход", new Color(180, 60, 60), new Color(210, 80, 80));
            exitButton.setPreferredSize(new Dimension(120, 35));
            exitButton.addActionListener(e -> {
                dialog.dispose();
            });
            bottomPanel.add(exitButton);
        }
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (!licenseManager.isLicenseValid() && remainingDays == 0) {
                    System.exit(0);
                }
            }
        });
        
        dialog.setVisible(true);
        
        return licenseManager.isLicenseValid() || remainingDays > 0;
    }

    private static void showLoopDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.setBackground(new Color(30, 30, 30));
        
        JLabel startLabel = new JLabel("С какой строки:");
        startLabel.setForeground(Color.WHITE);
        JTextField startField = new JTextField("1", 10);
        startField.setBackground(new Color(40, 40, 40));
        startField.setForeground(Color.WHITE);
        startField.setCaretColor(Color.WHITE);
        
        JLabel countLabel = new JLabel("Сколько раз:");
        countLabel.setForeground(Color.WHITE);
        JTextField countField = new JTextField("1", 10);
        countField.setBackground(new Color(40, 40, 40));
        countField.setForeground(Color.WHITE);
        countField.setCaretColor(Color.WHITE);
        
        panel.add(startLabel);
        panel.add(startField);
        panel.add(countLabel);
        panel.add(countField);
        
        int result = JOptionPane.showConfirmDialog(frame, panel, "Настройка цикла", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                int startLine = Integer.parseInt(startField.getText()) - 1;
                int count = Integer.parseInt(countField.getText());
                
                if (startLine < 0 || startLine >= currentActions.size()) {
                    JOptionPane.showMessageDialog(frame, "Неверный номер строки", 
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (count <= 0) {
                    JOptionPane.showMessageDialog(frame, "Количество повторов должно быть больше 0", 
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                List<Action> loopActions = new ArrayList<>(
                    currentActions.subList(startLine, currentActions.size()));
                
                long baseTime = currentActions.isEmpty() ? 0 : 
                    currentActions.get(currentActions.size() - 1).getTimestamp();
                
                for (int i = 0; i < count - 1; i++) {
                    long timeOffset = baseTime + (i + 1) * 1000;
                    for (Action action : loopActions) {
                        Action newAction = new Action(action.getType(), 
                            timeOffset + action.getTimestamp());
                        newAction.setX(action.getX());
                        newAction.setY(action.getY());
                        newAction.setButton(action.getButton());
                        newAction.setKeyCode(action.getKeyCode());
                        newAction.setModifiers(action.getModifiers());
                        newAction.setKeyChar(action.getKeyChar());
                        newAction.setStringValue(action.getStringValue());
                        currentActions.add(newAction);
                    }
                }
                
                if (currentMacroName != null) {
                    String desc = macroManager.getMacroDescription(currentMacroName);
                    String comment = macroManager.getMacroComment(currentMacroName);
                    String software = macroManager.getMacroSoftware(currentMacroName);
                    macroManager.saveMacro(currentMacroName, desc, comment, software, currentActions);
                }
                updateActionsList();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Неверный формат числа", 
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
