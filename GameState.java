/*更新各種遊戲狀態 */
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO; // 有多執行續的Map，不用寫synchronized

public class GameState extends Frame{
    

    private ConcurrentHashMap<Integer, Sprite> players = new ConcurrentHashMap<>(); // <playerID, sprite>
    BufferedImage bufferPage=null;// 地板

    private GameMap map = new GameMap(); 
    private List<Rectangle> platforms = new ArrayList<>();
    private List<Rectangle> deathRegion = new ArrayList<>(); 
    public static List<Point> bornPoint = new ArrayList<>();
    private List<Point> WordPos = new ArrayList<>();
    private int mapIndex;

    // public GameState(){
    // }

    public void setMapIndex(int index) {
        this.mapIndex = index;
        map.getMap(index);
        platforms = map.getplatforms();
        bornPoint = map.getbornPoint();
        deathRegion = map.getdeathRegion();
        WordPos = map.getWordPos();
    }

    // 新增玩家
    public void addPlayer(int id) {
        Sprite player = new Sprite(id, bornPoint.get(id%bornPoint.size()).x, bornPoint.get(id%bornPoint.size()).y);  // 用id來決定出生點
        // System.out.println(bornPoint[id%4].x+" "+bornPoint[id%4].y);
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
            player.update(platforms, deathRegion, playerList); // call Sprite的update來用
        }
    }

    // 畫所有東西
    public void draw(Graphics g) {
        Graphics bufferg;
        if(bufferPage == null)
            bufferPage = new BufferedImage(1440, 720, BufferedImage.TYPE_INT_ARGB);
        bufferg = bufferPage.getGraphics();

        bufferg.setColor(map.getbgColor());
        bufferg.fillRect(0, 0, 1440, 720);
        // 畫地圖
        for (Rectangle p : platforms){
            bufferg.setColor(new Color(0,0,0,100));
            bufferg.fillRect(p.x-2, p.y-2, p.width+4, p.height+4);
            bufferg.setColor(map.getplatformColor());
            bufferg.fillRect(p.x, p.y, p.width, p.height);
        }
        
        for (Rectangle d : deathRegion){
            bufferg.setColor(Color.black);
            bufferg.fillRect(d.x, d.y, d.width, d.height);
        }

        bufferg.setColor(map.getwordColor());
        bufferg.setFont(new Font("Arial", Font.BOLD, 100));
        bufferg.drawString("Java", WordPos.get(0).x, WordPos.get(0).y);
        bufferg.setFont(new Font("Arial", Font.BOLD, 65));
        bufferg.drawString("Network Programming", WordPos.get(1).x, WordPos.get(1).y);

        // 畫玩家
        for(Sprite player : players.values()) {
            if(player.isAlive()){
                BufferedImage plr;
                try {
                    // System.out.println(player.getX()+" "+player.getY());
                    // 讀取 JPG 檔案
                    String path = "plr/plr"+String.valueOf(player.getId()%4+1)+".png";
                    plr = ImageIO.read(new File(path));
                    Image scaledPlr = plr.getScaledInstance(30, 30, Image.SCALE_SMOOTH); // 把求變平滑
                    bufferg.drawImage(scaledPlr,(int)player.getX(),(int)player.getY(),30,30,this);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("無法讀取圖片檔案！");
                }
            }
        }

        bufferg.dispose();
        g.drawImage(bufferPage, getInsets().left, getInsets().top, this);
    }

    public ConcurrentHashMap<Integer, Sprite> getPlayers() {return players;} // server要用
    // public void setmapindex(int x){ mapindex=x; }

    
}