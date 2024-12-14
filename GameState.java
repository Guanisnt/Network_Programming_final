/*更新各種遊戲狀態 */
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap; // 有多執行續的Map，不用寫synchronized

public class GameState {
    

    private ConcurrentHashMap<Integer, Sprite> players = new ConcurrentHashMap<>(); // <playerID, sprite>
    private List<Rectangle> platforms = new ArrayList<>(); // 地板
    private static final Point[] bornPoint = {
        new Point(150, 100),
        new Point(600, 100),
        new Point(150, 300),
        new Point(600, 300)
    };

    public GameState() {
        // 初始化地板，之後畫地圖也可以用
        this.initPlatforms();
    }

    public void initPlatforms() {
        platforms.add(new Rectangle(100, 500, 1200, 20));
        platforms.add(new Rectangle(200, 400, 400, 20));
        platforms.add(new Rectangle(800, 400, 400, 20));
    }

    // 新增玩家
    public void addPlayer(int id) {
        Sprite player = new Sprite(id, bornPoint[id%4].x, bornPoint[id%4].y);  // 用id來決定出生點
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

    // 畫所有東西，我都先用內建的畫，之後要套圖片
    public void draw(Graphics g) {
        // 畫背景
        g.setColor(new Color(135, 206, 235)); // Sky blue
        g.fillRect(0, 0, 1440, 720);

        // 畫地板
        g.setColor(Color.GREEN);
        for(Rectangle platform : platforms) {
            g.fillRect(platform.x, platform.y, platform.width, platform.height);
        }
        
        // 畫玩家
        for(Sprite player : players.values()) {
            player.draw(g);
        }
    }

    public ConcurrentHashMap<Integer, Sprite> getPlayers() {return players;} // server要用

    
}
