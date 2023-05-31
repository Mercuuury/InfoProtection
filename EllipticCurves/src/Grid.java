import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.awt.Dimension;
import java.awt.Color;

public class Grid {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private GridPane panel;

    public Grid(int size, ECPoint[] solutions) {
        JFrame frame = new JFrame("Elliptic curve");
        panel = new GridPane(WIDTH, HEIGHT, size, solutions);

        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void setPostOpPoint(ECPoint point, String label) {
        panel.setPostOpPoint(point, label);
        panel.repaint();
    }

    public void clearPostOpPoints() {
        panel.clearPostOpPoints();
        panel.repaint();
    }

    public class GridPane extends JPanel {
        private static final int OFFSET = 5; // Сдвиг
        private static int WIDTH; // Ширина панели
        private static int HEIGHT; // Высота панели
        private static int GRID_SIZE; // Размер клетки
        private static ECPoint[] points; // Исходные точки кривой
        private static HashMap<ECPoint, String> postOpPoints; // Точки после применения операций над ними

        public GridPane(int w, int h, int size, ECPoint[] solutions) {
            WIDTH = w;
            HEIGHT = h;
            GRID_SIZE = w / size;
            points = solutions;
            postOpPoints = new HashMap<ECPoint, String>();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(WIDTH, HEIGHT);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, getWidth(), getHeight());

            drawGrid(g2d);
            // drawAxes(g2d);
            drawLabels(g2d);
            
            for (ECPoint ecPoint : points) {
                drawPoint(g2d, ecPoint.x.intValue(), ecPoint.y.intValue(), "", Color.RED);
            }

            postOpPoints.forEach((point, label) -> {
                drawPoint(g2d, point.x.intValue(), point.y.intValue(), label, new Color((int)(Math.random() * 0x1000000)));
            });

            g2d.dispose();
        }

        private void drawPoint(Graphics2D g2d, double x, double y, String label, Color color) {
            g2d.setColor(color);
            
            int scaledX = (int) (x * GRID_SIZE);
            int scaledY = (int) (y * GRID_SIZE);
            g2d.fillOval(scaledX - 4, HEIGHT - scaledY - 4, 8, 8);

            g2d.drawString(label, (int) (x * GRID_SIZE) + 4, HEIGHT - (int) (y * GRID_SIZE));
        }

        private void drawGrid(Graphics2D g2d) {
            g2d.setColor(Color.LIGHT_GRAY);  
            for (int x = 0; x <= WIDTH; x += GRID_SIZE) {
                g2d.drawLine(x, 0, x, HEIGHT); // Vertical
            }
            for (int y = 0; y <= HEIGHT; y += GRID_SIZE) {
                g2d.drawLine(0, y, WIDTH, y); // Horizontal
            }
        }
    
        private void drawLabels(Graphics2D g2d) {
            g2d.setColor(Color.BLACK);
            for (int x = GRID_SIZE; x <= WIDTH; x += GRID_SIZE) {
                g2d.drawString(Integer.toString(x / GRID_SIZE), x + OFFSET, HEIGHT - OFFSET); // OX
            }
            for (int y = GRID_SIZE; y <= HEIGHT; y += GRID_SIZE) {
                g2d.drawString(Integer.toString(y / GRID_SIZE), OFFSET, HEIGHT - y - OFFSET); // OY
            }
        }

        public void setPostOpPoint(ECPoint point, String label) {
            postOpPoints.put(point, label);
        }

        public void clearPostOpPoints() {
            postOpPoints = new HashMap<ECPoint, String>();
        }

        // private void drawAxes(Graphics2D g2d) {
        //     g2d.setColor(Color.BLACK);
        //     g2d.drawLine(0, HEIGHT - OFFSET, WIDTH, HEIGHT - OFFSET); // OX
        //     g2d.drawLine(OFFSET, 0, OFFSET, HEIGHT); // OY
        // }
    }
}
