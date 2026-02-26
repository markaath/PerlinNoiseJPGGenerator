package control;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import model.ArrayToImage;
import view.PerlinPanel;

public class PerlinNoiseControlPanel extends JPanel {

  private JComboBox<SelectedTransformation> transformationBox;
  private JComboBox<SelectedStart> startBox;
  private JComboBox<String> colorBox;
  private SelectedTransformation transformation;
  private PerlinPanel centerPanel;
  private JSlider quantitySlider;
  private int quantity;
  private JButton applyButton;
  private JButton printImage;
  private JCheckBox grid;
  private JFrame frame;
  private JButton startStopSim;
  private boolean isSim;
  private Thread simThread;

  public enum SelectedTransformation {
    CONVERGENTE,
    LAPLACE,
    HORREUR,
    DIFF,
  }

  public enum SelectedStart {
    DEFAULT,
    OCTAVE,
    CELLULAR,
    PATCHY,
  }

  public PerlinNoiseControlPanel(PerlinPanel centerPanel, JFrame frame) {
    super();
    this.frame = frame;
    this.centerPanel = centerPanel;

    setLayout(new GridLayout(8, 1));
    initComponents();
    add(grid);
    grid.setVisible(true);
    add(startBox);
    startBox.setVisible(true);
    add(transformationBox);
    transformationBox.setVisible(true);
    add(quantitySlider);
    quantitySlider.setVisible(true);
    add(applyButton);
    applyButton.setVisible(true);
    add(printImage);
    printImage.setVisible(true);
    add(colorBox);
    colorBox.setVisible(true);
    add(startStopSim);
    startStopSim.setVisible(true);
    
  }

  private void initComponents() {
    this.transformationBox = new JComboBox<>(SelectedTransformation.values());
    this.startBox = new JComboBox<>(SelectedStart.values());
    transformationBox.addActionListener(e -> {
      this.transformation =
        (SelectedTransformation) transformationBox.getSelectedItem();
    });

    startBox.addActionListener(e -> {
      centerPanel.setNoise((SelectedStart) startBox.getSelectedItem());
    });

    this.quantitySlider = new JSlider(0, 10, 2);
    this.quantitySlider.setToolTipText("Simulation Speed");
    this.quantitySlider.setFocusable(false);
    this.quantitySlider.setPaintTicks(true);
    this.quantitySlider.setPaintLabels(true);
    this.quantitySlider.setMajorTickSpacing(1);
    this.quantitySlider.setValue(1);

    quantitySlider.addChangeListener(e -> {
      JSlider source = (JSlider) e.getSource();
      this.quantity = source.getValue();
    });

    this.applyButton = new JButton("+1 turn");
    applyButton.addActionListener(e -> {
      if (transformation != null) {
        centerPanel.playturn(transformation, Math.max(1, quantity));
      } else {
        centerPanel.playturn(SelectedTransformation.LAPLACE, Math.max(1, quantity));
      }
    });

    this.printImage = new JButton("Print");
    printImage.addActionListener(e1 -> {
      JDialog fileNameDialog = new JDialog(this.frame);
      fileNameDialog.setSize(500, 300);
      JPanel mainDialogPanel = new JPanel();
      mainDialogPanel.setLayout(new GridBagLayout());

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridheight = 1;
      gbc.gridwidth = 3;

      fileNameDialog.add(mainDialogPanel);

      JTextField fileNameField = new JTextField();
      fileNameField.setPreferredSize(new Dimension(200, 100));

      gbc.gridwidth = 2;
      gbc.gridx = 0;
      gbc.gridy = 1;

      mainDialogPanel.add(fileNameField, gbc);

      JButton validate = new JButton("Validate and print");
      validate.addActionListener(e2 -> {
        this.centerPanel.updateColor();
        ArrayToImage<Double> colorArray = new ArrayToImage<>(
          this.centerPanel.getNoiseAsNode()
        );

        colorArray.convert(fileNameField.getText(), centerPanel.getConverter());
        fileNameDialog.setVisible(false);
      });
      validate.setPreferredSize(new Dimension(100, 100));

      gbc.gridwidth = 1;
      gbc.gridx = 2;
      gbc.gridy = 1;

      mainDialogPanel.add(validate, gbc);
      mainDialogPanel.setVisible(true);

      fileNameDialog.pack();
      fileNameDialog.setVisible(true);
    });

    this.colorBox = new JComboBox<>(
      new String[] {
        "Default",
        "Sigmoid",
        "Scientific",
        "Red and Blue",
        "Black and White",
        "?",
      }
    );
    this.colorBox.addActionListener(e -> {
      String selected = (String) colorBox.getSelectedItem();
      if (selected.equals("Default")) {
        centerPanel.setConverter(centerPanel.getColorConverter().get(0));
      } else if (selected.equals("Sigmoid")) {
        centerPanel.setConverter(centerPanel.getColorConverter().get(1));
      } else if (selected.equals("Scientific")) {
        centerPanel.setConverter(centerPanel.getColorConverter().get(2));
      } else if (selected.equals("Red and Blue")) {
        centerPanel.setConverter(centerPanel.getColorConverter().get(3));
      } else if (selected.equals("Black and White")) {
        centerPanel.setConverter(centerPanel.getColorConverter().get(4));
      } else {
        centerPanel.setConverter(centerPanel.getColorConverter().get(5));
      }
      centerPanel.updateColor();
      centerPanel.revalidate();
      centerPanel.repaint();
    });

    this.startStopSim = new JButton("Start");
    this.startStopSim.addActionListener(e -> {
      if (this.isSim) {
        this.startStopSim.setText("Start");
        this.isSim = false;
      } else {
        this.startStopSim.setText("Stop");
        this.isSim = true;
        simThread = new Thread() {
          @Override
          public void run() {
            while (isSim) {
              if (transformation != null) {
                centerPanel.playturn(transformation, Math.max(1, quantity));
              } else {
                centerPanel.playturn(SelectedTransformation.LAPLACE, Math.max(1, quantity));
              }
              SwingUtilities.invokeLater(() -> {
                // Mettre à jour l'interface graphique
                centerPanel.repaint();
              });

              try {
                Thread.sleep(200); // ~60 FPS
              } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
              }
            }
          }
        };
        simThread.start();
      }
    });
    this.grid = new JCheckBox("Grid : ");
    grid.addActionListener(e -> {
      centerPanel.setWithGrid(grid.isSelected());
      centerPanel.revalidate();
      centerPanel.repaint();
    });
  }
}
