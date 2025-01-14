package game;

import com.google.gson.Gson;
import game.client.PlayerClient;
import game.model.EndGameSubscene;
import game.view.models.EnemyPlayer;
import game.view.models.Food;
import game.view.models.Player;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HostGameController {

    private HashMap<KeyCode, Boolean> keys;
    private CopyOnWriteArrayList<Food> foodEntities;
    private ArrayList<Node> platforms;
    private Pane gamePane;
    private Player player;
    private EnemyPlayer enemy;
    private int firstPlayerScore;
    private int secondPlayerScore;
    private Label timerLabel;
    private Label firstPlayerScores;
    private Label secondPlayerScores;
    private int[] time = {120};
    private boolean isTimeIsUp = false;
    private EndGameSubscene endGameSubscene;
    private int idCounter = 0;
    private PlayerClient client;
    public static final Gson gson = new Gson();

    public HostGameController(Pane gamePane, ArrayList<Node> platforms, Player player, Label timer, Label firstPlayerScores,
                              HashMap<KeyCode, Boolean> keys, PlayerClient client, Label secondPlayerScores) {
        this.gamePane = gamePane;
        this.platforms = platforms;
        this.player = player;
        firstPlayerScore = 0;
        timerLabel = timer;
        this.firstPlayerScores = firstPlayerScores;
        this.keys = keys;
        this.client = client;
        this.secondPlayerScores = secondPlayerScores;
    }


    public void startGame() {
        AnimationTimer gameTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                System.out.println(enemy);
                if (enemy != null) {
                    AnimationTimer updateTimer = new AnimationTimer() {
                        @Override
                        public void handle(long l) {
                            update();
                        }
                    };
                    updateTimer.start();
                    setGameTimer();
                    spawnFood();
                    foodEntities = new CopyOnWriteArrayList<>();
                    System.out.println("GAME BEGINS");
                    AnimationTimer mainGameTimer = new AnimationTimer() {
                        @Override
                        public void handle(long l) {
                            eatFood();
                        }
                    };
                    mainGameTimer.start();

                    AnimationTimer timeTimer = new AnimationTimer() {
                        @Override
                        public void handle(long l) {
                            if (isTimeIsUp) {
                                gameEnding(checkWinner());
                                updateTimer.stop();
                                this.stop();
                            }
                        }
                    };
                    timeTimer.start();
                    this.stop();
                }
            }
        };
        gameTimer.start();
    }

    private String checkWinner() {
        if (firstPlayerScore > secondPlayerScore) {
            return "Host player"; //TODO: сделать никнеймы
        } else if (firstPlayerScore == secondPlayerScore) {
            return "Friendship";
        } else {
            return "Connected player";
        }
    }

    private void gameEnding(String winner) {
        endGameSubscene = new EndGameSubscene(winner);
        endGameSubscene.setLayoutX(285);
        endGameSubscene.setLayoutY(150);
        gamePane.getChildren().add(endGameSubscene);
    }

    private void setGameTimer() {
        Timeline gameTimer = new Timeline(
                new KeyFrame(
                        Duration.millis(1000),
                        actionEvent -> {
                            int minutes = (time[0] % 3600) / 60;
                            int seconds = time[0] % 60;
                            String formattedString = String.format("%02d.%02d", minutes, seconds);
                            timerLabel.setText(formattedString);

                            HashMap<String, String> message = new HashMap<>();
                            message.put("method", "updateTime");
                            message.put("parameter", formattedString);
                            client.sendMessage(gson.toJson(message) + "\n");
                            time[0]--;
                        }
                ));
        gameTimer.setCycleCount(121);
        gameTimer.play();
    }

    private void spawnFood() {
        Timeline spawnEntity = new Timeline(
                new KeyFrame(Duration.seconds(4 + (int) (Math.random() * 10)),
//                    new KeyFrame(Duration.seconds(1),
                        new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                Food food = createFoodEntitiy();
                                gamePane.getChildren().add(food.getFoodNode());
                                foodEntities.add(food);

                                HashMap<String, String> message = new HashMap<>();
                                message.put("method", "newFood");
                                message.put("foodId", Integer.toString(food.getFoodId()));
                                message.put("x", Double.toString(food.getFoodNode().getTranslateX()));
                                message.put("y", Double.toString(food.getFoodNode().getTranslateY()));
                                client.sendMessage(gson.toJson(message) + "\n");

                                System.out.println(foodEntities);
                                System.out.println("ADDED NEW FOOD");

                            }
                        }));
        spawnEntity.setCycleCount(Timeline.INDEFINITE);

        AnimationTimer spawnTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                if (time[0] <= -1) {
                    HashMap<String, String> message = new HashMap<>();
                    message.put("method", "endTime");
                    client.sendMessage(gson.toJson(message) + "\n");
                    isTimeIsUp = true;
                    spawnEntity.stop();
                    this.stop();
                }
            }
        };
        spawnTimer.start();
        spawnEntity.play();

    }

    private Node createEntityWithoutAdding(int x, int y, int w, int h, Image image) {
        Rectangle entity = new Rectangle(w, h);
        entity.setTranslateX(x);
        entity.setTranslateY(y);
        entity.setFill(new ImagePattern(image));

        return entity;
    }

    public Food createFoodEntitiy() {
        Food food = null;

        for (Node platform : platforms) {
            if (platform.getTranslateX() > 0 && platform.getTranslateY() > 0
                    && platform.getTranslateX() < 920 && platform.getTranslateY() < 740) {
                if (Math.random() < 0.5) {
                    food = new Food(idCounter, createEntityWithoutAdding((int) platform.getTranslateX() + 40, (int) platform.getTranslateY() - 20, 20, 20, new Image("food.png")));
                    idCounter++;
                    Collections.shuffle(platforms);
                    break;
                }
            }
        }
        return food;
    }

    private void eatFood() {
        if (foodEntities.size() != 0) {
            for (Food food : foodEntities) {
                if (player.getPlayerNode().getBoundsInParent().intersects(food.getFoodNode().getBoundsInParent())) {
                    HashMap<String, String> message = new HashMap<>();
                    message.put("method", "eat");
                    message.put("foodId", Integer.toString(food.getFoodId()));
                    client.sendMessage(gson.toJson(message) + "\n");
                    gamePane.getChildren().remove(food.getFoodNode());
                    firstPlayerScore += 1;
                    firstPlayerScores.setText(Integer.toString(firstPlayerScore));
                    foodEntities.remove(food);
                }
            }
        }
    }

    private void update() {
        if (isPressed(KeyCode.W) && player.getPlayerNode().getTranslateY() >= 5) {
            player.jumpPlayer();
        }
        if (isPressed(KeyCode.A) && player.getPlayerNode().getTranslateX() >= 5) {
            player.movePlayerX(-5, platforms);
        }
        if (isPressed(KeyCode.D) && player.getPlayerNode().getTranslateX() >= 5) {
            player.movePlayerX(5, platforms);
        }
        if (player.getPlayerVelocity().getY() < 10) {
            player.setPlayerVelocity(player.getPlayerVelocity().add(0, 1));
        }
        player.movePlayerY((int) player.getPlayerVelocity().getY(), platforms);

        HashMap<String, String> message = new HashMap<>();
        message.put("method", "move");
        message.put("x", Double.toString(player.getPlayerNode().getTranslateX()));
        message.put("y", Double.toString(player.getPlayerNode().getTranslateY()));
        client.sendMessage(gson.toJson(message) + "\n");
    }

    public synchronized void createEnemy() {
        javafx.application.Platform.runLater(() -> {
            if (enemy == null) {
                System.out.println("created Enemy");
                enemy = new EnemyPlayer(createEntityWithoutAdding(500, 200, 40, 40, new Image("enemy_cat.png")));
                gamePane.getChildren().add(enemy.getPlayerNode());
            }
        });
    }

    public synchronized void moveEnemy(double x, double y) {
        javafx.application.Platform.runLater(() -> {
            enemy.getPlayerNode().setTranslateX(x);
            enemy.getPlayerNode().setTranslateY(y);
        });
    }

    public synchronized void enemyEatFood(int foodId) {
        javafx.application.Platform.runLater(() -> {
            for (Food food : foodEntities) {
                if (food.getFoodId() == foodId) {
                    gamePane.getChildren().remove(food.getFoodNode());
                    secondPlayerScore += 1;
                    secondPlayerScores.setText(Integer.toString(secondPlayerScore));
                    foodEntities.remove(food);
                }
            }
        });
    }

    private boolean isPressed(KeyCode key) {
        return keys.getOrDefault(key, false);
    }

    public EnemyPlayer getEnemy() {
        return enemy;
    }
}
