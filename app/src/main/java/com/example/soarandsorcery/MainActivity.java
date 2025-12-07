package com.example.soarandsorcery;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private FrameLayout gameContainer;
    private GameView gameView;

    private View startScreen;
    private TextView titleText;
    private Button playButton;
    private ImageButton leftButton;
    private ImageButton rightButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startScreen = findViewById(R.id.startScreen);
        titleText = findViewById(R.id.titleText);
        playButton = findViewById(R.id.playButton);
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);

        gameContainer = findViewById(R.id.gameContainer);
        gameView = new GameView(this);
        gameContainer.addView(gameView);
        gameContainer.setVisibility(View.GONE);

        playButton.setOnClickListener(v -> startGame());
    }

    private void startGame() {
        startScreen.setVisibility(View.GONE);
        gameContainer.setVisibility(View.VISIBLE);

        gameView.startCountdown(this::showGameOver);
    }

    private void showGameOver() {
        GameOverDialog dialog = new GameOverDialog(this::returnToStartScreen);
        dialog.show(getSupportFragmentManager(), "game_over");
    }

    private void returnToStartScreen() {
        gameContainer.setVisibility(View.GONE);
        startScreen.setVisibility(View.VISIBLE);
        gameView.resetGame();
    }
}
