package model;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Function;
import javax.imageio.ImageIO;
import model.PerlinNoise.Node;

public class ArrayToImage<T> {

  private Node<T>[][] toConvert;
  private String fileName;

  public ArrayToImage(Node<T>[][] toConvert) {
    this.toConvert = toConvert;
  }

  public void convert(String filename, Function<T, Color> converter) {
    int cellheight = toConvert[0][0].height();
    int cellwidth = toConvert[0][0].width();
    BufferedImage buffImg = new BufferedImage(
      toConvert[0].length * cellwidth,
      toConvert.length * cellheight,
      BufferedImage.TYPE_INT_RGB
    );
    for (int i = 0; i < toConvert.length; i++) {
      for (int j = 0; j < toConvert[0].length; j++) {
        if (toConvert[i][j].content() != null) {
          for (int y = 0; y < cellheight; y++) {
            for (int x = 0; x < cellwidth; x++) {
              buffImg.setRGB(
                j * cellwidth + x,
                i * cellheight + y,
                (converter.apply(toConvert[i][j].content())).getRGB()
              );
            }
          }
        }
      }
    }
    File output = new File("generated/" + filename);
    try {
      output.createNewFile();
      ImageIO.write(buffImg, "jpg", output);
    } catch (Exception e) {
      System.out.println("erreur");
    }
  }

  public void setFileName(String s) {
    this.fileName = "generated/" + s;
  }
}
