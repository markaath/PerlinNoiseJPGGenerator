package model;

import java.util.Random;

public class PerlinNoise {

  public record Node<T>(int row, int col, int height, int width, T content) {};

  private static final int PERMUTATION_SIZE = 256;
  private final int[] perm = new int[PERMUTATION_SIZE * 2];
  private final Random random;

  public PerlinNoise() {
    this(new Random());
  }

  public PerlinNoise(long seed) {
    this(new Random(seed));
  }

  public PerlinNoise(Random random) {
    this.random = random;
    initializePermutation();
  }

  private void initializePermutation() {
    // Initialise le tableau de permutation
    for (int i = 0; i < PERMUTATION_SIZE; i++) {
      perm[i] = i;
    }

    // Mélange le tableau
    for (int i = 0; i < PERMUTATION_SIZE; i++) {
      int swapIndex = i + random.nextInt(PERMUTATION_SIZE - i);
      int temp = perm[i];
      perm[i] = perm[swapIndex];
      perm[swapIndex] = temp;
    }

    // Duplique pour éviter les modulo coûteux
    for (int i = 0; i < PERMUTATION_SIZE; i++) {
      perm[PERMUTATION_SIZE + i] = perm[i];
    }
  }

  // Fonction de fade (courbe de lissage)
  private static double fade(double t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  // Interpolation linéaire
  private static double lerp(double t, double a, double b) {
    return a + t * (b - a);
  }

  // Fonction de gradient (vecteurs unitaires)
  private static double grad(int hash, double x, double y) {
    // 8 directions possibles
    switch (hash & 7) {
      case 0:
        return x + y; // NE
      case 1:
        return -x + y; // NW
      case 2:
        return x - y; // SE
      case 3:
        return -x - y; // SW
      case 4:
        return x; // E
      case 5:
        return -x; // W
      case 6:
        return y; // N
      case 7:
        return -y; // S
      default:
        return 0;
    }
  }

  // Bruit de Perlin 2D
  public double noise(double x, double y) {
    // Trouver la cellule unitaire contenant le point
    int xi = (int) Math.floor(x) & 255;
    int yi = (int) Math.floor(y) & 255;

    // Coordonnées relatives dans la cellule
    double xf = x - Math.floor(x);
    double yf = y - Math.floor(y);

    // Calcul des vecteurs de distance aux coins
    double d00 = grad(perm[perm[xi] + yi], xf, yf);
    double d10 = grad(perm[perm[xi + 1] + yi], xf - 1, yf);
    double d01 = grad(perm[perm[xi] + yi + 1], xf, yf - 1);
    double d11 = grad(perm[perm[xi + 1] + yi + 1], xf - 1, yf - 1);

    // Interpolation
    double u = fade(xf);
    double v = fade(yf);

    double x1 = lerp(u, d00, d10);
    double x2 = lerp(u, d01, d11);

    return lerp(v, x1, x2);
  }

  // Bruit octavé (fractal)
  public double octaveNoise(
    double x,
    double y,
    int octaves,
    double persistence
  ) {
    double total = 0;
    double frequency = 1;
    double amplitude = 1;
    double maxValue = 0;

    for (int i = 0; i < octaves; i++) {
      total += noise(x * frequency, y * frequency) * amplitude;
      maxValue += amplitude;
      amplitude *= persistence;
      frequency *= 2;
    }

    return total / maxValue; // Normalisé entre -1 et 1
  }

  /**
   * Génère une grille de bruit Perlin simple (1 octave)
   */
  public double[][] generateNoise(int height, int width) {
    double[][] noise = new double[height][width];
    double scale = 0.03; // Échelle de base

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        // Bruit Perlin simple
        noise[y][x] = noise(x * scale, y * scale);
      }
    }

    return noise;
  }

  /**
   * Génère une grille de bruit Perlin fractal (multi-octaves)
   */
  public double[][] generateOctaveNoise(int height, int width) {
    double[][] noise = new double[height][width];
    double scale = 0.03; // Échelle de base
    int octaves = 8; // Nombre d'octaves
    double persistence = 0.5; // Persistance

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        // Bruit fractal avec plusieurs octaves
        noise[y][x] = octaveNoise(x * scale, y * scale, octaves, persistence);
      }
    }

    return noise;
  }

  /**
   * Génère une grille de bruit cellulaire (Voronoi/Worley)
   */
  public double[][] generateCellularNoise(int height, int width) {
    double[][] noise = new double[height][width];
    double scale = 0.05; // Densité des cellules
    int points = 4; // Points par cellule

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        // Bruit cellulaire
        noise[y][x] = cellularNoise(x, y, scale, points);
      }
    }

    return noise;
  }

  public double cellularNoise(double x, double y, double scale, int points) {
    double minDist = Double.MAX_VALUE;

    for (int i = 0; i < points; i++) {
      // Génère un point aléatoire dans une cellule
      double px = Math.floor(x * scale) + random.nextDouble();
      double py = Math.floor(y * scale) + random.nextDouble();

      double dx = (x * scale) - px;
      double dy = (y * scale) - py;
      double dist = dx * dx + dy * dy;

      if (dist < minDist) {
        minDist = dist;
      }
    }

    return 1.0 - Math.sqrt(minDist);
  }

  // Combinaison de Perlin et cellular noise pour des taches
  public double patchNoise(double x, double y, double scale) {
    double perlin = noise(x * 0.1, y * 0.1);
    double cellular = cellularNoise(x, y, scale, 3);

    // Combinaison non linéaire pour créer des taches
    return Math.pow(perlin, 2) * cellular;
  }

  public double[][] generatePatchyNoise(int width, int height) {
    double[][] noise = new double[height][width];
    double scale = 0.03; // Plus petit = plus grandes taches
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        // Combinaison de plusieurs bruits
        double n1 = octaveNoise(x * scale, y * scale, 10, 0.8);
        double n2 = octaveNoise(x * scale * 2, y * scale * 2, 2, 0.3);
        double n3 = patchNoise(x * 0.02, y * 0.02, 0.05);
        double n4 = patchNoise(x*0.01, y *0.01, 0.1);  

        // Forme des taches avec des fonctions de distance
        double distanceEffect = getDistanceEffect(x, y, width, height);

        // Combinaison non linéaire
        double value = (n1 * 0.3 + n2 * 0.5 + n3 * 0.2 + n4 * 0.3) * distanceEffect;
        noise[y][x] = value;
        min = Math.min(min, value);
        max = Math.max(max, value);
      }
    }

    double range = max - min;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        // Normalisation linéaire: -1 à 1
        noise[y][x] = 2 * ((noise[y][x] - min) / range) - 1;
      }
    }

    return noise;
  }

  // Crée des taches plus au centre qu'aux bords
  private double getDistanceEffect(int x, int y, int width, int height) {
    // Distance normalisée au centre
    double dx = (x - width / 2.0) / (width / 2.0);
    double dy = (y - height / 2.0) / (height / 2.0);
    double distance = Math.sqrt(dx * dx + dy * dy);

    // Fonction qui réduit près des bords
    return Math.max(0, 1 - distance * 0.5);
  }

  public double calculateAdaptiveThreshold(double[][] noise) {
    // Calcule la moyenne et écart-type
    double sum = 0;
    double sumSq = 0;
    int count = 0;

    for (double[] row : noise) {
      for (double value : row) {
        sum += value;
        sumSq += value * value;
        count++;
      }
    }

    double mean = sum / count;
    double stdDev = Math.sqrt(sumSq / count - mean * mean);

    // Seuil adaptatif : moyenne + 30% de l'écart-type
    return mean * 0.9 + stdDev * 0.4;
  }
}
