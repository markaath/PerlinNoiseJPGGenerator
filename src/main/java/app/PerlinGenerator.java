package app;

import control.PerlinNoiseControlPanel;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import view.PerlinPanel;

public class PerlinGenerator {



  public static void main(String[] args) {
    JFrame frame = new JFrame("PerlinGenerator");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // 1. Créer les panels
    PerlinPanel centerPanel = new PerlinPanel();
    JPanel controlPanel = new PerlinNoiseControlPanel(centerPanel, frame);

    // 2. Configurer le layout du frame
    frame.setLayout(new BorderLayout());
    frame.add(centerPanel, BorderLayout.CENTER);
    frame.add(controlPanel, BorderLayout.WEST);

    // 3. Afficher
    frame.pack(); // IMPORTANT: laisse le layout manager calculer les tailles
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    frame.setVisible(true);
  }
}
