/*Sprite用來當作遊戲中所有玩家資訊的superclass */
import java.awt.*;
import java.util.List;

public class Sprite {
    private double x, y; // 座標
    private double vX, vY; // 速度 
    private static final double GRAVITY = 0.6; // 重力
    private static final double MOVE_SPEED = 4; // 移動速度
    private static final double JUMP_FORCE = -15; // 跳躍力
    private static final double FRICTION = 0.98; // 速度每次減少2%
    private int id; // 玩家id
    private boolean isAlive = true; 
    private boolean isOnGround = false;
    private Color color;
    private static final int BALL_SIZE = 30;

    public Sprite(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        // random顏色
        this.color = new Color(
            (float)Math.random(), 
            (float)Math.random(), 
            (float)Math.random()
        );
    }

    // 更新玩家資訊和地圖
    public void update(List<Rectangle> platforms, List<Sprite> otherPlayers) {
        if(!isAlive) return;  // 死了
        // 球一直保持往下掉
        vY += GRAVITY;

        // 加上摩擦力
        vX *= FRICTION;
        vY *= FRICTION;

        // 更新球座標
        x += vX;
        y += vY;

        // 檢查碰到地板
        isOnGround = false;
        Rectangle bounds = new Rectangle((int)x, (int)y, BALL_SIZE, BALL_SIZE); // 球的範圍，hitbox的概念
        for(Rectangle platform : platforms) { // 檢查每個地板
            if(bounds.intersects(platform)) { // 如果球在地板上方
                if(vY > 0) {
                    y = platform.y - BALL_SIZE; // 球的底部和地板的頂部對齊
                    vY = 0;
                    isOnGround = true;
                }
            }
        }
        // 球不能穿過flatform
        for(Rectangle platform : platforms) {
            if(bounds.intersects(platform)) {
                if(vY < 0) {
                    if(y + BALL_SIZE > platform.y && y < platform.y) {
                        y = platform.y + platform.height; // y是往下增加的所以用+
                        vY = 0;
                    }
                }
            }
        }

        // 檢查撞到其他玩家
        for(Sprite otherPlayer : otherPlayers) {
            if(otherPlayer != this && otherPlayer.isAlive) { // 如果不是自己
                double dx = (otherPlayer.x + BALL_SIZE/2) - (this.x + BALL_SIZE/2); // x方向距離
                double dy = (otherPlayer.y + BALL_SIZE/2) - (this.y + BALL_SIZE/2); // y方向距離
                double distance = Math.sqrt(dx*dx + dy*dy); // 兩科球的距離
                if(distance < BALL_SIZE) { // 發生碰撞
                    double angle = Math.atan2(dy, dx); // 碰撞角度
                    double force = 3; // 碰撞多大力
                    // 改對方的 x 和 y 速度
                    otherPlayer.vX += Math.cos(angle) * force; 
                    otherPlayer.vY += Math.sin(angle) * force;
                    // 改自己的 x 和 y 速度
                    vX -= Math.cos(angle) * force;
                    vY -= Math.sin(angle) * force;
                }
            }
        }

        // 檢查邊界
        if(y > 720 || y < -100 || x < -100 || x > 1500) {
            isAlive = false;
        }
    }

    public void moveLeft() {
        vX = -MOVE_SPEED;
    }

    public void moveRight() {
        vX = MOVE_SPEED;
    }

    public void jump() {
        if(!isOnGround) return;
        vY = JUMP_FORCE;
    }

    public void increaseGravity() {
        vY = GRAVITY + 0.5;
    }

    public void draw(Graphics g) {
        if(!isAlive) return;
        g.setColor(color);
        g.fillOval((int)x, (int)y, BALL_SIZE, BALL_SIZE);
    }

    public double getX() {return x;}
    public double getY() {return y;}
    public double getVelocityX() {return vX;}
    public double getVelocityY() {return vY;}
    public int getId() {return id;}
    public boolean isAlive() {return isAlive;}
    public Color getColor() {return color;}
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public void setVelocity(double vx, double vy) {
        this.vX = vx;
        this.vY = vy;
    }
    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }
    public void setColor(Color color) {
        this.color = color;
    }
}