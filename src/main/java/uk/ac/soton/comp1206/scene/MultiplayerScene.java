package uk.ac.soton.comp1206.scene;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.component.GameBoard;
import uk.ac.soton.comp1206.component.PieceBoard;
import uk.ac.soton.comp1206.event.BlockClickedListener;
import uk.ac.soton.comp1206.event.NextPieceListener;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.multimedia.Multimedia;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static uk.ac.soton.comp1206.scene.MenuScene.muteSwitcher;

public class MultiplayerScene extends ChallengeScene{

    private static final Logger logger = LogManager.getLogger(MenuScene.class);
    private final Communicator communicator;

    protected MultiplayerGame game;
    private PieceBoard currentPiece;
    private PieceBoard secondPiece;
    protected int x = 0;
    protected int y = 0;
    protected GameBoard board;
    protected Rectangle timer;
    protected StackPane timerStack;
    private BufferedReader bufferedReader;
    private int highScoreInteger;
    private SimpleIntegerProperty highScoreProperty = new SimpleIntegerProperty(0);
    public VBox otherPlayersScore;




    //Listen to new pieces inside game and call the method
    private NextPieceListener nextPieceListener;
    private BlockClickedListener blockClickedListener;


    /**
     * Create a new Multiplayer Player challenge scene
     *
     * @param gameWindow the Game Window
     */
    public MultiplayerScene(GameWindow gameWindow) {
        super(gameWindow);
        logger.info("Creating Multiplayer Scene");
        communicator = gameWindow.getCommunicator();
    }


    public void build() {
        logger.info("Building " + getClass().getName());

        setupGame();

        root = new GamePane(gameWindow.getWidth(), gameWindow.getHeight());

        var challengePane = new StackPane();
        challengePane.setMaxWidth(gameWindow.getWidth());
        challengePane.setMaxHeight(gameWindow.getHeight());
        challengePane.getStyleClass().add("starWarsChallenge-background");
        root.getChildren().add(challengePane);

        //BorderPane
        var mainPane = new BorderPane();
        challengePane.getChildren().add(mainPane);

        board = new GameBoard(game.getGrid(),gameWindow.getWidth()/2.05,gameWindow.getWidth()/2.05);
        mainPane.setCenter(board);

        //Start editing

        //Make the top section

        //Create the top section which will contain score name of game and lives
        GridPane topSection = new GridPane();
        mainPane.setTop(topSection);
        topSection.setPadding(new Insets(10, 10, 10, 10));
        //topSection.setVgap(10);
        //topSection.setHgap(10);

        //Create Challenge mode text
        Text challengeModeText = new Text("Challenge Mode");
        challengeModeText.getStyleClass().add("starWarsMandalorThemeHeading");
        challengeModeText.setTextAlignment(TextAlignment.CENTER);
        GridPane.setConstraints(challengeModeText,1,0);
        challengeModeText.setTextAlignment(TextAlignment.CENTER);

        //Make the score Box

        //Create score label
        Text scorePoints = new Text("0");
        scorePoints.setTextAlignment(TextAlignment.CENTER);
        scorePoints.getStyleClass().add("starWarsScore");
        scorePoints.textProperty().bind(game.scoreProperty().asString());

        //Create the "Score" Text
        Text literallyScoreText = new Text("Score");
        literallyScoreText.getStyleClass().add("starWarsScore");
        literallyScoreText.setTextAlignment(TextAlignment.CENTER);

        //Create the vertical VBoc to display the score
        VBox scoreBox = new VBox();
        scoreBox.getChildren().add(literallyScoreText);
        scoreBox.getChildren().add(scorePoints);
        GridPane.setConstraints(scoreBox,0,0);
        scoreBox.setAlignment(Pos.CENTER);

        //Create lives counter interface

        Text literallyLivesCounterText = new Text("Lives");
        literallyLivesCounterText.getStyleClass().add("starWarsLives");
        String livesCounterString = "";
        Text livesCounter = new Text(livesCounterString);
        livesCounter.getStyleClass().add("starWarsLives");
        livesCounter.setTextAlignment(TextAlignment.CENTER);
        livesCounter.textProperty().bind(game.livesProperty().asString());
        game.livesProperty().addListener((observable, oldValue, newValue) -> {
            communicator.send("LIVES " + newValue);

        });
        VBox livesBox = new VBox();
        livesBox.getChildren().add(literallyLivesCounterText);
        livesBox.getChildren().add(livesCounter);
        GridPane.setConstraints(livesBox,2,0);
        livesBox.setAlignment(Pos.CENTER);

        //Add all the elements in the GridPane
        topSection.getChildren().addAll(scoreBox,challengeModeText,livesBox);
        GridPane.setFillWidth(challengeModeText, true);
        GridPane.setHgrow(challengeModeText, Priority.ALWAYS);
        GridPane.setHalignment(challengeModeText, HPos.CENTER);

        //Creating the rightBar
        VBox rightBar = new VBox();
        mainPane.setRight(rightBar);
        rightBar.setAlignment(Pos.CENTER);
        rightBar.setSpacing(6);
        rightBar.setPadding(new Insets(5, 5, 5, 5));

        VBox versusBox = new VBox();
        versusBox.setAlignment(Pos.CENTER);
        rightBar.getChildren().add(versusBox);
        versusBox.setMinSize(40,40);
        versusBox.setPadding(new Insets(20, 5,20,20));

        otherPlayersScore = new VBox();
        versusBox.getChildren().add(otherPlayersScore);

        //Create the level display
        Text literallyLevelText = new Text("Level");
        rightBar.getChildren().add(literallyLevelText);
        literallyLevelText.getStyleClass().add("starWarsLevel");

        Text level = new Text("3");
        rightBar.getChildren().add(level);
        level.getStyleClass().add("starWarsLevel");
        level.textProperty().bind(game.levelProperty().asString());

        //Create the current piece board
        currentPiece = new PieceBoard(3, 3, gameWindow.getWidth() / 7, gameWindow.getWidth() / 7);
        rightBar.getChildren().add(currentPiece);
        currentPiece.setOnBlockClick(event -> rotate(1));

        //Create the second piece
        secondPiece = new PieceBoard(3,3,gameWindow.getWidth() / 10, gameWindow.getWidth() / 10);
        rightBar.getChildren().add(secondPiece);
        secondPiece.setOnBlockClick(event -> swap());

        currentPiece.showCentre();
        //secondPiece.showCentre();


        Text versusText = new Text("Versus");
        versusText.getStyleClass().add("starWarsVersus");
        versusBox.getChildren().add(versusText);
        versusBox.getChildren().add(game.multiplayerPlayersScore);

        //create a new time bar
        timerStack = new StackPane();
        mainPane.setBottom(timerStack);
        timerStack.setAlignment(Pos.CENTER);
        timer = new Rectangle();
        timer.setArcHeight(30);
        timer.setArcWidth(30);
        timer.setHeight(25);
        timer.setFill(Color.WHITE);
        BorderPane.setMargin(timerStack, new Insets(10, 5, 10, 5));
        timerStack.getChildren().add(timer);
        StackPane.setAlignment(timer, Pos.CENTER);

        //Handle block on gameBoard grid being clicked
        board.setOnBlockClick(this::blockClicked);
        board.setRightClickedListener(() -> {
            rotate(1);
        });
    }

    /**
     * Setup the game object and model
     */
    @Override
    protected void setupGame() {
        logger.info("Starting a new challenge");
        game = new MultiplayerGame(communicator,5, 5);
    }

    /**
     * Initialise the scene and start the game
     */
    @Override
    public void initialise() {
        logger.info("Initialising Challenge");
        Multimedia.stop();
        if(Multimedia.shouldPlayMusic){
            Multimedia.startPlayBackgroundMusic("o_fortuna.mp3");
        }
        game.setNextPieceListener((currentPiece1, secondPiece1) -> {
            currentPiece.setPiece(currentPiece1);
            secondPiece.setPiece(secondPiece1);
        });
        scene.setOnKeyPressed(this::keyListener);
        game.setOnGameLoop(this::changeRound);
        game.setOnGameOver(this::gameOver);
        game.setLineClearedListener(this::fadeLinesImplementedInChallengeScene);
        game.start();
    }

    /**
     * Rotate the piece
     * @param rotate
     */
    protected void rotate(int rotate) {
        logger.info("Rotate successful");
        game.rotateCurrentPiece(rotate);
        currentPiece.setPiece(game.getCurrentPiece());
        Multimedia.playAudio("rotate 1.mp3");
    }

    /**
     * Swap the piece
     */
    private void swap() {
        game.swapCurrentPiece();
        currentPiece.setPiece(game.getCurrentPiece());
        secondPiece.setPiece(game.getSecondPiece());
        Multimedia.playAudio("swap sound.wav");
    }

    /**
     * Handle when a block is clicked
     * @param gameBlock the Game Block that was clicked
     */
    private void blockClicked(GameBlock gameBlock) {
        if (game.blockClicked(gameBlock)) {
            Multimedia.playAudio("blaster-firing.mp3");
            game.restartGameLoop();
        } else {
            Multimedia.playAudio("fail cool.mp3");
        }
    }

    /**
     * Make the display bar
     * @param getTimerDelay
     */
    protected void changeRound(int getTimerDelay) {
        //HelpDesk did this
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(timer.widthProperty(), timerStack.getWidth())),
                new KeyFrame(new Duration( 0), new KeyValue(timer.fillProperty(), Color.WHITE)),
                //make transition faster
                new KeyFrame(new Duration( getTimerDelay * 0.7), new KeyValue(timer.fillProperty(), Color.WHITE)),
                new KeyFrame(new Duration( getTimerDelay * 0.9), new KeyValue(timer.fillProperty(), Color.rgb(176, 13, 35))),
                new KeyFrame(new Duration(getTimerDelay), new KeyValue(timer.widthProperty(), 0))
        );
        timeline.play();

    }

    //fade out one by one in every call
    protected void fadeLinesImplementedInChallengeScene(int x,int y) {
        board.passTheGameBlockToFade(x,y);
    }

    /**
     * game over
     */
    protected void gameOver() {
        logger.info("Game over");
        communicator.send("DIE");
        game.stop();
        Multimedia.stop();
        gameWindow.startScores(game,true);
    }

    /**
     * Exit the game
     */
    private void exitChallengeScene() {
        gameOver();
        Multimedia.checkIfMusicShouldBePlayer();
        gameWindow.startMenu();
    }

    /**
     * Handle key events
     * @param key
     */
    public void keyListener (KeyEvent key) {
        boolean switcherValue;
        if (key.getCode().equals(KeyCode.M)){
            switcherValue = muteSwitcher % 2 == 0;
            muteSwitcher++;
            logger.info("Switcher value passed : " + switcherValue);
            Multimedia.shouldPlayMusic(switcherValue);
        } else {
            board.deHover(board.getBlock(board.returnX(),board.returnY()));
            if(key.getCode() == KeyCode.ESCAPE){
                exitChallengeScene();
            } else if (key.getCode() == KeyCode.SPACE || key.getCode() == KeyCode.R){
                swap();
            } else if (key.getCode() == KeyCode.Q || key.getCode() == KeyCode.C || key.getCode() == KeyCode.CLOSE_BRACKET || key.getCode() == KeyCode.OPEN_BRACKET) {
                rotate(1);
            } else if (key.getCode() == KeyCode.ENTER || key.getCode() == KeyCode.X) {
                blockClicked(board.getBlock(x,y));
            } else if (key.getCode() == KeyCode.W || key.getCode() == KeyCode.UP) {
                board.deHover(board.getBlock(x,y));
                if(y > 0) {
                    y--;
                }
            } else if (key.getCode() == KeyCode.S || key.getCode() == KeyCode.DOWN) {
                board.deHover(board.getBlock(x,y));
                if(y < game.getRows() - 1) {
                    y++;
                }
            } else if (key.getCode() == KeyCode.A || key.getCode() == KeyCode.LEFT) {
                board.deHover(board.getBlock(x,y));
                if(x > 0){
                    x--;
                }
            } else if (key.getCode() == KeyCode.D || key.getCode() == KeyCode.RIGHT) {
                board.deHover(board.getBlock(x,y));
                if(x < game.getCols() - 1){
                    x++;
                }

            }
            board.hover(board.getBlock(x, y));
        }
    }



}
