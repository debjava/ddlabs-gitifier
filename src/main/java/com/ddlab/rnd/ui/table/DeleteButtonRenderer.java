package com.ddlab.rnd.ui.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class DeleteButtonRenderer extends JButton implements TableCellRenderer {

    public DeleteButtonRenderer() {
//        setText("Remove");
        setForeground(Color.RED);
        setIcon(new ImageIcon(DeleteButtonRenderer.class.getResource("/icons/delete-16_1.png")));
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        return this;
    }
}