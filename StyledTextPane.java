import javax.swing.*;
import javax.swing.text.*;
import java.awt.Color;

class StyledTextPane {

    private JTextPane textPane;
    private StyledDocument doc;

    public StyledTextPane(JTextPane pane) {
        this.textPane = pane;
        this.doc = pane.getStyledDocument();
    }

    public void append(String text, Color color, boolean isBold) {
        Style style = textPane.addStyle("Style_" + color.getRGB() + "_" + isBold, null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, isBold);
        // Could add other attributes like font family, size here

        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace(); // Or handle more gracefully
        }
    }

     public void append(String text, Color color) {
         append(text, color, false); // Default to not bold
     }
}
