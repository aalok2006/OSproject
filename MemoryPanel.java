import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

class MemoryPanel extends JPanel {
    private final JLabel titleLabel;
    private final JLabel usageLabel;
    private final JProgressBar progressBar;
    private final JPanel blockContainer;
    private final String titleBase;
    private final VirtualMemoryManagerGUI controller; // Need controller ref for block creation
    private int maxSize;
    private final ProcessBlock.ColorType colorType;

    public MemoryPanel(String title, int initialMaxSize, ProcessBlock.ColorType type, VirtualMemoryManagerGUI guiController) {
        this.titleBase = title;
        this.maxSize = initialMaxSize;
        this.colorType = type;
        this.controller = guiController; // Directly pass the controller

        setLayout(new BorderLayout(5, 10));
        setOpaque(false); // Transparent background
        setBorder(new EmptyBorder(10, 10, 10, 10)); // Padding

        // Title
        titleLabel = new JLabel(title);
        titleLabel.setFont(VirtualMemoryManagerGUI.BOLD_FONT.deriveFont(16f));
        titleLabel.setForeground(VirtualMemoryManagerGUI.PRIMARY_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, VirtualMemoryManagerGUI.PRIMARY_COLOR), // Bottom border
                new EmptyBorder(0, 0, 10, 0) // Padding below border
        ));
        add(titleLabel, BorderLayout.NORTH);

        // Center contains usage/progress and blocks
        JPanel centerContent = new JPanel(new BorderLayout(5, 5));
        centerContent.setOpaque(false);

        // Usage/Progress Bar Panel
        JPanel usagePanel = new JPanel(new BorderLayout(10, 0));
        usagePanel.setOpaque(false);
        usageLabel = new JLabel("0/" + maxSize + " Used");
        usageLabel.setFont(VirtualMemoryManagerGUI.MAIN_FONT.deriveFont(11f));
        usageLabel.setForeground(VirtualMemoryManagerGUI.TEXT_MUTED_COLOR);
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(false); // Don't paint percentage text
        progressBar.setPreferredSize(new Dimension(100, 10)); // Set height
        // Style progress bar (basic)
        progressBar.setForeground(getProgressColor());
        progressBar.setBackground(VirtualMemoryManagerGUI.BG_COLOR.brighter());
        progressBar.setBorderPainted(false);

        usagePanel.add(usageLabel, BorderLayout.WEST);
        usagePanel.add(progressBar, BorderLayout.CENTER);
        centerContent.add(usagePanel, BorderLayout.NORTH);

        // Block Container
        blockContainer = new JPanel();
        blockContainer.setLayout(new BoxLayout(blockContainer, BoxLayout.Y_AXIS)); // Stack blocks vertically
        blockContainer.setOpaque(false);
        blockContainer.setBorder(new EmptyBorder(10, 0, 0, 0)); // Padding above blocks

        // Use a JScrollPane to handle overflow if many blocks
        JScrollPane scrollPane = new JScrollPane(blockContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Style scrollbar
         scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
             @Override protected void configureScrollBarColors() { this.thumbColor = VirtualMemoryManagerGUI.PRIMARY_COLOR; this.trackColor = VirtualMemoryManagerGUI.BG_COLOR; }
             @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
             @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
             private JButton createZeroButton() { JButton jbutton = new JButton(); jbutton.setPreferredSize(new Dimension(0, 0)); jbutton.setMinimumSize(new Dimension(0, 0)); jbutton.setMaximumSize(new Dimension(0, 0)); return jbutton; }
        });


        centerContent.add(scrollPane, BorderLayout.CENTER);
        add(centerContent, BorderLayout.CENTER);

        updateUsage(0); // Initial update
    }

    // Removed findController() as controller is passed in constructor now

    public JPanel getBlockContainer() {
        return blockContainer;
    }

    public void setMaxSize(int newSize) {
        this.maxSize = Math.max(0, newSize); // Ensure non-negative
        updateUsage(blockContainer.getComponentCount()); // Update label/progress
    }

    public void updateBlocks(List<String> processIds, Set<String> dirtySet) {
        blockContainer.removeAll(); // Clear existing blocks

        for (String pid : processIds) {
            if (pid != null) {
                 boolean isDirty = (colorType == ProcessBlock.ColorType.RAM) && dirtySet.contains(pid);
                 // Pass the controller instance when creating ProcessBlock
                 ProcessBlock block = new ProcessBlock(pid, colorType, isDirty, controller);
                 blockContainer.add(block);
                 blockContainer.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing between blocks
            }
        }
        // Fill remaining space with placeholders (optional, helps maintain size)
        /*
        int currentCount = processIds.size();
        for (int i = currentCount; i < maxSize; i++) {
            JPanel placeholder = new JPanel();
            placeholder.setPreferredSize(new Dimension(100, 42)); // Same size as block
            placeholder.setOpaque(false);
             placeholder.setBorder(new LineBorder(Color.DARK_GRAY)); // Visual indication
            blockContainer.add(placeholder);
            blockContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        */

        blockContainer.revalidate();
        blockContainer.repaint();
        updateUsage(processIds.size());
    }

    public void updateUsage(int currentCount) {
        usageLabel.setText(currentCount + "/" + maxSize + " Used");
        int percentage = (maxSize > 0) ? (int) Math.round(((double) currentCount / maxSize) * 100.0) : 0;
        progressBar.setValue(percentage);
    }

    public ProcessBlock findBlock(String processId) {
        for (Component comp : blockContainer.getComponents()) {
            if (comp instanceof ProcessBlock) {
                ProcessBlock block = (ProcessBlock) comp;
                if (block.getProcessId().equals(processId)) {
                    return block;
                }
            }
        }
        return null;
    }

    private Color getProgressColor() {
        switch (colorType) {
            case RAM:   return VirtualMemoryManagerGUI.PRIMARY_COLOR; // Use main GUI colors
            case SWAP:  return VirtualMemoryManagerGUI.SECONDARY_COLOR; // Use main GUI colors
            case CACHE: return VirtualMemoryManagerGUI.SUCCESS_COLOR; // Use main GUI colors
            default:    return VirtualMemoryManagerGUI.PRIMARY_COLOR;
        }
    }
}
