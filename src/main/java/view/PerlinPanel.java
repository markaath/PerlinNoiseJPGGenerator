package view;

import control.PerlinNoiseControlPanel.SelectedStart;
import control.PerlinNoiseControlPanel.SelectedTransformation;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;
import javax.swing.JPanel;
import model.PerlinNoise;
import model.PerlinNoise.Node;

public class PerlinPanel extends JPanel {

  private Random RANDOM = new Random();
  private PerlinNoise perlinNoise;
  private long seed;
  private Color[][] map;
  private double[][] noise;

  private final int MAP_SIZE = 100;
  private int cellSize;
  private Function<Double, Color> converter;
  private boolean withGrid;

  public static Function<Double, Color> normalizedConverter = value -> {
    // Normaliser le double dans [0, 1] de manière continue
    double normalized;
    if (value < 0) {
      // Échelle logarithmique pour les valeurs négatives
      normalized =
        0.5 * (1 + Math.log1p(-value) / Math.log1p(-Double.MIN_VALUE));
    } else if (value > 0) {
      // Échelle logarithmique pour les valeurs positives
      normalized =
        0.5 + 0.5 * (Math.log1p(value) / Math.log1p(Double.MAX_VALUE));
    } else {
      // Pour zéro, on retourne une couleur intermédiaire (par exemple, gris)
      normalized = 0.5;
    }

    // Assurer que normalized est dans [0, 1]
    normalized = Math.max(0, Math.min(1, normalized));

    // Couleurs de départ (violet) et d'arrivée (rouge)
    int startR = 0;
    int startG = 0;
    int startB = 0;

    int endR = 255;
    int endG = 255;
    int endB = 255;

    // Interpolation linéaire pour chaque composante
    int r = (int) (startR + normalized * (endR - startR));
    int g = (int) (startG + normalized * (endG - startG));
    int b = (int) (startB + normalized * (endB - startB));

    return new Color(r, g, b);
  };

  public static Function<Double, Color> BlackAndWhite = value -> {
    value = Math.max(-1.0, Math.min(1.0, value));

    // Fonction sigmoïde pour avoir une transition douce
    // sigmoid(x) = 1 / (1 + exp(-k*x)) où k contrôle la pente
    final double k = 5.0; // Plus k est grand, plus la transition est brutale

    double normalized = 1.0 / (1.0 + Math.exp(-k * value));

    int gray = (int) (normalized * 255);
    return new Color(gray, gray, gray);
  };

  public static Function<Double, Color> Red2Blue = value -> {
    // Clamp la valeur
    value = Math.max(-1.0, Math.min(1.0, value));

    // Sigmoïde pour une transition douce
    final double k = 5.0; // Contrôle la pente
    double normalized = 1.0 / (1.0 + Math.exp(-k * value));

    // Dégradé de bleu (-1) à rouge (1) en passant par violet/gris (0)
    int r, g, b;

    if (normalized < 0.5) {
      // Partie bleue → violet (0 → 0.5)
      double t = normalized * 2.0; // [0,1]
      r = (int) (100 * t); // Du noir au rouge foncé
      g = 0;
      b = (int) (255 * (1 - t)); // Du bleu au noir
    } else {
      // Partie violet → rouge (0.5 → 1)
      double t = (normalized - 0.5) * 2.0; // [0,1]
      r = (int) (100 + 155 * t); // Du rouge foncé au rouge vif
      g = (int) (50 * t); // Un peu de vert pour adoucir
      b = (int) (100 * (1 - t)); // Du violet au rouge
    }

    // Clamp les valeurs
    r = Math.max(0, Math.min(255, r));
    g = Math.max(0, Math.min(255, g));
    b = Math.max(0, Math.min(255, b));

    return new Color(r, g, b);
  };

  public static Function<Double, Color> colorsSigmoid = value -> {
    value = Math.max(-1.0, Math.min(1.0, value));

    // Sigmoïde
    final double k = 8.0;
    double normalized = 1.0 / (1.0 + Math.exp(-k * value));

    // Hue dans HSV: 0=rouge, 0.33=vert, 0.66=bleu, 1=rouge
    float hue = (float) normalized * 0.5f; // 0.8 pour éviter de boucler sur rouge
    float saturation = 0.9f; // Couleurs vives
    float brightness = 0.9f; // Pas trop sombre

    return Color.getHSBColor(hue, saturation, brightness);
  };

  public static Function<Double, Color> colors = value -> {
    double normalized = ((value + 100) / (200)) * 360;

    float hue = (float) normalized * 0.5f;
    float saturation = 0.9f; // Couleurs vives
    float brightness = 0.9f; // Pas trop sombre

    return Color.getHSBColor(hue, saturation, brightness);
  };

  public static Function<Double, Color> sci = value -> {
    value = Math.max(-1.0, Math.min(1.0, value));

    // Tanh pour une transition symétrique
    double normalized = 0.5 + 0.5 * Math.tanh(value * 3.0);

    // Bleu pour les négatifs, rouge pour les positifs, blanc au centre
    int r, g, b;

    if (normalized < 0.5) {
      // Bleu → blanc
      double t = normalized * 2.0;
      r = (int) (255 * t);
      g = (int) (255 * t);
      b = 255;
    } else {
      // Blanc → rouge
      double t = (normalized - 0.5) * 2.0;
      r = 255;
      g = (int) (255 * (1 - t));
      b = (int) (255 * (1 - t));
    }

    return new Color(r, g, b);
  };

  private final ArrayList<Function<Double, Color>> colorConverter =
    new ArrayList<>(
      Arrays.asList(
        colors,
        colorsSigmoid,
        sci,
        Red2Blue,
        BlackAndWhite,
        normalizedConverter
      )
    );

  // Liste des couleurs disponibles (maillage fin)
  // private static final Color[] COLOR_PALETTE = {
  //   Color.BLACK,
  //   Color.WHITE,
  //   Color.RED,
  //   Color.GREEN,
  //   Color.BLUE,
  //   Color.YELLOW,
  //   Color.MAGENTA,
  //   Color.CYAN,
  //   Color.ORANGE,
  //   Color.PINK,
  //   // Nuances de gris
  //   Color.DARK_GRAY,
  //   Color.GRAY,
  //   Color.LIGHT_GRAY,
  //   // Variantes plus claires/sombres
  //   Color.BLACK.brighter(),
  //   Color.RED.darker(),
  //   Color.GREEN.darker(),
  //   Color.BLUE.darker(),
  //   Color.YELLOW.darker(),
  //   Color.MAGENTA.darker(),
  //   Color.CYAN.darker(),
  // };

  // // Nombre de seuils (plus fin que votre version originale)
  // private static final int NUM_THRESHOLDS = COLOR_PALETTE.length;

  // // Calcul automatique des seuils entre -1.0 et 1.0
  // private final double[] thresholds = new double[NUM_THRESHOLDS + 1];

  public PerlinPanel(Function<Double, Color> converter) {
    this.map = new Color[MAP_SIZE][MAP_SIZE];
    this.perlinNoise = new PerlinNoise();
    noise = perlinNoise.generateNoise(MAP_SIZE, MAP_SIZE);
    this.converter = converter;
    this.withGrid = false;

    // Initialiser les seuils uniformément répartis entre -1.0 et 1.0
    // initializeThresholds();

    updateColor();
  }

  public boolean isWithGrid() {
    return withGrid;
  }

  public void setWithGrid(boolean withGrid) {
    this.withGrid = withGrid;
  }

  public ArrayList<Function<Double, Color>> getColorConverter() {
    return colorConverter;
  }

  public void setConverter(Function<Double, Color> converter) {
    this.converter = converter;
  }

  public PerlinPanel() {
    this(colors);
  }

  public void playturn(SelectedTransformation s, int speed) {
    for (int i = 0; i < speed; i++) {
      if (s == SelectedTransformation.CONVERGENTE) {
        playturnConvergeante();
      } else if (s == SelectedTransformation.LAPLACE) {
        playturnCool();
      } else if (s == SelectedTransformation.DIFF) {
        playturnEpillepsie();
      } else if (s == SelectedTransformation.HORREUR) {
        playturnTerreureNocturne();
      }
    }
    updateColor();
    revalidate();
    repaint();
  }

  @Override
  public void setSize(int height, int width) {
    super.setSize(height, width);
    this.cellSize = Math.min(height, width) / 2;
  }

  // private void initializeThresholds() {
  //   // Créer des seuils uniformément répartis entre -1.0 et 1.0
  //   double minValue = -1.0;
  //   double maxValue = 1.0;
  //   double step = (maxValue - minValue) / NUM_THRESHOLDS;

  //   for (int i = 0; i <= NUM_THRESHOLDS; i++) {
  //     thresholds[i] = minValue + (i * step);
  //   }
  // }

  public final void updateColor() {
    // Appliquer les couleurs avec mapping dynamique
    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        map[i][j] = this.converter.apply(noise[i][j]);
      }
    }
  }

  private double getValue(int i, int j) {
    if (i < 0 || i >= MAP_SIZE || j < 0 || j >= MAP_SIZE) {
      return 0;
    }
    return noise[i][j];
  }

  private double applyContrast(double value, double contrast) {
    // Fonction de contraste sigmoïde
    return 2 / (1 + Math.exp(-contrast * value)) - 1;
  }

  public Function<Double, Color> getConverter() {
    return converter;
  }

  private double[][] generateReactionDiffusionPattern() {
    double[][] pattern = new double[MAP_SIZE][MAP_SIZE];
    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        double x = ((double) i / MAP_SIZE) * 2 * Math.PI;
        double y = ((double) j / MAP_SIZE) * 2 * Math.PI;
        pattern[i][j] = 0.5 + 0.5 * Math.sin(x) * Math.cos(y);
      }
    }
    return pattern;
  }

  private double computeLaplacian(double[][] grid, int i, int j) {
    double sum = 0;
    int[][] neighbors = {
      { -1, 0 },
      { 1, 0 },
      { 0, -1 },
      { 0, 1 },
      { -1, -1 },
      { -1, 1 },
      { 1, -1 },
      { 1, 1 },
    };

    for (int[] n : neighbors) {
      int ni = i + n[0];
      int nj = j + n[1];
      if (isInBound(ni, nj)) {
        sum += grid[ni][nj];
      }
    }

    return sum - 8 * grid[i][j];
  }

  public double[][] getNoise() {
    return noise;
  }

  public void setNoise(SelectedStart s) {
    if (this.perlinNoise == null) {
      this.perlinNoise = new PerlinNoise();
    }
    if (s == SelectedStart.DEFAULT) {
      this.noise = this.perlinNoise.generateNoise(MAP_SIZE, MAP_SIZE);
    } else if (s == SelectedStart.OCTAVE) {
      this.noise = this.perlinNoise.generateOctaveNoise(MAP_SIZE, MAP_SIZE);
    } else if (s == SelectedStart.CELLULAR) {
      this.noise = this.perlinNoise.generateCellularNoise(MAP_SIZE, MAP_SIZE);
    } else if (s == SelectedStart.PATCHY) {
      this.noise = this.perlinNoise.generatePatchyNoise(MAP_SIZE, MAP_SIZE);
    }
    updateColor();
    revalidate();
    repaint();
  }

  private boolean isInBound(int row, int col) {
    return col < MAP_SIZE && col >= 0 && row < MAP_SIZE && row >= 0;
  }

  public void playturnConvergeante() {
    int[] di = { 0, 1, 0, -1 };
    int[] dj = { 1, 0, -1, 0 };
    double[][] newNoise = new double[MAP_SIZE][MAP_SIZE];
    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        double average = 0;
        for (int k = 0; k < 4; k++) {
          if (isInBound(i + di[k], j + dj[k])) {
            average += this.noise[i + di[k]][j + dj[k]];
          }
        }
        average /= 4;
        newNoise[i][j] = (2 * noise[i][j] + average) / 3;
      }
    }
    RANDOM.setSeed(seed);
    RANDOM.nextInt();
    double[][] tempNoise = new PerlinNoise(RANDOM).generatePatchyNoise(
      MAP_SIZE,
      MAP_SIZE
    );
    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        newNoise[i][j] =
          (newNoise[i][j] * newNoise[i][j] + tempNoise[i][j]) / 2;
      }
    }
    noise = newNoise;
    updateColor();
  }

  public void playturnEpillepsie() {
    int[] di = { 0, 1, 0, -1, 1, 1, -1, -1 };
    int[] dj = { 1, 0, -1, 0, 1, -1, 1, -1 };

    double[][] newNoise = new double[MAP_SIZE][MAP_SIZE];

    // Paramètres pour contrôler l'évolution
    double diffusionRate = 0.3; // Combien se diffuse
    double noiseStrength = 0.4; // Force du nouveau bruit
    double nonlinearity = 0.2; // Effets non-linéaires

    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        // Calcul de la moyenne avec voisins (incluant diagonales)
        double average = 0;
        int count = 0;

        for (int k = 0; k < di.length; k++) {
          if (isInBound(i + di[k], j + dj[k])) {
            average += this.noise[i + di[k]][j + dj[k]];
            count++;
          }
        }
        average = count > 0 ? average / count : 0;

        // Diffusion partielle (pas complète)
        double diffused =
          noise[i][j] * (1 - diffusionRate) + average * diffusionRate;

        // Ajouter un terme non-linéaire pour éviter la convergence
        double nonlinear = Math.sin(noise[i][j] * Math.PI) * nonlinearity;

        newNoise[i][j] = diffused + nonlinear;
      }
    }

    // Générer du bruit fractal pour plus de complexité
    PerlinNoise perlin = new PerlinNoise(RANDOM.nextLong());
    double[][] fractalNoise = perlin.generatePatchyNoise(MAP_SIZE, MAP_SIZE);

    // Générer un pattern de réaction-diffusion (pattern de Turing)
    double[][] reactionDiffusion = generateReactionDiffusionPattern();

    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        // Combinaison avec poids variables
        double weight = 0.3 + 0.4 * Math.sin(i * j * 0.01); // Poids variable

        // Mélange avec la nouvelle texture
        newNoise[i][j] =
          newNoise[i][j] * (1 - weight) +
          fractalNoise[i][j] * weight +
          reactionDiffusion[i][j] * 0.2;

        // Appliquer une fonction de contraste pour éviter l'aplatissement
        newNoise[i][j] = applyContrast(newNoise[i][j], 1.5);
      }
    }

    noise = newNoise;
    updateColor();
  }

  public void playturnCool() {
    // Conserver l'historique pour avoir de l'inertie
    double[][] velocityX = new double[MAP_SIZE][MAP_SIZE];
    double[][] velocityY = new double[MAP_SIZE][MAP_SIZE];
    double[][] divergence = new double[MAP_SIZE][MAP_SIZE];

    // Étape 1 : Advection (le bruit se déplace)
    double[][] advected = new double[MAP_SIZE][MAP_SIZE];
    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        // Vitesse basée sur le gradient local
        velocityX[i][j] = (getValue(i + 1, j) - getValue(i - 1, j)) * 0.1;
        velocityY[i][j] = (getValue(i, j + 1) - getValue(i, j - 1)) * 0.1;

        // Advection semi-lagrangienne
        int sourceX = (int) Math.max(
          0,
          Math.min(MAP_SIZE - 1, i - velocityX[i][j])
        );
        int sourceY = (int) Math.max(
          0,
          Math.min(MAP_SIZE - 1, j - velocityY[i][j])
        );

        advected[i][j] = noise[sourceX][sourceY];
      }
    }

    // Étape 2 : Ajouter des sources aléatoires périodiquement
    if (RANDOM.nextDouble() < 0.1) {
      // 10% de chance par tour
      int sourceX = RANDOM.nextInt(MAP_SIZE);
      int sourceY = RANDOM.nextInt(MAP_SIZE);
      double strength = RANDOM.nextDouble() * 2 - 1; // Entre -1 et 1

      // Source gaussienne
      for (int i = 0; i < MAP_SIZE; i++) {
        for (int j = 0; j < MAP_SIZE; j++) {
          double dist = Math.sqrt(
            (i - sourceX) * (i - sourceX) + (j - sourceY) * (j - sourceY)
          );
          if (dist < 5) {
            double factor = Math.exp((-dist * dist) / 8.0);
            advected[i][j] += strength * factor;
          }
        }
      }
    }

    // Étape 3 : Diffusion non-linéaire
    double[][] diffused = new double[MAP_SIZE][MAP_SIZE];
    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        double sum = 0;
        int count = 0;

        // Voisinage de taille variable
        int radius = 1 + (int) (Math.abs(noise[i][j]) * 3);
        for (int di = -radius; di <= radius; di++) {
          for (int dj = -radius; dj <= radius; dj++) {
            if (isInBound(i + di, j + dj)) {
              sum += advected[i + di][j + dj];
              count++;
            }
          }
        }

        diffused[i][j] = advected[i][j] * 0.7 + (sum / count) * 0.3;
      }
    }

    // Étape 4 : Ajouter du bruit fractal à haute fréquence
    PerlinNoise highFreq = new PerlinNoise(RANDOM.nextLong());
    double[][] highFreqNoise = highFreq.generatePatchyNoise(MAP_SIZE, MAP_SIZE);

    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        // Mélanger avec poids dépendant de la position
        double mixFactor = 0.1 + 0.1 * Math.sin(i * 0.1) * Math.cos(j * 0.1);
        noise[i][j] =
          diffused[i][j] * (1 - mixFactor) + highFreqNoise[i][j] * mixFactor;

        // Limiter les extrêmes
        noise[i][j] = Math.max(-1, Math.min(1, noise[i][j]));
      }
    }

    updateColor();
  }

  public void playturnTerreureNocturne() {
    double[][] a = noise; // "activateur"
    double[][] b = new double[MAP_SIZE][MAP_SIZE]; // "inhibiteur"

    // Initialiser b avec un pattern complémentaire
    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        b[i][j] = 1.0 - noise[i][j];
      }
    }

    double[][] newA = new double[MAP_SIZE][MAP_SIZE];
    double[][] newB = new double[MAP_SIZE][MAP_SIZE];

    // Paramètres du système de Turing
    double feedRate = 0.055;
    double killRate = 0.062;
    double da = 0.5; // Diffusion de A
    double db = 0.25; // Diffusion de B (plus lent)

    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        // Laplacien (diffusion) pour A et B
        double laplacianA = computeLaplacian(a, i, j);
        double laplacianB = computeLaplacian(b, i, j);

        // Équations de réaction-diffusion (Gray-Scott model simplifié)
        double reaction = a[i][j] * b[i][j] * b[i][j];
        newA[i][j] =
          a[i][j] + (da * laplacianA - reaction + feedRate * (1 - a[i][j]));
        newB[i][j] =
          b[i][j] +
          (db * laplacianB + reaction - (feedRate + killRate) * b[i][j]);

        // Limiter entre 0 et 1
        newA[i][j] = Math.max(0, Math.min(1, newA[i][j]));
        newB[i][j] = Math.max(0, Math.min(1, newB[i][j]));
      }
    }

    // Utiliser A comme nouveau bruit (ou une combinaison)
    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        // Pattern intéressant : produit ou différence
        noise[i][j] = newA[i][j] * newB[i][j];
      }
    }

    updateColor();
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    int cellSize =
      Toolkit.getDefaultToolkit().getScreenSize().height / MAP_SIZE;
    int horizontal_padding =
      (Toolkit.getDefaultToolkit().getScreenSize().width -
        (cellSize * MAP_SIZE)) /
      2;
    int vertical_padding =
      (Toolkit.getDefaultToolkit().getScreenSize().height -
        (cellSize * MAP_SIZE)) /
      2;

    for (int y = 0; y < MAP_SIZE; y++) {
      for (int x = 0; x < MAP_SIZE; x++) {
        g.setColor(map[y][x]);
        g.fillRect(
          x * cellSize + horizontal_padding,
          y * cellSize + vertical_padding,
          cellSize,
          cellSize
        );
        if (withGrid) {
          g.setColor(Color.BLACK);
          g.drawRect(
            x * cellSize + horizontal_padding,
            y * cellSize + vertical_padding,
            cellSize,
            cellSize
          );
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public Node<Color>[][] getMapAsNode() {
    Node<Color>[][] res = new Node[MAP_SIZE][MAP_SIZE];
    int cellSize =
      Toolkit.getDefaultToolkit().getScreenSize().height / MAP_SIZE;

    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        res[i][j] = new Node<>(i, j, cellSize, cellSize, map[i][j]);
      }
    }

    return res;
  }

  @SuppressWarnings("unchecked")
  public Node<Double>[][] getNoiseAsNode() {
    Node<Double>[][] res = new Node[MAP_SIZE][MAP_SIZE];
    int cellSize =
      Toolkit.getDefaultToolkit().getScreenSize().height / MAP_SIZE;

    for (int i = 0; i < MAP_SIZE; i++) {
      for (int j = 0; j < MAP_SIZE; j++) {
        res[i][j] = new Node<>(i, j, cellSize, cellSize, noise[i][j]);
      }
    }

    return res;
  }
}
