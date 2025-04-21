import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class VirtualMemoryManagerGUI extends JFrame {

    // --- Configuration & Constants ---
    private static final int MAX_PROCESS_ID = 20;
    private static final int MIN_PROCESS_KB = 512;
    private static final int MAX_PROCESS_KB = 2 * 1024 * 1024; // 2GB
    private static final long WRITE_BACK_DELAY_MS = 400; // Base delay
    private static final int HISTORY_SIZE = 15;
    private static final double THRASHING_THRESHOLD = 0.7;

    // --- UI Colors & Fonts (Approximations) ---
    protected static final Color BG_COLOR = new Color(22, 27, 34); // GitHub Dark Dimmed BG approximation
    protected static final Color SURFACE_COLOR = new Color(34, 40, 49, 200); // Slightly transparent surface
    protected static final Color SURFACE_SOLID_COLOR = new Color(34, 40, 49);
    protected static final Color TEXT_COLOR = new Color(201, 209, 217);
    protected static final Color TEXT_MUTED_COLOR = new Color(139, 148, 158);
    protected static final Color PRIMARY_COLOR = new Color(88, 166, 255); // GitHub Blue
    protected static final Color SECONDARY_COLOR = new Color(163, 113, 247); // GitHub Purple
    protected static final Color SUCCESS_COLOR = new Color(63, 185, 80); // GitHub Green
    protected static final Color WARNING_COLOR = new Color(210, 153, 34); // GitHub Yellow
    protected static final Color ERROR_COLOR = new Color(248, 81, 73); // GitHub Red
    protected static final Color DIRTY_COLOR = ERROR_COLOR;
    protected static final Color HIGHLIGHT_COLOR = new Color(250, 218, 94); // Brighter yellow

    protected static final Font MAIN_FONT = new Font("Poppins", Font.PLAIN, 13); // Use available font
    protected static final Font MONO_FONT = new Font("Fira Code", Font.PLAIN, 12); // Use available font
    protected static final Font BOLD_FONT = MAIN_FONT.deriveFont(Font.BOLD);

    // --- Simulation State ---
    private int ramSize = 4;
    private int swapSize = 4;
    private int cacheSize = 3;
    private List<String> ram = new ArrayList<>();
    private List<String> swap = new ArrayList<>();
    private Map<String, CacheEntry> cache = new LinkedHashMap<>(); // Keep insertion order somewhat for LRU tie-breaking
    private Set<String> dirtyProcesses = new HashSet<>();
    private List<String> accessOrder = new LinkedList<>(); // For LRU/MRU (RAM processes)
    private Map<String, Integer> accessFrequency = new HashMap<>(); // For LFU
    private Map<String, Long> ramAddTime = new HashMap<>(); // For FIFO
    private Map<String, Integer> processSizes = new HashMap<>();
    private List<String> processPool = new ArrayList<>();
    private Stats stats = new Stats();
    private List<String> accessHistory = new LinkedList<>(); // For thrashing detection
    private double simulationSpeedFactor = 1.0; // 1x speed
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SS"); // Include millis

    // --- UI Components ---
    private JSpinner ramSizeSpinner, swapSizeSpinner, cacheSizeSpinner;
    private JComboBox<String> algorithmComboBox;
    protected JTextField processIdInput; // Changed to protected for ProcessBlock access
    private JButton applyConfigButton, aboutButton, allocateButton, accessButton, markDirtyButton;
    private JButton manualToCacheButton, clearCacheButton, resetSimButton;
    private JComboBox<String> simSpeedComboBox;

    private JLabel cacheHitsLabel, cacheAccessesLabel, ramHitsLabel, ramAccessesLabel;
    private JLabel pageFaultsLabel, swapAccessesLabel, tlbHitsLabel, tlbMissesLabel;
    private JLabel totalAccessesLabel, hitRateLabel, faultRateLabel;
    private JLabel thrashingIndicator;

    private MemoryPanel cachePanel, ramPanel, swapPanel;
    private JTextPane logTextPane;
    private StyledTextPane styledLog;
    private JScrollPane logScrollPane;

    private JDialog aboutDialog;

    private JLayeredPane layeredPane; // For animations

    // --- Constructor ---
    public VirtualMemoryManagerGUI() {
        super("Advanced Hybrid Virtual Memory Manager (Java Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1400, 900)); // Initial size
        getContentPane().setBackground(BG_COLOR);
        setLayout(new BorderLayout(10, 10)); // Use BorderLayout

        // Use JLayeredPane for animations
        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(1400, 900));
        setContentPane(layeredPane); // Set layered pane as content pane

        // Main content panel that sits on the default layer
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false); // Make it transparent
        mainPanel.setBounds(0, 0, 1400, 900); // Must set bounds for layered pane children
        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER); // Add to bottom layer


        // Title
        JLabel titleLabel = new JLabel("Advanced Hybrid Virtual Memory Manager");
        titleLabel.setFont(new Font("Poppins", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(20, 10, 10, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Center Area (Controls, Stats, Memory Grid)
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Control Panels
        centerPanel.add(createConfigPanel());
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        centerPanel.add(createControlsPanel());
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        centerPanel.add(createStatsPanel());
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacer

        // Memory Grid Panel
        centerPanel.add(createMemoryGridPanel());

        // Log Panel
        mainPanel.add(createLogPanel(), BorderLayout.SOUTH);

        // Populate process pool
        for (int i = 1; i <= MAX_PROCESS_ID; i++) {
            processPool.add("P" + i);
        }

        createAboutDialog();
        updateSimSpeed(); // Set initial speed factor from combo box
        initSimulation();

        pack(); // Adjust frame size to components
        setLocationRelativeTo(null); // Center on screen
        setVisible(true);

         // Ensure mainPanel resizes with the frame
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                 mainPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
            }
        });
    }

    // --- UI Panel Creation ---

    private JPanel createStyledPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE_COLOR);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BG_COLOR.brighter(), 1), // Approximation of subtle border
                new EmptyBorder(15, 20, 15, 20) // Padding
        ));
        // Rounded corners are hard in standard Swing, skipping for simplicity
        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(BOLD_FONT);
        button.setBackground(bgColor);
        button.setForeground(bgColor.equals(PRIMARY_COLOR) || bgColor.equals(SECONDARY_COLOR) || bgColor.equals(SUCCESS_COLOR) ? Color.BLACK : TEXT_COLOR); // Contrast
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(bgColor.darker()),
                new EmptyBorder(8, 15, 8, 15)
        ));
        // Add hover/pressed effects if desired (more complex)
        return button;
    }

     private JComboBox<String> createStyledComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setFont(MAIN_FONT);
        comboBox.setBackground(SURFACE_SOLID_COLOR);
        comboBox.setForeground(TEXT_COLOR);
        // Styling the arrow and popup requires a custom UI delegate (complex)
        return comboBox;
    }

    private JSpinner createStyledSpinner(int value, int min, int max) {
        SpinnerModel model = new SpinnerNumberModel(value, min, max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.setFont(MAIN_FONT);
        spinner.setBackground(SURFACE_SOLID_COLOR); // Might not work depending on L&F
        spinner.setForeground(TEXT_COLOR);
        spinner.getEditor().getComponent(0).setBackground(SURFACE_SOLID_COLOR);
        spinner.getEditor().getComponent(0).setForeground(TEXT_COLOR);
        spinner.setBorder(new LineBorder(TEXT_MUTED_COLOR));
        return spinner;
    }

     private JTextField createStyledTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setFont(MAIN_FONT);
        textField.setBackground(SURFACE_SOLID_COLOR);
        textField.setForeground(TEXT_COLOR);
        textField.setCaretColor(PRIMARY_COLOR);
        textField.setBorder(new CompoundBorder(
            new LineBorder(TEXT_MUTED_COLOR),
            new EmptyBorder(5, 8, 5, 8)
        ));
        return textField;
    }

    private JPanel createConfigPanel() {
        JPanel panel = createStyledPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        panel.add(new JLabel("RAM:") {{ setForeground(TEXT_MUTED_COLOR); setFont(MAIN_FONT); }});
        ramSizeSpinner = createStyledSpinner(ramSize, 1, 20);
        ramSizeSpinner.setToolTipText("RAM Size (Frames)");
        panel.add(ramSizeSpinner);

        panel.add(new JLabel("Swap:") {{ setForeground(TEXT_MUTED_COLOR); setFont(MAIN_FONT); }});
        swapSizeSpinner = createStyledSpinner(swapSize, 0, 20);
        swapSizeSpinner.setToolTipText("Swap Space Size (Frames)");
        panel.add(swapSizeSpinner);

        panel.add(new JLabel("Cache:") {{ setForeground(TEXT_MUTED_COLOR); setFont(MAIN_FONT); }});
        cacheSizeSpinner = createStyledSpinner(cacheSize, 0, 10);
        cacheSizeSpinner.setToolTipText("Cache Size (Entries)");
        panel.add(cacheSizeSpinner);

        applyConfigButton = createStyledButton("⚙️ Apply & Reset", PRIMARY_COLOR);
        applyConfigButton.setToolTipText("Apply new sizes and reset simulation");
        applyConfigButton.addActionListener(e -> applyConfig());
        panel.add(applyConfigButton);

        aboutButton = createStyledButton("ℹ️ About", SECONDARY_COLOR);
        aboutButton.setToolTipText("Show information about this simulation");
        aboutButton.addActionListener(e -> aboutDialog.setVisible(true));
        panel.add(aboutButton);

        return panel;
    }

    private JPanel createControlsPanel() {
        JPanel panel = createStyledPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 5));

        panel.add(new JLabel("Algorithm:") {{ setForeground(TEXT_MUTED_COLOR); setFont(MAIN_FONT); }});
        algorithmComboBox = createStyledComboBox();
        algorithmComboBox.addItem("FIFO");
        algorithmComboBox.addItem("LRU");
        algorithmComboBox.addItem("LFU");
        algorithmComboBox.addItem("LIFO");
        algorithmComboBox.addItem("MRU");
        algorithmComboBox.addItem("Random");
        algorithmComboBox.setToolTipText("Select page replacement algorithm for RAM eviction");
        panel.add(algorithmComboBox);

        processIdInput = createStyledTextField(10);
        processIdInput.setToolTipText("Enter Process ID (e.g., P5) or leave blank for random selection");
        panel.add(processIdInput);

        allocateButton = createStyledButton("➕ Allocate", PRIMARY_COLOR);
        allocateButton.setToolTipText("Allocate the specified (or random) process to RAM");
        allocateButton.addActionListener(e -> allocateProcess());
        panel.add(allocateButton);

        accessButton = createStyledButton("👆 Access", PRIMARY_COLOR);
        accessButton.setToolTipText("Access the specified (or random) process");
        accessButton.addActionListener(e -> accessProcess());
        panel.add(accessButton);

        markDirtyButton = createStyledButton("✏️ Write/Dirty", SECONDARY_COLOR);
        markDirtyButton.setToolTipText("Mark specified RAM process as 'dirty' (modified)");
        markDirtyButton.addActionListener(e -> markProcessDirty());
        panel.add(markDirtyButton);

        manualToCacheButton = createStyledButton("⚡ To Cache", SECONDARY_COLOR);
        manualToCacheButton.setToolTipText("Manually add process from RAM to Cache");
        manualToCacheButton.addActionListener(e -> manualAddToCache());
        panel.add(manualToCacheButton);

        clearCacheButton = createStyledButton("🧹 Clear Cache", ERROR_COLOR);
        clearCacheButton.setToolTipText("Remove all entries from the cache");
        clearCacheButton.addActionListener(e -> clearCache());
        panel.add(clearCacheButton);

        resetSimButton = createStyledButton("🔄 Reset Sim", ERROR_COLOR);
        resetSimButton.setToolTipText("Reset the entire simulation to initial state");
        resetSimButton.addActionListener(e -> resetSimulation());
        panel.add(resetSimButton);

        panel.add(new JLabel("Speed:") {{ setForeground(TEXT_MUTED_COLOR); setFont(MAIN_FONT); }});
        simSpeedComboBox = createStyledComboBox();
        simSpeedComboBox.addItem("Slow (0.5x)"); // Value = 2.0
        simSpeedComboBox.addItem("Slower (0.75x)");// Value = 1.5
        simSpeedComboBox.addItem("Normal (1x)"); // Value = 1.0
        simSpeedComboBox.addItem("Fast (2x)"); // Value = 0.5
        simSpeedComboBox.addItem("Faster (4x)"); // Value = 0.25
        simSpeedComboBox.addItem("Instant (10x)");// Value = 0.1
        simSpeedComboBox.setSelectedIndex(2); // Default Normal
        simSpeedComboBox.setToolTipText("Adjust simulation animation speed");
        simSpeedComboBox.addActionListener(e -> updateSimSpeed());
        panel.add(simSpeedComboBox);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = createStyledPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5)); // More spacing

        cacheHitsLabel = new JLabel("0");
        cacheAccessesLabel = new JLabel("0");
        ramHitsLabel = new JLabel("0");
        ramAccessesLabel = new JLabel("0");
        pageFaultsLabel = new JLabel("0");
        swapAccessesLabel = new JLabel("0");
        tlbHitsLabel = new JLabel("0");
        tlbMissesLabel = new JLabel("0");
        totalAccessesLabel = new JLabel("0");
        hitRateLabel = new JLabel("N/A");
        faultRateLabel = new JLabel("N/A");

        configureStatsLabel(cacheHitsLabel);
        configureStatsLabel(cacheAccessesLabel);
        configureStatsLabel(ramHitsLabel);
        configureStatsLabel(ramAccessesLabel);
        configureStatsLabel(pageFaultsLabel);
        configureStatsLabel(swapAccessesLabel);
        configureStatsLabel(tlbHitsLabel);
        configureStatsLabel(tlbMissesLabel);
        configureStatsLabel(totalAccessesLabel);
        configureStatsLabel(hitRateLabel);
        configureStatsLabel(faultRateLabel);

        panel.add(createStatsGroup("Cache:", cacheHitsLabel, "H /", cacheAccessesLabel, "Acc |"));
        panel.add(createStatsGroup("RAM:", ramHitsLabel, "H /", ramAccessesLabel, "Acc |"));
        panel.add(createStatsGroup("Swap (Faults):", pageFaultsLabel, "F /", swapAccessesLabel, "Acc |"));
        panel.add(createStatsGroup("TLB:", tlbHitsLabel, "H /", tlbMissesLabel, "M |"));
        panel.add(createStatsGroup("Total Access:", totalAccessesLabel, "|"));
        panel.add(createStatsGroup("Hit Rate:", hitRateLabel, "|"));
        panel.add(createStatsGroup("Fault Rate:", faultRateLabel));

        thrashingIndicator = new JLabel("🚨 THRASHING ALERT!");
        thrashingIndicator.setForeground(ERROR_COLOR);
        thrashingIndicator.setFont(BOLD_FONT);
        thrashingIndicator.setVisible(false);
        panel.add(thrashingIndicator);

        return panel;
    }

    private void configureStatsLabel(JLabel label) {
        label.setForeground(PRIMARY_COLOR);
        label.setFont(BOLD_FONT.deriveFont(14f)); // Slightly larger bold
    }

     private JPanel createStatsGroup(String title, JComponent... components) {
        JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0)); // Tight spacing
        group.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_MUTED_COLOR);
        titleLabel.setFont(MAIN_FONT);
        group.add(titleLabel);
        for (JComponent comp : components) {
            if (comp instanceof JLabel && !comp.getFont().isBold()) { // Style non-stat labels
                 comp.setForeground(TEXT_MUTED_COLOR);
                 comp.setFont(MAIN_FONT);
            }
            group.add(comp);
        }
        return group;
    }
     private JPanel createStatsGroup(String title, String value, String suffix) {
         return createStatsGroup(title, new JLabel(value), new JLabel(suffix));
     }
     private JPanel createStatsGroup(String title, JLabel valueLabel, String suffix) {
         return createStatsGroup(title, valueLabel, new JLabel(suffix));
     }
     private JPanel createStatsGroup(String title, JLabel valueLabel, String suffix1, JLabel valueLabel2, String suffix2) {
          return createStatsGroup(title, valueLabel, new JLabel(suffix1), valueLabel2, new JLabel(suffix2));
     }

    private JPanel createMemoryGridPanel() {
        JPanel gridPanel = new JPanel(new GridLayout(1, 3, 40, 0)); // 1 row, 3 columns, gap 40
        gridPanel.setOpaque(false);
        gridPanel.setBorder(new EmptyBorder(20, 0, 20, 0)); // Top/bottom padding

        cachePanel = new MemoryPanel("⚡ Cache", cacheSize, ProcessBlock.ColorType.CACHE, this);
        ramPanel = new MemoryPanel("💾 Main Memory (RAM)", ramSize, ProcessBlock.ColorType.RAM, this);
        swapPanel = new MemoryPanel("💿 Swap Space", swapSize, ProcessBlock.ColorType.SWAP, this);

        gridPanel.add(cachePanel);
        gridPanel.add(ramPanel);
        gridPanel.add(swapPanel);

        return gridPanel;
    }


    private JPanel createLogPanel() {
        JPanel logContainer = new JPanel(new BorderLayout());
        logContainer.setBackground(SURFACE_COLOR);
        logContainer.setBorder(new CompoundBorder(
                new LineBorder(BG_COLOR.brighter(), 1),
                new EmptyBorder(15, 20, 15, 20)
        ));
        logContainer.setPreferredSize(new Dimension(0, 250)); // Initial height

        JLabel logTitle = new JLabel("System Log");
        logTitle.setFont(BOLD_FONT.deriveFont(16f));
        logTitle.setForeground(PRIMARY_COLOR);
        logTitle.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TEXT_MUTED_COLOR), // Bottom border
                new EmptyBorder(0, 0, 10, 0) // Padding below border
        ));
        logContainer.add(logTitle, BorderLayout.NORTH);

        logTextPane = new JTextPane();
        logTextPane.setEditable(false);
        logTextPane.setBackground(SURFACE_SOLID_COLOR);
        logTextPane.setForeground(TEXT_COLOR);
        logTextPane.setFont(MONO_FONT);
        logTextPane.setMargin(new Insets(5, 5, 5, 5));
        styledLog = new StyledTextPane(logTextPane);

        logScrollPane = new JScrollPane(logTextPane);
        logScrollPane.setBorder(null); // Remove default scrollpane border
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        // Style scrollbar (requires Look and Feel specific code or custom UI)
        logScrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
             @Override protected void configureScrollBarColors() { this.thumbColor = PRIMARY_COLOR; this.trackColor = BG_COLOR; }
             @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
             @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
             private JButton createZeroButton() { JButton jbutton = new JButton(); jbutton.setPreferredSize(new Dimension(0, 0)); jbutton.setMinimumSize(new Dimension(0, 0)); jbutton.setMaximumSize(new Dimension(0, 0)); return jbutton; }
        });

        logContainer.add(logScrollPane, BorderLayout.CENTER);

        return logContainer;
    }

    private void createAboutDialog() {
        aboutDialog = new JDialog(this, "About Advanced Hybrid VMM", true); // Modal
        aboutDialog.setSize(750, 650);
        aboutDialog.setLocationRelativeTo(this);
        aboutDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        aboutDialog.getContentPane().setBackground(SURFACE_SOLID_COLOR);
        aboutDialog.setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(SURFACE_SOLID_COLOR);
        contentPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel titleLabel = new JLabel("About Advanced Hybrid VMM");
        titleLabel.setFont(BOLD_FONT.deriveFont(20f));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, PRIMARY_COLOR),
                new EmptyBorder(0, 0, 15, 0)
        ));
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Use JEditorPane for basic HTML rendering
        JEditorPane infoPane = new JEditorPane();
        infoPane.setContentType("text/html");
        infoPane.setEditable(false);
        infoPane.setOpaque(false); // Inherit background

        // Convert CSS colors to hex for HTML
        String cssTextColor = String.format("#%02x%02x%02x", TEXT_COLOR.getRed(), TEXT_COLOR.getGreen(), TEXT_COLOR.getBlue());
        String cssPrimaryColor = String.format("#%02x%02x%02x", PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue());
        String cssSuccessColor = String.format("#%02x%02x%02x", SUCCESS_COLOR.getRed(), SUCCESS_COLOR.getGreen(), SUCCESS_COLOR.getBlue());
        String cssErrorColor = String.format("#%02x%02x%02x", ERROR_COLOR.getRed(), ERROR_COLOR.getGreen(), ERROR_COLOR.getBlue());
         String cssWarningColor = String.format("#%02x%02x%02x", WARNING_COLOR.getRed(), WARNING_COLOR.getGreen(), WARNING_COLOR.getBlue());
        String cssMutedColor = String.format("#%02x%02x%02x", TEXT_MUTED_COLOR.getRed(), TEXT_MUTED_COLOR.getGreen(), TEXT_MUTED_COLOR.getBlue());


        String aboutHtml = "<html><body style='font-family: SansSerif; font-size: 11pt; color: " + cssTextColor + ";'>"
            + "<p>This simulation demonstrates concepts of a hybrid virtual memory system involving <strong>Cache</strong>, <strong>RAM</strong>, and <strong>Swap Space</strong> with enhanced features and visualizations.</p>"
            + "<p><strong>Key Features:</strong></p>"
            + "<ul style='list-style-type: none; padding-left: 10px;'>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong>Memory Hierarchy:</strong> Visualizes processes in Cache (fast), RAM (medium), or Swap Space (slow).</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong>Process Allocation/Access:</strong> Add/access processes, triggering placement and movement.</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong>Caching:</strong> RAM hits move processes to the Cache (LRU eviction).</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong>Page Replacement Algorithms:</strong> Select FIFO, LRU, LFU, LIFO, MRU, or Random for RAM eviction.</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong><span style='color: " + cssErrorColor + ";'>Dirty Bit</span> Simulation:</strong> Processes in RAM can be marked 'dirty'. Evicting a dirty page simulates a slower write-back to Swap.</li>"
             + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong><span style='color: " + cssWarningColor + ";'>Thrashing Detection:</span></strong> Alerts if the recent page fault rate becomes excessively high.</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong>Process Termination:</strong> Right-click a block to remove it entirely.</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong>Dynamic Configuration & Speed Control:</strong> Adjust sizes and animation speed.</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong>Detailed Info & Stats:</strong> Hover for info; view hits, faults, rates, and conceptual TLB performance.</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span><strong>Advanced Animations:</strong> Visual feedback for movement, state changes, and events.</li>"
            + "</ul>"
            + "<p><strong>Interactions:</strong></p>"
            + "<ul style='list-style-type: none; padding-left: 10px;'>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span>Use controls to configure, allocate, access, mark dirty, or manage memory.</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span>Click a block to select it and trigger an access.</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span>Right-click a block to terminate the process.</li>"
            + "<li style='margin-bottom: 5px;'><span style='color:" + cssSuccessColor + "; margin-right: 5px;'>✓</span>Hover over blocks for detailed information via tooltips.</li>"
            + "</ul>"
            + "<p style='margin-top: 35px; text-align: right; font-style: italic; font-size: 9pt; color: " + cssMutedColor + ";'>Concept By: AALOK KUMAR YADAV</p>"
            + "</body></html>";
        infoPane.setText(aboutHtml);

        JScrollPane scrollPane = new JScrollPane(infoPane);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false); // Make viewport transparent too
        scrollPane.setOpaque(false);

        contentPanel.add(scrollPane);

        // Close Button
        JButton closeButton = createStyledButton("Close", PRIMARY_COLOR);
        closeButton.addActionListener(e -> aboutDialog.setVisible(false));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        buttonPanel.add(closeButton);

        aboutDialog.add(contentPanel, BorderLayout.CENTER);
        aboutDialog.add(buttonPanel, BorderLayout.SOUTH);
    }


    // --- Initialization & Reset ---

    private void initSimulation() {
        ram.clear();
        swap.clear();
        cache.clear();
        dirtyProcesses.clear();
        accessOrder.clear();
        accessFrequency.clear();
        ramAddTime.clear();
        processSizes.clear();
        accessHistory.clear();

        // Reset stats
        stats = new Stats();

        // Generate random sizes
        Random rand = new Random();
        for (String processId : processPool) {
            processSizes.put(processId, MIN_PROCESS_KB + rand.nextInt(MAX_PROCESS_KB - MIN_PROCESS_KB + 1));
        }
        logEvent("Simulation Initialized", "success", "🚀");
        logEvent(String.format("Generated random sizes for %d processes (Range: %s - %s).",
                MAX_PROCESS_ID, formatSize(MIN_PROCESS_KB), formatSize(MAX_PROCESS_KB)), "debug", null);

        updateUIDisplay();
        updateStatsDisplay();
        checkThrashing(true); // Reset indicator
    }

    private void applyConfig() {
        int newRamSize = (Integer) ramSizeSpinner.getValue();
        int newSwapSize = (Integer) swapSizeSpinner.getValue();
        int newCacheSize = (Integer) cacheSizeSpinner.getValue();

        // Validation already handled by JSpinner's model, but good practice
        if (newRamSize < 1 || newSwapSize < 0 || newCacheSize < 0) {
            logEvent("Invalid configuration.", "error", "❌");
            // Reset spinners to current values
            ramSizeSpinner.setValue(ramSize);
            swapSizeSpinner.setValue(swapSize);
            cacheSizeSpinner.setValue(cacheSize);
            return;
        }

        ramSize = newRamSize;
        swapSize = newSwapSize;
        cacheSize = newCacheSize;

        logEvent(String.format("Config Updated: RAM=%d, Swap=%d, Cache=%d. Resetting...",
                ramSize, swapSize, cacheSize), "info", "⚙️");

        // Update panel sizes
        cachePanel.setMaxSize(cacheSize);
        ramPanel.setMaxSize(ramSize);
        swapPanel.setMaxSize(swapSize);

        initSimulation(); // Resets everything and updates display
    }

    private void resetSimulation() {
        logEvent("Simulation Reset", "info", "🔄");
        ramSizeSpinner.setValue(ramSize);
        swapSizeSpinner.setValue(swapSize);
        cacheSizeSpinner.setValue(cacheSize);
        initSimulation();
    }

    // --- Core Logic ---

    private String getProcessIdInput(boolean required, boolean checkExists) {
        String processId = processIdInput.getText().trim().toUpperCase();
        boolean usedFallback = false;

        if (processId.isEmpty() || !processId.matches("P\\d+")) {
            usedFallback = true;
            Set<String> allocated = new HashSet<>(ram);
            allocated.addAll(swap);
            allocated.addAll(cache.keySet());

            List<String> available = processPool.stream()
                    .filter(p -> !allocated.contains(p))
                    .collect(Collectors.toList());
            List<String> allExisting = new ArrayList<>(allocated);

            if (checkExists && !allExisting.isEmpty()) {
                processId = allExisting.get(new Random().nextInt(allExisting.size()));
                logEvent("No valid ID input. Using random existing process: " + processId, "debug", "🎲");
            } else if (!checkExists && !available.isEmpty()) {
                processId = available.get(new Random().nextInt(available.size()));
                logEvent("No valid ID input. Using random available process: " + processId, "debug", "🎲");
            } else if (required) {
                logEvent("Valid Process ID required, but none available/suitable.", "warning", "⚠️");
                processIdInput.requestFocus();
                return null;
            } else {
                logEvent("No valid Process ID and no processes available/suitable.", "warning", "⚠️");
                return null;
            }
        } else {
            try {
                int num = Integer.parseInt(processId.substring(1));
                if (num < 1 || num > MAX_PROCESS_ID) {
                    logEvent("Process ID " + processId + " out of range (P1-P" + MAX_PROCESS_ID + ").", "error", "❌");
                    processIdInput.setText("");
                    return null;
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                logEvent("Invalid Process ID format: " + processId, "error", "❌");
                processIdInput.setText("");
                return null;
            }
        }

        if (!usedFallback) {
            processIdInput.setText(""); // Clear input after successful use
        }
        return processId;
    }

    // --- Actions (Need SwingWorker for delays/animations) ---

    protected void allocateProcess() { // Changed visibility for ProcessBlock
        final String processId = getProcessIdInput(false, false);
        if (processId == null) return;

        if (ram.contains(processId) || swap.contains(processId) || cache.containsKey(processId)) {
            logEvent("Process " + processId + " already allocated.", "warning", "⚠️");
            highlightExistingBlock(processId);
            return;
        }

        logEvent("Allocating Process " + processId + "...", "info", "📥");

        // Run the allocation logic in the background to allow UI updates/animations
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (ram.size() < ramSize) {
                    // Direct Allocation
                    publish("add_ram", processId); // Signal UI update
                    long now = System.currentTimeMillis();
                    updateTrackingOnAdd(processId, now);
                    publish("log_success", "Process " + processId + " allocated to RAM.", "✅");
                    publish("animate_appear", "ram", processId, "fade-in");
                } else {
                    // Eviction Required
                    publish("log_warning", "RAM full. Evicting page via " + getSelectedAlgorithm().toUpperCase() + "...", "⚖️");
                    EvictionResult eviction = evictPage(); // This might involve delays for write-back

                    if (eviction.victimId != null) {
                        if (!eviction.writebackNeeded) { // If writeback happened, anim done there
                             publish("animate_remove", "ram", eviction.victimId, "fade-out");
                             waitSim(300); // Wait for fade out
                        }

                        String evictedDestination;
                        if (swap.size() < swapSize) {
                            evictedDestination = "Swap";
                            publish("add_swap", eviction.victimId);
                            publish("log_info", "Process " + eviction.victimId + " moved to Swap.", "📦");
                            publish("animate_move", "ram", "swap", eviction.victimId);
                        } else {
                            evictedDestination = "Discarded";
                            publish("log_warning", "Process " + eviction.victimId + " evicted & discarded (Swap full).", "🗑️");
                            // Just faded out or write-back animation completed
                        }

                        cleanUpTrackingData(eviction.victimId); // Clean up after move decision

                        // Allocate New Process
                        publish("add_ram", processId);
                        long now = System.currentTimeMillis();
                        updateTrackingOnAdd(processId, now);
                        publish("log_success", "Process " + processId + " allocated to RAM.", "✅");
                        waitSim(getAnimationDuration("move")); // Wait for move anim before appearing
                        publish("animate_appear", "ram", processId, "fade-in");

                    } else {
                        publish("log_error", "Allocation Failed: Could not evict page from RAM.", "❌");
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                // Process UI updates published from doInBackground on the EDT
                handlePublishCommands(chunks);
            }

            @Override
            protected void done() {
                 try {
                    get(); // Check for exceptions during doInBackground
                } catch (InterruptedException | ExecutionException e) {
                    logEvent("Error during allocation: " + e.getMessage(), "error", "🔥");
                    e.printStackTrace();
                } finally {
                     // Final UI update to ensure consistency
                     updateUIDisplay();
                     updateStatsDisplay();
                }
            }
        }.execute();
    }


    protected void accessProcess() { // Changed visibility for ProcessBlock
        final String processId = getProcessIdInput(false, true);
        if (processId == null) return;

        logEvent("Accessing Process " + processId + "...", "info", "🔍");
        stats.totalAccesses++;
        final long now = System.currentTimeMillis();
        String accessType = "miss"; // Default

        // Run in background for potential page fault delays/animations
        new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() throws Exception {
                String resultAccessType = "miss";

                // 1. Check Cache
                if (cache.containsKey(processId)) {
                    stats.cacheHits++; stats.cacheAccesses++; stats.tlbHits++;
                    resultAccessType = "hit";
                    publish("log_success", "Cache Hit: Process " + processId + ".", "🎯");
                    CacheEntry entry = cache.get(processId);
                    entry.lastAccess = now;
                    // cache.put(processId, entry); // Map already holds the reference, just update it
                    publish("animate_highlight", "cache", processId, "highlight");
                    waitSim(getAnimationDuration("highlight"));

                }
                // 2. Check RAM
                else if (ram.contains(processId)) {
                    stats.ramHits++; stats.ramAccesses++; stats.tlbHits++;
                    resultAccessType = "hit";
                    publish("log_success", "RAM Hit: Process " + processId + ". Moving to Cache...", "✅");
                    updateUsageTracking(processId, now); // Update LRU/LFU etc.
                    publish("animate_highlight", "ram", processId, "highlight");
                    waitSim(getAnimationDuration("highlight"));

                    // Add to cache (might involve eviction animation)
                    addToCache(processId, now); // This method uses publish for UI updates internally

                }
                // 3. Check Swap -> Page Fault
                else if (swap.contains(processId)) {
                    stats.pageFaults++; stats.swapAccesses++; stats.tlbMisses++;
                    resultAccessType = "fault";
                    publish("log_warning", "Page Fault: Process " + processId + " in Swap. Loading...", "📉");
                    // Handle page fault, which involves potential RAM eviction and animations
                    handlePageFault(processId, now); // This method uses publish for UI updates internally

                }
                // 4. Not Found
                else {
                    publish("log_error", "Access Failed: Process " + processId + " not found.", "❓");
                    stats.totalAccesses--; // Decrement as it wasn't a valid access
                    resultAccessType = "not_found";
                }
                return resultAccessType;
            }

            @Override
            protected void process(List<String> chunks) {
                handlePublishCommands(chunks);
            }

            @Override
            protected void done() {
                try {
                    String finalAccessType = get();
                    // Update history for thrashing detection
                    if (!finalAccessType.equals("miss") && !finalAccessType.equals("not_found")) {
                        accessHistory.add(finalAccessType.equals("fault") ? "fault" : "hit");
                        if (accessHistory.size() > HISTORY_SIZE) {
                            accessHistory.remove(0); // Keep history size fixed
                        }
                        checkThrashing(false);
                    }
                } catch (InterruptedException | ExecutionException e) {
                     logEvent("Error during access: " + e.getMessage(), "error", "🔥");
                     e.printStackTrace();
                 } finally {
                    // Final UI sync after background task
                    updateUIDisplay();
                    updateStatsDisplay();
                }
            }
        }.execute();
    }

    private void markProcessDirty() {
        final String processId = getProcessIdInput(true, true);
        if (processId == null) return;

        if (ram.contains(processId)) {
             if (dirtyProcesses.contains(processId)) {
                logEvent("Process " + processId + " is already dirty.", "info", "✏️");
                highlightExistingBlock(processId); // Just highlight
            } else {
                // Needs background worker only for the animation delay
                 new SwingWorker<Void, String>() {
                     @Override
                     protected Void doInBackground() throws Exception {
                         dirtyProcesses.add(processId);
                         publish("mark_dirty", processId); // Signal UI to update state
                         publish("log_warning", "Process " + processId + " marked as dirty.", "✏️");
                         publish("animate_state", "ram", processId, "marked-dirty");
                         waitSim(getAnimationDuration("state_change")); // Wait for pulse
                         return null;
                     }
                     @Override
                     protected void process(List<String> chunks) {
                         handlePublishCommands(chunks);
                     }
                     @Override
                     protected void done() {
                          updateUIDisplay(); // Ensure final state is rendered correctly
                     }
                 }.execute();
            }
        } else {
            logEvent("Cannot mark dirty: Process " + processId + " is not in RAM.", "error", "❌");
            highlightExistingBlock(processId); // Highlight where it is
        }
    }


    private void manualAddToCache() {
        final String processId = getProcessIdInput(true, true);
        if (processId == null) return;
        if (cacheSize <= 0) { logEvent("Cache disabled (size 0).", "warning", "⚠️"); return; }

        if (cache.containsKey(processId)) {
             logEvent("Process " + processId + " already in cache. Updating access.", "info", "💡");
             // Run in background just for the highlight animation consistency
             new SwingWorker<Void, String>() {
                  @Override
                  protected Void doInBackground() throws Exception {
                     CacheEntry entry = cache.get(processId);
                     entry.lastAccess = System.currentTimeMillis();
                     publish("animate_highlight", "cache", processId, "highlight");
                     waitSim(getAnimationDuration("highlight"));
                     return null;
                  }
                  @Override protected void process(List<String> chunks) { handlePublishCommands(chunks); }
                   @Override protected void done() { updateUIDisplay(); updateStatsDisplay(); } // Update usage bars etc.
             }.execute();
             return;
        }

         if (ram.contains(processId)) {
            stats.ramAccesses++; // Count as RAM access
            stats.totalAccesses++;
            logEvent("Manually adding " + processId + " (RAM) to Cache...", "info", "➡️");
             // Needs worker because addToCache can have delays/animations
            new SwingWorker<Void, String>() {
                 @Override
                 protected Void doInBackground() throws Exception {
                      long now = System.currentTimeMillis();
                      updateUsageTracking(processId, now); // Update RAM stats (FIFO/LRU/LFU)
                      addToCache(processId, now); // Handles logic and animations via publish
                      return null;
                 }
                 @Override protected void process(List<String> chunks) { handlePublishCommands(chunks); }
                 @Override protected void done() {
                      // Update history for thrashing detection
                      accessHistory.add("hit");
                      if (accessHistory.size() > HISTORY_SIZE) accessHistory.remove(0);
                      checkThrashing(false);
                      updateUIDisplay(); // Final sync
                      updateStatsDisplay();
                 }
            }.execute();

         } else {
             logEvent("Cannot add to Cache: Process " + processId + " not in RAM.", "warning", "⚠️");
             highlightExistingBlock(processId);
         }
    }

    private void clearCache() {
        if (cache.isEmpty()) { logEvent("Cache already empty.", "info", "✅"); return; }
        logEvent("Clearing Cache...", "info", "🧹");

        new SwingWorker<Void, String>() {
             @Override
             protected Void doInBackground() throws Exception {
                  // Create list of keys before iterating as we modify the map
                  List<String> cacheKeys = new ArrayList<>(cache.keySet());
                  for (String key : cacheKeys) {
                      publish("animate_remove", "cache", key, "fade-out");
                  }
                  waitSim(getAnimationDuration("fade")); // Wait for animations to roughly finish

                  publish("clear_cache_data"); // Signal to clear data structure
                  publish("log_success", "Cache Cleared.", "✨");
                  return null;
             }
             @Override protected void process(List<String> chunks) { handlePublishCommands(chunks); }
             @Override protected void done() { updateUIDisplay(); updateStatsDisplay(); } // Update UI
        }.execute();
    }

    // Called when right-clicking a block
    protected void endProcessRequest(String processId) { // Changed visibility for ProcessBlock
        if (processId == null) return;
        logEvent("Terminating Process " + processId + "...", "warning", "❌");

         new SwingWorker<Boolean, String>() {
             @Override
             protected Boolean doInBackground() throws Exception {
                  boolean found = false;
                  String location = null;

                  if (cache.containsKey(processId)) {
                      location = "cache";
                      publish("animate_remove", location, processId, "terminate");
                      publish("remove_cache", processId);
                      found = true;
                  }
                  if (ram.contains(processId)) {
                      if (!found) { // Only animate first found location
                          location = "ram";
                          publish("animate_remove", location, processId, "terminate");
                      }
                      publish("remove_ram", processId);
                      found = true;
                  }
                  if (swap.contains(processId)) {
                       if (!found) { // Only animate first found location
                          location = "swap";
                          publish("animate_remove", location, processId, "terminate");
                      }
                      publish("remove_swap", processId);
                      found = true;
                  }

                  if (found) {
                      waitSim(getAnimationDuration("terminate")); // Wait for animation
                      cleanUpTrackingData(processId); // Clean up all tracking
                      publish("log_success", "Process " + processId + " terminated.", "✔️");
                  } else {
                      publish("log_error", "Termination Failed: Process " + processId + " not found.", "❓");
                  }
                  return found;
             }

             @Override protected void process(List<String> chunks) { handlePublishCommands(chunks); }
             @Override protected void done() { updateUIDisplay(); updateStatsDisplay(); }
         }.execute();
    }


    // --- Helper Functions (May need to run in SwingWorker or publish UI updates) ---

    /**
     * Handles a page fault. MUST be called from a background thread (SwingWorker)
     * as it involves potential delays and animations. Uses publish() for UI updates.
     */
    private void handlePageFault(String processId, long accessTime) throws Exception {
        // 1. Animate removal from Swap & update data
        publish("animate_remove", "swap", processId, "fade-out");
        waitSim(getAnimationDuration("fade")); // Wait for animation
        publish("remove_swap", processId); // Remove from data structure
        publish("log_debug", "Process " + processId + " removed from Swap.", null);

        EvictionResult eviction = new EvictionResult(null, false);
        String evictedProcessId = null;

        // 2. Check for RAM Eviction
        if (ram.size() >= ramSize) {
            publish("log_warning", "RAM full. Evicting page via " + getSelectedAlgorithm().toUpperCase() + "...", "⚖️");
            eviction = evictPage(); // This handles its own logging and animations/delays
            evictedProcessId = eviction.victimId;

            if (evictedProcessId != null) {
                 if (!eviction.writebackNeeded) { // Writeback anim done in evictPage
                     publish("animate_remove", "ram", evictedProcessId, "fade-out");
                     waitSim(getAnimationDuration("fade"));
                 }

                 if (swap.size() < swapSize) {
                     publish("add_swap", evictedProcessId);
                     publish("log_info", "Process " + evictedProcessId + " moved to Swap.", "📦");
                     publish("animate_move", "ram", "swap", evictedProcessId);
                     waitSim(getAnimationDuration("move")); // Wait for move anim
                 } else {
                     publish("log_warning", "Process " + evictedProcessId + " evicted & discarded (Swap full).", "🗑️");
                 }
                 cleanUpTrackingData(evictedProcessId);
            } else {
                publish("log_error", "Page Fault Failed: Could not evict page. " + processId + " load aborted.", "❌");
                // Process is now out of swap, effectively discarded
                return; // Abort the page fault handling
            }
        }

        // 3. Add faulted process to RAM (data structure)
        publish("add_ram", processId);
        updateTrackingOnAdd(processId, accessTime);
        publish("log_success", "Process " + processId + " loaded into RAM.", "✅");

        // 4. Animate Page-In
        publish("animate_move", "swap", "ram", processId); // Visual move
        waitSim(getAnimationDuration("move")); // Wait for move animation

        // 5. Add to cache (potentially)
        addToCache(processId, accessTime); // This handles its own animations/delays
    }

    private static class EvictionResult {
        final String victimId;
        final boolean writebackNeeded;
        EvictionResult(String id, boolean wb) { victimId = id; writebackNeeded = wb; }
    }

    /**
     * Selects and potentially handles write-back for a victim page from RAM.
     * MUST be called from a background thread. Uses publish() for UI updates.
     * @return EvictionResult containing victim ID and whether write-back occurred.
     */
    private EvictionResult evictPage() throws Exception {
        if (ram.isEmpty()) {
            publish("log_debug", "Eviction skipped: RAM empty.", null);
            return new EvictionResult(null, false);
        }

        String algorithm = getSelectedAlgorithm();
        String victimId = null;
        List<String> validRamProcesses = ram.stream()
                .filter(p -> p != null && processPool.contains(p))
                .collect(Collectors.toList());

        if (validRamProcesses.isEmpty()) {
            publish("log_error", "Eviction Failed: No valid processes in RAM.", "❌");
            return new EvictionResult(null, false);
        }

        // --- Algorithm Logic ---
        String candidate = validRamProcesses.get(0); // Default candidate
        switch (algorithm) {
            case "fifo":
                victimId = validRamProcesses.stream()
                        .min(Comparator.comparingLong(p -> ramAddTime.getOrDefault(p, Long.MAX_VALUE)))
                        .orElse(candidate);
                break;
            case "lru":
                 // Iterate accessOrder from oldest to newest
                 for (String p_ordered : accessOrder) {
                      if (validRamProcesses.contains(p_ordered)) {
                          victimId = p_ordered;
                          break;
                      }
                  }
                  if (victimId == null) victimId = candidate; // Fallback
                break;
            case "lfu":
                victimId = validRamProcesses.stream()
                        .min(Comparator.<String, Integer>comparing(p -> accessFrequency.getOrDefault(p, 0))
                                .thenComparingLong(p -> ramAddTime.getOrDefault(p, Long.MAX_VALUE))) // Tie-break with FIFO
                        .orElse(candidate);
                break;
            case "lifo":
                victimId = validRamProcesses.stream()
                        .max(Comparator.comparingLong(p -> ramAddTime.getOrDefault(p, 0L)))
                        .orElse(candidate);
                break;
             case "mru":
                 // Iterate accessOrder from newest to oldest
                 for (int i = accessOrder.size() - 1; i >= 0; i--) {
                      String p_ordered = accessOrder.get(i);
                      if (validRamProcesses.contains(p_ordered)) {
                          victimId = p_ordered;
                          break;
                      }
                  }
                  if (victimId == null) victimId = validRamProcesses.get(validRamProcesses.size() - 1); // Fallback
                 break;
            case "random":
            default:
                victimId = validRamProcesses.get(new Random().nextInt(validRamProcesses.size()));
                break;
        }

        boolean writebackNeeded = false;
        if (victimId != null) {
            publish("log_debug", "Eviction choice (" + algorithm.toUpperCase() + "): Process " + victimId, null);

            // --- Dirty Check ---
            if (dirtyProcesses.contains(victimId)) {
                writebackNeeded = true;
                stats.writeBacks++;
                publish("log_writeback", "Process " + victimId + " is dirty. Simulating Write-Back...", "💾");
                publish("animate_state", "ram", victimId, "write-back");
                waitSim(getAnimationDuration("write_back")); // Simulate write delay + animation
                publish("undirty", victimId); // Clean the bit after write simulation
                publish("log_writeback", "Write-Back complete for " + victimId + ".", "✔️");
            }

            // --- Remove from RAM data structure ---
            publish("remove_ram", victimId);
            // Tracking data cleaned up in calling function after move decision

        } else {
            publish("log_error", "Eviction failed: No victim determined.", "❌");
        }

        return new EvictionResult(victimId, writebackNeeded);
    }

     /**
      * Adds a process to the cache, handling eviction if necessary.
      * MUST be called from a background thread. Uses publish() for UI updates.
      */
     private void addToCache(String processId, long accessTime) throws Exception {
         if (cacheSize <= 0) return; // Cache disabled

         CacheEntry existingEntry = cache.get(processId);
         if (existingEntry != null) {
             existingEntry.lastAccess = accessTime;
             // No need to re-put if using LinkedHashMap for LRU behavior on access
             // cache.put(processId, existingEntry); // Needed if regular HashMap
             publish("log_debug", "Process " + processId + " cache access time updated.", null);
             publish("animate_highlight", "cache", processId, "highlight");
             waitSim(getAnimationDuration("highlight_short"));
             return;
         }

         if (cache.size() >= cacheSize) {
             // Cache Eviction (LRU based on insertion order for LinkedHashMap, or explicit check)
              String lruKey = null;
              long oldestTime = Long.MAX_VALUE;

              // Find LRU entry
              for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
                   if (entry.getValue().lastAccess < oldestTime) {
                       oldestTime = entry.getValue().lastAccess;
                       lruKey = entry.getKey();
                   }
              }

              if (lruKey != null) {
                  publish("log_warning", "Cache full. Evicting " + lruKey + " (LRU).", "💨");
                  publish("animate_remove", "cache", lruKey, "fade-out");
                  waitSim(getAnimationDuration("fade")); // Wait for fade
                  publish("remove_cache", lruKey); // Remove data
              } else if (!cache.isEmpty()) {
                   publish("log_error", "Cache full but failed to find LRU entry.", "❌");
                   return; // Don't add if eviction failed
              }
         }

         // Add the new process if space available (or made available)
          if (cache.size() < cacheSize) {
              publish("add_cache", processId, String.valueOf(accessTime));
              publish("log_success", "Process " + processId + " added to cache.", "💡");
              publish("animate_appear", "cache", processId, "fade-in");
              waitSim(getAnimationDuration("fade"));
          } else {
               publish("log_error", "Internal Error: Failed add to cache post-eviction check.", "❌");
          }
     }


    // --- Data Structure Management (Run on EDT or background, no UI updates here) ---

    private void cleanUpTrackingData(String processId) {
        // Remove from RAM tracking structures
        accessOrder.remove(processId);
        accessFrequency.remove(processId);
        ramAddTime.remove(processId);
        dirtyProcesses.remove(processId); // Ensure dirty bit is cleared

        // Also remove from cache if it exists there (e.g., termination)
        // This is handled by the publish commands like "remove_cache"
    }

    private void updateTrackingOnAdd(String processId, long timestamp) {
        ramAddTime.put(processId, timestamp);
        accessFrequency.put(processId, 1); // Initial access frequency
        accessOrder.remove(processId); // Remove if somehow exists
        accessOrder.add(processId);    // Add to end (most recently added)
        dirtyProcesses.remove(processId); // Process starts clean
    }

    private void updateUsageTracking(String processId, long accessTime) {
        // For LRU/MRU: Move to end of accessOrder list
        accessOrder.remove(processId);
        accessOrder.add(processId);

        // For LFU: Increment frequency count
        accessFrequency.put(processId, accessFrequency.getOrDefault(processId, 0) + 1);

        // FIFO doesn't update on access
    }


    // --- Thrashing Detection ---
     private void checkThrashing(boolean forceReset) {
        SwingUtilities.invokeLater(() -> { // Ensure UI update is on EDT
            if (forceReset || accessHistory.size() < HISTORY_SIZE / 2.0) {
                thrashingIndicator.setVisible(false);
                return;
            }

            long faultsInHistory = accessHistory.stream().filter(a -> a.equals("fault")).count();
            double faultRate = (double) faultsInHistory / accessHistory.size();

            if (faultRate >= THRASHING_THRESHOLD) {
                if (!thrashingIndicator.isVisible()) {
                    logEvent(String.format("High Page Fault Rate (%.0f%%)! Potential Thrashing!", faultRate * 100), "error", "🚨");
                }
                thrashingIndicator.setVisible(true);
                // Simple pulse effect using a Timer
                 Timer pulseTimer = new Timer(500, ae -> thrashingIndicator.setVisible(!thrashingIndicator.isVisible()));
                 pulseTimer.setRepeats(true);
                 // Start pulsing only if not already doing so (crude check)
                 // A better way would be to store the timer reference
                 if (thrashingIndicator.getClientProperty("pulsing") == null) {
                      thrashingIndicator.putClientProperty("pulsing", pulseTimer);
                      pulseTimer.start();
                 }

            } else {
                Object timerProp = thrashingIndicator.getClientProperty("pulsing");
                if (timerProp instanceof Timer) {
                    ((Timer) timerProp).stop();
                    thrashingIndicator.putClientProperty("pulsing", null);
                }
                 thrashingIndicator.setVisible(false);
            }
        });
    }

    // --- SwingWorker Command Processing ---

    /** Handles commands published from background threads to update the UI safely. */
     private void handlePublishCommands(List<String> commands) {
         SwingUtilities.invokeLater(() -> { // Ensure execution on EDT
             for (int i = 0; i < commands.size(); ) {
                 String command = commands.get(i);
                 try {
                     switch (command) {
                         case "log_info":
                         case "log_success":
                         case "log_warning":
                         case "log_error":
                         case "log_debug":
                         case "log_writeback":
                             logEvent(commands.get(i + 1), command.substring(4), commands.get(i + 2)); // message, type, icon
                             i += 3;
                             break;
                         case "add_ram":
                             ram.add(commands.get(i + 1));
                             updateUIDisplay("ram");
                             i += 2;
                             break;
                         case "add_swap":
                             swap.add(commands.get(i + 1));
                             updateUIDisplay("swap");
                             i += 2;
                             break;
                          case "add_cache":
                              String pidCache = commands.get(i + 1);
                              long timeCache = Long.parseLong(commands.get(i + 2));
                              cache.put(pidCache, new CacheEntry("Data for " + pidCache, timeCache));
                              updateUIDisplay("cache");
                              i += 3;
                              break;
                         case "remove_ram":
                             ram.remove(commands.get(i + 1));
                             updateUIDisplay("ram");
                             i += 2;
                             break;
                         case "remove_swap":
                             swap.remove(commands.get(i + 1));
                             updateUIDisplay("swap");
                             i += 2;
                             break;
                         case "remove_cache":
                              cache.remove(commands.get(i + 1));
                              updateUIDisplay("cache");
                              i += 2;
                              break;
                         case "clear_cache_data":
                             cache.clear();
                             updateUIDisplay("cache");
                             i += 1;
                             break;
                         case "mark_dirty":
                             dirtyProcesses.add(commands.get(i+1));
                             updateUIDisplay("ram"); // Redraw RAM to show dirty state
                             i += 2;
                             break;
                         case "undirty":
                              dirtyProcesses.remove(commands.get(i + 1));
                              updateUIDisplay("ram");
                              i += 2;
                              break;
                         case "animate_highlight":
                             animateHighlight(commands.get(i + 1), commands.get(i + 2), "highlight");
                             i += 3;
                             break;
                         case "animate_appear":
                             // Appearance animation often happens implicitly with add + updateUIDisplay
                             // Could add an explicit fade-in timer here if needed
                             animateBlockEffect(commands.get(i + 1), commands.get(i + 2), commands.get(i + 3));
                             i += 4;
                             break;
                         case "animate_remove":
                             animateBlockEffect(commands.get(i + 1), commands.get(i + 2), commands.get(i + 3));
                             i += 4;
                             break;
                         case "animate_state":
                             animateBlockEffect(commands.get(i + 1), commands.get(i + 2), commands.get(i + 3));
                             i += 4;
                             break;
                         case "animate_move":
                             animateBlockMove(commands.get(i + 1), commands.get(i + 2), commands.get(i + 3));
                             i += 4;
                             break;
                         default:
                             System.err.println("Unknown command: " + command);
                             i++; // Skip unknown command
                             break;
                     }
                 } catch (IndexOutOfBoundsException e) {
                     System.err.println("Command parameter error for: " + command);
                     e.printStackTrace();
                     break; // Stop processing this batch on error
                 } catch (Exception e) {
                      System.err.println("Error processing command '" + command + "': " + e.getMessage());
                      e.printStackTrace();
                       break;
                 }
             }
             // Maybe a final update after processing batch? Usually handled by individual commands
             // updateUIDisplay();
             updateStatsDisplay();
         });
     }


    // --- Display & Animation ---

    private void updateUIDisplay(String... sectionsToUpdate) {
        SwingUtilities.invokeLater(() -> { // Ensure UI updates happen on the EDT
            List<String> sections = (sectionsToUpdate == null || sectionsToUpdate.length == 0)
                    ? Arrays.asList("cache", "ram", "swap")
                    : Arrays.asList(sectionsToUpdate);

            if (sections.contains("cache")) cachePanel.updateBlocks(new ArrayList<>(cache.keySet()), dirtyProcesses);
            if (sections.contains("ram")) ramPanel.updateBlocks(ram, dirtyProcesses);
            if (sections.contains("swap")) swapPanel.updateBlocks(swap, dirtyProcesses);

            // Update progress and counts
             cachePanel.updateUsage(cache.size());
             ramPanel.updateUsage(ram.size());
             swapPanel.updateUsage(swap.size());

            // Revalidate and repaint the containing panels
            cachePanel.revalidate(); cachePanel.repaint();
            ramPanel.revalidate(); ramPanel.repaint();
            swapPanel.revalidate(); swapPanel.repaint();

            // It's often necessary to revalidate/repaint the parent container too
             cachePanel.getParent().revalidate();
             cachePanel.getParent().repaint();
        });
    }

    // Get the MemoryPanel for a given location string
    private MemoryPanel getMemoryPanel(String location) {
        switch(location.toLowerCase()) {
            case "cache": return cachePanel;
            case "ram": return ramPanel;
            case "swap": return swapPanel;
            default: return null;
        }
    }

    // --- Logging ---
     private void logEvent(String message, String type, String icon) {
         SwingUtilities.invokeLater(() -> { // Ensure log updates are on EDT
             String time = timeFormat.format(new Date());
             Color color = TEXT_COLOR;
             String logIcon = (icon != null) ? icon : "";

             switch (type) {
                 case "success": color = SUCCESS_COLOR; if (icon == null) logIcon = "✅"; break;
                 case "warning": color = WARNING_COLOR; if (icon == null) logIcon = "⚠️"; break;
                 case "error": color = ERROR_COLOR; if (icon == null) logIcon = "❌"; break;
                 case "debug": color = TEXT_MUTED_COLOR; if (icon == null) logIcon = "🐞"; break;
                 case "writeback": color = SECONDARY_COLOR; if (icon == null) logIcon = "💾"; break;
                 case "info": default: color = PRIMARY_COLOR; if (icon == null) logIcon = "ℹ️"; break;
             }

             styledLog.append(logIcon + " ", color, true); // Icon bold
             styledLog.append("[" + time + "] ", TEXT_MUTED_COLOR, false);
             styledLog.append(message + "\n", color, false);

             // Auto-scroll
             logTextPane.setCaretPosition(logTextPane.getDocument().getLength());
         });
     }

    // --- Animation Helpers ---

     private long getAnimationDuration(String type) {
         long baseDuration = 500; // Default ms
         switch(type) {
             case "fade": baseDuration = 300; break;
             case "highlight":
             case "write_back":
                  baseDuration = 700; break;
             case "highlight_short": baseDuration = 250; break;
             case "state_change": baseDuration = 500; break;
             case "terminate": baseDuration = 400; break;
             case "move": baseDuration = 1000; break; // Longer for move
             default: break;
         }
          // Adjust based on simulation speed factor
          double speed = (simulationSpeedFactor <= 0.1 && baseDuration > 0) ? 0.1 : simulationSpeedFactor;
          return Math.max(10, (long)(baseDuration * speed)); // Ensure minimum duration
     }

     // Simplified wait respecting simulation speed (use only in SwingWorker background thread)
     private void waitSim(long durationMs) throws InterruptedException {
         if (durationMs <= 0) return;
         Thread.sleep(durationMs);
     }

     // Highlight animation (runs on EDT)
     private void animateHighlight(String location, String processId, String type) {
         MemoryPanel panel = getMemoryPanel(location);
         if (panel == null) return;
         ProcessBlock block = panel.findBlock(processId);
         if (block == null) return;

         Color originalBorder = block.getBorderColor();
         Color animColor = HIGHLIGHT_COLOR; // Default highlight
          if ("write-back".equals(type)) animColor = SECONDARY_COLOR;
          else if ("marked-dirty".equals(type)) animColor = DIRTY_COLOR;

         final Color finalAnimColor = animColor; // Effective final for lambda
         final int steps = 10;
         final long duration = getAnimationDuration("highlight");
         final long stepDelay = Math.max(1, duration / (steps * 2)); // Pulse in/out

         block.setBorderColor(finalAnimColor, 3); // Set thick border immediately

         Timer timer = new Timer((int)stepDelay, null);
         timer.setRepeats(true);
         final int[] currentStep = {0};

         timer.addActionListener(e -> {
             currentStep[0]++;
             float fraction;
             if (currentStep[0] <= steps) { // Pulse in (brighten border?)
                 fraction = (float) currentStep[0] / steps;
                 // Optionally interpolate color brightness here
             } else { // Pulse out
                 fraction = (float) (steps * 2 - currentStep[0]) / steps;
             }

             // Simple border thickness pulse for now
             int thickness = 1 + Math.round(2 * fraction); // Varies between 1 and 3
             block.setBorderColor(finalAnimColor, thickness + 1); // Use thickness+1 for visibility

             if (currentStep[0] >= steps * 2) {
                 timer.stop();
                 block.setBorderColor(originalBorder); // Restore original border
                 block.resetBorderThickness();
             }
         });
         timer.start();
     }

      // Generic effect animation (fade, terminate, state pulses) - runs on EDT
     private void animateBlockEffect(String location, String processId, String effectType) {
          MemoryPanel panel = getMemoryPanel(location);
          if (panel == null) return;
          ProcessBlock block = panel.findBlock(processId);
          if (block == null) return;

          long duration = getAnimationDuration(effectType.contains("fade") ? "fade" : effectType.contains("terminate") ? "terminate" : "state_change");
          if (duration < 50) { // Skip animation if too fast
               if (effectType.contains("fade") || effectType.contains("terminate")) {
                    block.setVisible(false); // Just hide it
               }
               return;
          }

           final int steps = 15;
           final long stepDelay = Math.max(1, duration / steps);
           Timer timer = new Timer((int)stepDelay, null);
           timer.setRepeats(true);
           final int[] currentStep = {0};
           final float startAlpha = 1.0f; // Assuming block starts visible

           if (effectType.contains("fade") || effectType.contains("terminate")) {
                // Fade Out / Terminate
                timer.addActionListener(e -> {
                     currentStep[0]++;
                     float fraction = (float) currentStep[0] / steps;
                     float alpha = Math.max(0f, startAlpha - fraction);
                     block.setAlpha(alpha); // ProcessBlock needs setAlpha method

                     if (currentStep[0] >= steps) {
                          timer.stop();
                          block.setVisible(false); // Hide completely after fade
                          block.setAlpha(1.0f); // Reset alpha for potential reuse
                          updateUIDisplay(location); // Remove from layout after animation
                     }
                });

           } else if (effectType.contains("write-back") || effectType.contains("marked-dirty")) {
                // Pulse effect (e.g., border flash) - handled by animateHighlight now
                 animateHighlight(location, processId, effectType);
                 return; // Return early as highlight handles it
           } else {
               // Other effects (e.g., simple fade-in if block was hidden)
                 // Assumes block added but initially set to alpha 0 or invisible
                 block.setAlpha(0f);
                 block.setVisible(true);
                 timer.addActionListener(e -> {
                     currentStep[0]++;
                     float fraction = (float) currentStep[0] / steps;
                     float alpha = Math.min(1.0f, fraction);
                     block.setAlpha(alpha);

                     if (currentStep[0] >= steps) {
                          timer.stop();
                          block.setAlpha(1.0f); // Ensure fully visible
                     }
                 });
           }
            timer.start();
      }

     // Movement Animation (runs on EDT, complex)
      private void animateBlockMove(String fromLocation, String toLocation, String processId) {
           MemoryPanel fromPanel = getMemoryPanel(fromLocation);
           MemoryPanel toPanel = getMemoryPanel(toLocation);
           if (fromPanel == null || toPanel == null) {
                logEvent("Move Anim Failed: Invalid location(s).", "error", "❌");
                return;
           }

           ProcessBlock sourceBlock = fromPanel.findBlock(processId);
           if (sourceBlock == null) {
                // This can happen if the source data was updated before the animation command arrived
                logEvent("Move Anim Warning: Source block " + processId + " in " + fromLocation + " not found visually.", "warning", "❓");
                // Attempt to force consistency
                updateUIDisplay(fromLocation, toLocation);
                return;
           }

           // 1. Get Start Position (relative to layeredPane)
           Point startPoint = SwingUtilities.convertPoint(sourceBlock, 0, 0, layeredPane);
           Dimension blockSize = sourceBlock.getSize();

           // 2. Create Temporary Moving Component (Label with snapshot or simple colored rect)
           // Using a simpler representation for the moving block:
            JLabel movingLabel = new JLabel(processId, SwingConstants.CENTER);
            movingLabel.setOpaque(true);
            movingLabel.setFont(sourceBlock.getFont());
            movingLabel.setForeground(sourceBlock.getForeground());
            movingLabel.setBackground(sourceBlock.getBackground()); // Use block's current bg
            movingLabel.setBorder(sourceBlock.getBorder()); // Copy border
            movingLabel.setBounds(startPoint.x, startPoint.y, blockSize.width, blockSize.height);
            movingLabel.setPreferredSize(blockSize);

           // 3. Hide Original Block
           sourceBlock.setVisible(false); // Hide original temporarily

           // 4. Calculate Approximate End Position
           // This is tricky because the target panel's layout might change.
           // We estimate based on the current number of blocks in the target.
           int targetIndex = 0;
           if (toLocation.equals("cache")) targetIndex = cache.size(); // Size *before* it's added visually
           else if (toLocation.equals("ram")) targetIndex = ram.size();
           else if (toLocation.equals("swap")) targetIndex = swap.size();

            Point targetPanelOrigin = SwingUtilities.convertPoint(toPanel.getBlockContainer(), 0, 0, layeredPane);
            int blockYSpacing = blockSize.height + 5; // Approximate spacing including gap
            // Calculate vertical offset within the container + padding etc.
            int targetYOffset = toPanel.getBlockContainer().getInsets().top + (targetIndex * blockYSpacing);
            // Center horizontally within the target block container
            int targetX = targetPanelOrigin.x + (toPanel.getBlockContainer().getWidth() / 2) - (blockSize.width / 2);
            int targetY = targetPanelOrigin.y + targetYOffset;


           // 5. Add Temp Component to Layered Pane (top layer)
           layeredPane.add(movingLabel, JLayeredPane.DRAG_LAYER); // Use DRAG_LAYER or higher

           // 6. Animation Timer
           long duration = getAnimationDuration("move");
           if (duration < 50) { // Instant move if too fast
                layeredPane.remove(movingLabel);
                updateUIDisplay(fromLocation, toLocation); // Update final state
                layeredPane.repaint();
                return;
           }

            final long startTime = System.currentTimeMillis();
            final Point finalTargetPoint = new Point(targetX, targetY); // Use final vars for lambda

            Timer timer = new Timer(15, null); // ~60 FPS updates
            timer.addActionListener(e -> {
                 long now = System.currentTimeMillis();
                 long elapsed = now - startTime;
                 float fraction = Math.min(1.0f, (float) elapsed / duration);

                 // Simple linear interpolation (easing functions can be added)
                 int currentX = startPoint.x + (int) (fraction * (finalTargetPoint.x - startPoint.x));
                 int currentY = startPoint.y + (int) (fraction * (finalTargetPoint.y - startPoint.y));

                 movingLabel.setLocation(currentX, currentY);

                 if (fraction >= 1.0f) {
                      timer.stop();
                      layeredPane.remove(movingLabel);
                      // Ensure the actual block is visible in the target panel
                      updateUIDisplay(fromLocation, toLocation); // Update data and redraw
                       // Optional: Add a little "settle" animation to the actual block
                       MemoryPanel targetMemPanel = getMemoryPanel(toLocation);
                       if(targetMemPanel != null) {
                           ProcessBlock finalBlock = targetMemPanel.findBlock(processId);
                           if (finalBlock != null) {
                                finalBlock.setVisible(true); // Make sure it's visible
                               // Could add a brief highlight or scale effect here
                           }
                       }
                      layeredPane.repaint(); // Repaint to remove artifacts
                 }
            });
            timer.start();
      }


     private void highlightExistingBlock(String processId) {
         // Use SwingWorker to run highlight animation off the EDT
         new SwingWorker<Void, String>() {
             @Override
             protected Void doInBackground() throws Exception {
                 if (cache.containsKey(processId)) {
                     publish("animate_highlight", "cache", processId, "highlight");
                 } else if (ram.contains(processId)) {
                     publish("animate_highlight", "ram", processId, "highlight");
                 } else if (swap.contains(processId)) {
                     publish("animate_highlight", "swap", processId, "highlight");
                 } else {
                      publish("log_debug", "Highlight Failed: " + processId + " not found.", null);
                 }
                 waitSim(getAnimationDuration("highlight"));
                 return null;
             }
             @Override protected void process(List<String> chunks) { handlePublishCommands(chunks); }
         }.execute();
     }


    // --- Stats Display ---

    private void updateStatsDisplay() {
         SwingUtilities.invokeLater(() -> { // Ensure UI updates happen on the EDT
            cacheHitsLabel.setText(String.valueOf(stats.cacheHits));
            cacheAccessesLabel.setText(String.valueOf(stats.cacheAccesses));
            ramHitsLabel.setText(String.valueOf(stats.ramHits));
            ramAccessesLabel.setText(String.valueOf(stats.ramAccesses));
            pageFaultsLabel.setText(String.valueOf(stats.pageFaults));
            swapAccessesLabel.setText(String.valueOf(stats.swapAccesses));
            tlbHitsLabel.setText(String.valueOf(stats.tlbHits));
            tlbMissesLabel.setText(String.valueOf(stats.tlbMisses));
            totalAccessesLabel.setText(String.valueOf(stats.totalAccesses));

            long totalMemAccesses = stats.cacheAccesses + stats.ramAccesses + stats.swapAccesses;
            long totalMemHits = stats.cacheHits + stats.ramHits;
            String hitRateStr = "N/A";
            String faultRateStr = "N/A";

            if (totalMemAccesses > 0) {
                hitRateStr = String.format("%.1f%%", (double) totalMemHits * 100.0 / totalMemAccesses);
                faultRateStr = String.format("%.1f%%", (double) stats.pageFaults * 100.0 / totalMemAccesses);
            }

            hitRateLabel.setText(hitRateStr);
            faultRateLabel.setText(faultRateStr);

             // Update usage text/progress (might be redundant if updateUIDisplay called, but safe)
             cachePanel.updateUsage(cache.size());
             ramPanel.updateUsage(ram.size());
             swapPanel.updateUsage(swap.size());
         });
    }

    // --- Simulation Speed ---

    private void updateSimSpeed() {
         int selectedIndex = simSpeedComboBox.getSelectedIndex();
         switch (selectedIndex) {
             case 0: simulationSpeedFactor = 2.0; break;   // Slow (0.5x)
             case 1: simulationSpeedFactor = 1.5; break;   // Slower (0.75x)
             case 2: simulationSpeedFactor = 1.0; break;   // Normal (1x)
             case 3: simulationSpeedFactor = 0.5; break;   // Fast (2x)
             case 4: simulationSpeedFactor = 0.25; break;  // Faster (4x)
             case 5: simulationSpeedFactor = 0.1; break;   // Instant (10x)
             default: simulationSpeedFactor = 1.0;
         }
         logEvent("Simulation speed set to " + simSpeedComboBox.getSelectedItem(), "info", "⏱️");
     }


    // --- Utilities ---

    private String getSelectedAlgorithm() {
        return ((String) algorithmComboBox.getSelectedItem()).toLowerCase();
    }

    private String formatSize(int kb) {
        if (kb >= 1024 * 1024) return String.format("%.1f GB", kb / (1024.0 * 1024.0));
        if (kb >= 1024) return String.format("%.1f MB", kb / 1024.0);
        return kb + " KB";
    }

    // Tooltip generation - protected for MemoryPanel access
     protected String generateTooltipText(String processId, String location) {
         int sizeInKB = processSizes.getOrDefault(processId, 0);
         String formattedSize = formatSize(sizeInKB);
         StringBuilder tooltip = new StringBuilder("<html><body style='font-family: SansSerif; font-size: 9pt;'>");
         tooltip.append("<b>").append(processId).append("</b><br>");
         tooltip.append("<span style='color: #").append(String.format("%06x", TEXT_MUTED_COLOR.getRGB() & 0xFFFFFF)).append(";'>Size:</span> ").append(formattedSize);

         if ("ram".equalsIgnoreCase(location)) {
             int freq = accessFrequency.getOrDefault(processId, 0);
             String added = ramAddTime.containsKey(processId) ? timeFormat.format(new Date(ramAddTime.get(processId))) : "-";
             boolean isDirty = dirtyProcesses.contains(processId);
             tooltip.append("<br><span style='color: #").append(String.format("%06x", TEXT_MUTED_COLOR.getRGB() & 0xFFFFFF)).append(";'>Freq:</span> ").append(freq);
             tooltip.append("<br><span style='color: #").append(String.format("%06x", TEXT_MUTED_COLOR.getRGB() & 0xFFFFFF)).append(";'>Added:</span> ").append(added);
             tooltip.append("<br><span style='color: #").append(String.format("%06x", TEXT_MUTED_COLOR.getRGB() & 0xFFFFFF)).append(";'>State:</span> ");
             if (isDirty) {
                 tooltip.append("<span style='color: #").append(String.format("%06x", ERROR_COLOR.getRGB() & 0xFFFFFF)).append(";'><b>Dirty</b></span>");
             } else {
                 tooltip.append("Clean");
             }
         } else if ("cache".equalsIgnoreCase(location)) {
             CacheEntry cacheEntry = cache.get(processId);
             String accessed = (cacheEntry != null) ? timeFormat.format(new Date(cacheEntry.lastAccess)) : "-";
             tooltip.append("<br><span style='color: #").append(String.format("%06x", TEXT_MUTED_COLOR.getRGB() & 0xFFFFFF)).append(";'>Cache Access:</span> ").append(accessed);
         } else if ("swap".equalsIgnoreCase(location)) {
             tooltip.append("<br><span style='color: #").append(String.format("%06x", TEXT_MUTED_COLOR.getRGB() & 0xFFFFFF)).append(";'>Location:</span> Swap");
         }

         tooltip.append("</body></html>");
         return tooltip.toString();
     }

    // --- Main Method ---
    public static void main(String[] args) {
        // Set a modern Look and Feel if available (e.g., Nimbus)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, use the default L&F.
            System.err.println("Nimbus L&F not found, using default.");
        }

        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(VirtualMemoryManagerGUI::new);
    }
}
