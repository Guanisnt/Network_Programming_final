import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GameMap {
    private Color platformColor;
    private Color bgColor;
    private Color wordColor;
    private List<Point> WordPos = new ArrayList<>();
    private List<Rectangle> platforms = new ArrayList<>();
    private List<Rectangle> deathRegion = new ArrayList<>();
    private List<Point> bornPoint = new ArrayList<>();

    public void getMap(int index){
        //根據不同的index畫不同的地圖
        switch (index) {
            case 1:
                Map1();
                break;
            case 2:
                Map2();
                break;
            default:
                throw new AssertionError();
        }
    }

    public Color getbgColor(){ return bgColor; }
    public Color getwordColor(){ return wordColor; }
    public Color getplatformColor(){ return platformColor; }
    public List<Rectangle> getplatforms(){ return platforms; }
    public List<Rectangle> getdeathRegion(){ return deathRegion; }
    public List<Point> getbornPoint(){ return bornPoint; }
    public List<Point> getWordPos(){ return WordPos; }

    private void Map1(){
        bornPoint = List.of(
            new Point(225, 300),
            new Point(1125, 350),
            new Point(680, 400),
            new Point(680, 150)
        );
        platforms = List.of(
            new Rectangle(150, 385, 200, 300),
            new Rectangle(150, 0, 200, 225),
            new Rectangle(450, 500, 500, 50),
            new Rectangle(450, 300, 500, 50),
            new Rectangle(1050, 410, 200, 275),
            new Rectangle(1050, 0, 200, 250)
        );
        WordPos = List.of(
            new Point(550, 200),
            new Point(350,450)
        );
        bgColor = Color.decode("#3E5879");
        wordColor = Color.decode("#213555");
        platformColor = Color.decode("#DCEAF7");
    }

    private void Map2(){
        bornPoint = List.of(
            new Point(100, 200),
            new Point(300, 310),
            new Point(700, 400),
            new Point(1100, 310),
            new Point(1300, 200)
        );
        platforms = List.of(
            new Rectangle(0, 300, 200, 450),
            new Rectangle(200, 410, 200, 275),
            new Rectangle(400, 500, 600, 250),
            new Rectangle(1000, 410, 200, 275),
            new Rectangle(1200, 300, 240, 450)
        );
        deathRegion = List.of(
            new Rectangle(190, 380, 10, 30),
            new Rectangle(390, 470, 10, 30),
            new Rectangle(1000, 470, 10, 30),
            new Rectangle(1200, 380, 10, 30)
        );
        WordPos = List.of(
            new Point(550, 200),
            new Point(350,350)
        );
        bgColor = Color.decode("#118B50");
        wordColor = Color.decode("#5DB996");
        platformColor = Color.decode("#E3F0AF");
    }
}