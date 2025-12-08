package com.example.soarandsorcery;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FrameLayout gameContainer;
    private GameView gameView;

    // Start screen
    private View startScreen;
    private TextView titleText;
    private Button playButton;
    private ImageButton leftButton;
    private ImageButton rightButton;

    // Nickname screen
    private View nicknameScreen;
    private TextView scoreTitle;
    private TextView letter1, letter2, letter3;
    private Button up1, up2, up3, down1, down2, down3, confirmButton;

    // Highscores screen
    private View highscoresScreen;
    private LinearLayout highscoresContainer;
    private Button highscoresReturnButton;

    private int lastScore = 0;

    private static final String PREFS_NAME = "knight_skies_prefs";
    private static final String KEY_HIGHSCORES = "highscores";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start screen
        startScreen = findViewById(R.id.startScreen);
        titleText = findViewById(R.id.titleText);
        playButton = findViewById(R.id.playButton);
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);

        // Game container
        gameContainer = findViewById(R.id.gameContainer);
        gameView = new GameView(this);
        gameContainer.addView(gameView);
        gameContainer.setVisibility(View.GONE);

        // Nickname screen
        nicknameScreen = findViewById(R.id.nicknameInclude);
        scoreTitle = findViewById(R.id.scoreTitle);
        letter1 = findViewById(R.id.letter1);
        letter2 = findViewById(R.id.letter2);
        letter3 = findViewById(R.id.letter3);
        up1 = findViewById(R.id.up1);
        up2 = findViewById(R.id.up2);
        up3 = findViewById(R.id.up3);
        down1 = findViewById(R.id.down1);
        down2 = findViewById(R.id.down2);
        down3 = findViewById(R.id.down3);
        confirmButton = findViewById(R.id.confirmButton);

        // Highscores screen
        highscoresScreen = findViewById(R.id.highscoresInclude);
        highscoresContainer = findViewById(R.id.highscoresContainer);
        highscoresReturnButton = findViewById(R.id.returnButton);

        // Start game
        playButton.setOnClickListener(v -> startGame());

        // Show highscores when right button is pressed
        rightButton.setOnClickListener(v -> showHighscoresScreen());

        // Letter cycling controls
        up1.setOnClickListener(v -> changeLetter(letter1, true));
        up2.setOnClickListener(v -> changeLetter(letter2, true));
        up3.setOnClickListener(v -> changeLetter(letter3, true));

        down1.setOnClickListener(v -> changeLetter(letter1, false));
        down2.setOnClickListener(v -> changeLetter(letter2, false));
        down3.setOnClickListener(v -> changeLetter(letter3, false));

        // Confirm nickname + score
        confirmButton.setOnClickListener(v -> {
            String name = "" + letter1.getText() + letter2.getText() + letter3.getText();
            addScoreIfTop10(name, lastScore);

            // Back to start screen
            nicknameScreen.setVisibility(View.GONE);
            highscoresScreen.setVisibility(View.GONE);
            gameContainer.setVisibility(View.GONE);

            startScreen.setVisibility(View.VISIBLE);
            gameView.resetGame();
        });

        // Return from highscores
        highscoresReturnButton.setOnClickListener(v -> {
            highscoresScreen.setVisibility(View.GONE);
            startScreen.setVisibility(View.VISIBLE);
        });
    }

    private void startGame() {
        startScreen.setVisibility(View.GONE);
        nicknameScreen.setVisibility(View.GONE);
        highscoresScreen.setVisibility(View.GONE);

        gameContainer.setVisibility(View.VISIBLE);

        // Start game with callback
        gameView.startCountdown(score -> runOnUiThread(() -> onGameOver(score)));
    }

    private void onGameOver(int score) {
        lastScore = score;
        showNicknameScreen(score);
    }

    private void showNicknameScreen(int score) {
        gameContainer.setVisibility(View.GONE);
        startScreen.setVisibility(View.GONE);
        highscoresScreen.setVisibility(View.GONE);

        scoreTitle.setText("Your score: " + score);

        // Reset letters
        letter1.setText("A");
        letter2.setText("A");
        letter3.setText("A");

        nicknameScreen.setVisibility(View.VISIBLE);
    }

    private void showHighscoresScreen() {
        startScreen.setVisibility(View.GONE);
        nicknameScreen.setVisibility(View.GONE);
        gameContainer.setVisibility(View.GONE);

        highscoresContainer.removeAllViews();
        List<ScoreEntry> scores = loadHighscores();

        if (scores.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No highscores yet");
            empty.setTextSize(18);
            empty.setTextColor(0xFFFFFFFF);
            highscoresContainer.addView(empty);
        } else {
            int rank = 1;
            for (ScoreEntry s : scores) {
                TextView t = new TextView(this);
                t.setText(rank + ". " + s.name + " - " + s.score);
                t.setTextSize(18);
                t.setTextColor(0xFFFFFFFF);
                t.setPadding(0, 8, 0, 8);
                highscoresContainer.addView(t);
                rank++;
            }
        }

        highscoresScreen.setVisibility(View.VISIBLE);
    }

    private void changeLetter(TextView tv, boolean up) {
        char c = tv.getText().charAt(0);
        if (up) {
            c = (char) ((c - 'A' + 1) % 26 + 'A');
        } else {
            c = (char) ((c - 'A' - 1 + 26) % 26 + 'A');
        }
        tv.setText(String.valueOf(c));
    }

    private List<ScoreEntry> loadHighscores() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HIGHSCORES, null);

        List<ScoreEntry> list = new ArrayList<>();
        if (json == null) return list;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new ScoreEntry(o.getString("name"), o.getInt("score")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }

    private void saveHighscores(List<ScoreEntry> list) {
        JSONArray arr = new JSONArray();
        try {
            for (ScoreEntry e : list) {
                JSONObject o = new JSONObject();
                o.put("name", e.name);
                o.put("score", e.score);
                arr.put(o);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_HIGHSCORES, arr.toString()).apply();
    }

    private void addScoreIfTop10(String name, int score) {
        List<ScoreEntry> list = loadHighscores();
        list.add(new ScoreEntry(name, score));

        // Sort descending
        Collections.sort(list, (a, b) -> Integer.compare(b.score, a.score));

        if (list.size() > 10) list = list.subList(0, 10);

        boolean madeTop10 = false;
        for (ScoreEntry e : list) {
            if (e.name.equals(name) && e.score == score) {
                madeTop10 = true;
                break;
            }
        }

        if (madeTop10) {
            saveHighscores(list);
            Toast.makeText(this, "Highscore saved!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Not in top 10", Toast.LENGTH_SHORT).show();
        }
    }

    private static class ScoreEntry {
        final String name;
        final int score;

        ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }
}
