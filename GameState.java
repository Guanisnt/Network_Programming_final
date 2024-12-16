/*更新各種遊戲狀態 */
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap; // 有多執行續的Map，不用寫synchronized
import javax.imageio.ImageIO;

public class GameState extends Frame{
    

    private ConcurrentHashMap<Integer, Sprite> players = new ConcurrentHashMap<>(); // <playerID, sprite>
    BufferedImage bufferPage=null;
    private List<Rectangle> platforms = new ArrayList<>(); // 地板
    private static final Point[] bornPoint = {
        new Point(225, 300),
        new Point(600, 100),
        new Point(150, 300),
        new Point(600, 300)
    };

    public GameState() {
        // 初始化地板，之後畫地圖也可以用
        this.initPlatforms();
    }


    public void initPlatforms() {
        platforms.add(new Rectangle(150, 385, 200, 300));
        platforms.add(new Rectangle(150, 0, 200, 225));

        platforms.add(new Rectangle(450, 500, 500, 50));
        platforms.add(new Rectangle(450, 200, 500, 50));

        platforms.add(new Rectangle(1050, 410, 200, 275));
        platforms.add(new Rectangle(1050, 0, 200, 250));
    }

    // 新增玩家
    public void addPlayer(int id) {
        Sprite player = new Sprite(id, bornPoint[0].x, bornPoint[0].y);  // 用id來決定出生點
        System.out.println(bornPoint[id%4].x+" "+bornPoint[id%4].y);
        players.put(id, player);
    }

    // 移除玩家
    public void removePlayer(int id) {
        players.remove(id);
    }

    // 更新玩家資訊
    public void updatePlayer(int id, double x, double y, Color color, double vX, double vY, boolean isAlive) {
        Sprite player = players.get(id);
        if(player != null) {
            player.setPosition(x, y);
            player.setColor(color);
            player.setVelocity(vX, vY);
            player.setAlive(isAlive);
        }
    }

    // 這裡可能會多玩家或少玩家
    public void update() {
        List<Sprite> playerList = new ArrayList<>(players.values()); // copy玩家清單
        for(Sprite player : playerList) {
            player.update(platforms, playerList); // call Sprite的update來用
        }
    }

    // 畫所有東西
    public void draw(Graphics g) {
        Graphics bufferg;
        if(bufferPage == null)
            bufferPage = new BufferedImage(1440, 720, BufferedImage.TYPE_INT_ARGB);
        bufferg = bufferPage.getGraphics();
        BufferedImage bg;
        // 畫背景
        try {
            // 讀取 JPG 檔案
            bg = ImageIO.read(new File("map1/background.jpg"));
            bufferg.drawImage(bg, 0,0,1440,720, this);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("無法讀取圖片檔案！");
        }
        
        BufferedImage floor;
        // 畫地板
        try{
            floor = ImageIO.read(new File("map1/floor.png"));
            bufferg.drawImage(floor,150 ,385,200,300 ,this);
            bufferg.drawImage(floor,150 ,0,200,225 ,this);

            bufferg.drawImage(floor,450 ,500,500,50 ,this);
            bufferg.drawImage(floor,450 ,200,500,50 ,this);

            bufferg.drawImage(floor,1050 ,410,200,275 ,this);
            bufferg.drawImage(floor,1050 ,0,200,250 ,this);
        }catch(IOException e) {
            e.printStackTrace();
            System.out.println("無法讀取圖片檔案！");
        }

        // 畫玩家
        for(Sprite player : players.values()) {
            if(player.isAlive()){
                BufferedImage plr;
                try {
                    System.out.println(player.getX()+" "+player.getY());
                    // 讀取 JPG 檔案
                    plr = ImageIO.read(new File("plr/plr1.png"));
                    bufferg.drawImage(plr,(int)player.getX(),(int)player.getY(),30,30,this);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("無法讀取圖片檔案！");
                }
            }
        }

        // BufferedImage plr;
        // try {
        //     // 讀取 JPG 檔案
        //     plr = ImageIO.read(new File("plr/plr1.png"));
        //     bufferg.drawImage(plr,550,300,50,50,this);
        // } catch (IOException e) {
        //     e.printStackTrace();
        //     System.out.println("無法讀取圖片檔案！");
        // }

        bufferg.dispose();
        g.drawImage(bufferPage, getInsets().left, getInsets().top, this);
    }

    public ConcurrentHashMap<Integer, Sprite> getPlayers() {return players;} // server要用

    
}
