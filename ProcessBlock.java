import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class ProcessBlock extends JPanel {

    enum ColorType { RAM, SWAP, CACHE }

    private final String processId;
    private final ColorType colorType;
    private boolean isDirty;
    private float alpha = 1.0f;
    private Color currentBorderColor = Color.DARK_GRAY; // Default subtle border
    private int currentBorderThickness = 1;
    private final VirtualMemoryManagerGUI controller; // Reference to main GUI

    // Gradient colors (approximations from CSS)
    private static final Color RAM_COLOR_START = new Color(100, 181, 246);
    private static final Color RAM_COLOR_END = new Color(25, 118, 210);
    private static final Color SWAP_COLOR_START = new Color(255, 171, 145);
    private static final Color SWAP_COLOR_END = new Color(216, 67, 21);
    private static final Color CACHE_COLOR_START = new Color(165, 214, 167);
    private static final Color CACHE_COLOR_END = new Color(46, 125, 50);
    private static final Color DIRTY_INDICATOR_COLOR = new Color(248, 81, 73); // ERROR_COLOR

    public ProcessBlock(String processId, ColorType type, boolean dirty, VirtualMemoryManagerGUI controller) {
        this.processId = processId;
        this.colorType = type;
        this.isDirty = dirty;
        this.controller = controller; // Store reference

        setPreferredSize(new Dimension(100, 42)); // Adjust width as needed
        setMinimumSize(new Dimension(80, 42));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); // Fill width
        setBorder(new LineBorder(currentBorderColor, currentBorderThickness)); // Initial border
        setOpaque(false); // We handle painting

        // Tooltip
        setToolTipText(controller.generateTooltipText(processId, type.name()));

        // Mouse Listener for Access (Left Click) and Terminate (Right Click)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                     controller.processIdInput.setText(processId); // Set ID in input field
                     controller.accessProcess(); // Trigger access action
                } else if (SwingUtilities.isRightMouseButton(e)) {
                     controller.endProcessRequest(processId); // Trigger terminate action
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Optional: Add hover effect (e.g., brighter border)
                // setBorderColor(VirtualMemoryManagerGUI.HIGHLIGHT_COLOR, 2);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                 // Optional: Remove hover effect
                 // setBorderColor(Color.DARK_GRAY); // Restore default
                 // resetBorderThickness();
            }
        });
    }

    public String getProcessId() {
        return processId;
    }

    public void setDirty(boolean dirty) {
        if (this.isDirty != dirty) {
             this.isDirty = dirty;
             setToolTipText(controller.generateTooltipText(processId, colorType.name())); // Update tooltip
             repaint();
        }
    }

    public void setAlpha(float alpha) {
         this.alpha = Math.max(0f, Math.min(1f, alpha));
         repaint();
     }

     public void setBorderColor(Color color, int thickness) {
         this.currentBorderColor = color;
         this.currentBorderThickness = Math.max(1, thickness);
         setBorder(new LineBorder(this.currentBorderColor, this.currentBorderThickness));
         repaint();
     }
     public void setBorderColor(Color color) {
          setBorderColor(color, this.currentBorderThickness);
      }
      public Color getBorderColor() { return currentBorderColor; }

     public void resetBorderThickness() {
         this.currentBorderThickness = 1;
         setBorderColor(this.currentBorderColor, this.currentBorderThickness);
     }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Apply Alpha
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Rounded Rectangle Background
        int arc = 16; // Corner roundness
        GradientPaint gp = null;
        switch (colorType) {
            case RAM:   gp = new GradientPaint(0, 0, RAM_COLOR_START, getWidth(), getHeight(), RAM_COLOR_END); break;
            case SWAP:  gp = new GradientPaint(0, 0, SWAP_COLOR_START, getWidth(), getHeight(), SWAP_COLOR_END); break;
            case CACHE: gp = new GradientPaint(0, 0, CACHE_COLOR_START, getWidth(), getHeight(), CACHE_COLOR_END); break;
        }
        if (gp != null) {
             g2d.setPaint(gp);
        } else {
            g2d.setColor(Color.GRAY); // Fallback
        }
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

        // Dirty Indicator (thick left border style)
        if (isDirty && colorType == ColorType.RAM) {
            g2d.setColor(DIRTY_INDICATOR_COLOR);
            g2d.fillRect(0, 0, 5, getHeight()); // 5 pixel wide bar on the left
             // Optionally round the corners of the indicator slightly
             // g2d.fillRoundRect(0, 0, 5, getHeight(), arc/2, arc/2);
        }

        // Draw Process ID Text
        g2d.setFont(getFont() != null ? getFont() : VirtualMemoryManagerGUI.BOLD_FONT);
        g2d.setColor(Color.WHITE); // Text color
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(processId);
        int textHeight = fm.getAscent();
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + textHeight;
        g2d.drawString(processId, x, y);

        g2d.dispose();
    }
}
