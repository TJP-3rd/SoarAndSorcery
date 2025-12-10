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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FrameLayout gameContainer;
    private GameView gameView;

    // UI screens
    private View startScreen, nicknameScreen, highscoresScreen, worldScoresScreen;

    private LinearLayout highscoresContainer, worldScoresContainer;
    private TextView scoreTitle, letter1, letter2, letter3;

    private int lastScore = 0;

    private static final String PREFS_NAME = "knight_skies_prefs";
    private static final String KEY_HIGHSCORES = "highscores";

    // ✅ Firestore
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        gameContainer = findViewById(R.id.gameContainer);
        gameView = new GameView(this);
        gameContainer.addView(gameView);
        gameContainer.setVisibility(View.GONE);

        // Views
        startScreen = findViewById(R.id.startScreen);
        nicknameScreen = findViewById(R.id.nicknameInclude);
        highscoresScreen = findViewById(R.id.highscoresInclude);
        worldScoresScreen = findViewById(R.id.worldScoresInclude);

        highscoresContainer = findViewById(R.id.highscoresContainer);
        worldScoresContainer = findViewById(R.id.worldScoresContainer);

        scoreTitle = findViewById(R.id.scoreTitle);
        letter1 = findViewById(R.id.letter1);
        letter2 = findViewById(R.id.letter2);
        letter3 = findViewById(R.id.letter3);

        Button playButton = findViewById(R.id.playButton);
        ImageButton leftButton = findViewById(R.id.leftButton);
        ImageButton rightButton = findViewById(R.id.rightButton);

        Button confirmButton = findViewById(R.id.confirmButton);
        Button up1 = findViewById(R.id.up1);
        Button up2 = findViewById(R.id.up2);
        Button up3 = findViewById(R.id.up3);

        Button down1 = findViewById(R.id.down1);
        Button down2 = findViewById(R.id.down2);
        Button down3 = findViewById(R.id.down3);

        up1.setOnClickListener(v -> changeLetter(letter1, true));
        up2.setOnClickListener(v -> changeLetter(letter2, true));
        up3.setOnClickListener(v -> changeLetter(letter3, true));

        down1.setOnClickListener(v -> changeLetter(letter1, false));
        down2.setOnClickListener(v -> changeLetter(letter2, false));
        down3.setOnClickListener(v -> changeLetter(letter3, false));


        Button worldReturnButton = findViewById(R.id.worldReturnButton);
        Button highscoresReturnButton = findViewById(R.id.returnButton);

        // Start game
        playButton.setOnClickListener(v -> startGame());

        // World / Local
        leftButton.setOnClickListener(v -> showWorldScoresScreen());
        rightButton.setOnClickListener(v -> showHighscoresScreen());

        // Save score
        confirmButton.setOnClickListener(v -> {
            String name = "" + letter1.getText() + letter2.getText() + letter3.getText();

            addScoreIfTop10(name, lastScore);        // Local
            checkAndUploadWorldScore(name, lastScore); // Firestore

            nicknameScreen.setVisibility(View.GONE);
            startScreen.setVisibility(View.VISIBLE);
            gameView.resetGame();
        });

        worldReturnButton.setOnClickListener(v -> {
            worldScoresScreen.setVisibility(View.GONE);
            startScreen.setVisibility(View.VISIBLE);
        });

        highscoresReturnButton.setOnClickListener(v -> {
            highscoresScreen.setVisibility(View.GONE);
            startScreen.setVisibility(View.VISIBLE);
        });
    }

    private void startGame() {
        startScreen.setVisibility(View.GONE);
        gameContainer.setVisibility(View.VISIBLE);

        gameView.startCountdown(score -> runOnUiThread(() -> onGameOver(score)));
    }

    private void onGameOver(int score) {
        lastScore = score;
        showNicknameScreen(score);
    }

    private void showNicknameScreen(int score) {
        gameContainer.setVisibility(View.GONE);
        scoreTitle.setText("Your score: " + score);

        letter1.setText("A");
        letter2.setText("A");
        letter3.setText("A");

        nicknameScreen.setVisibility(View.VISIBLE);
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


    // ---------- LOCAL HIGHSCORES ----------
    private void showHighscoresScreen() {
        highscoresContainer.removeAllViews();
        List<ScoreEntry> scores = loadHighscores();

        for (int i = 0; i < scores.size(); i++) {
            TextView t = new TextView(this);
            t.setText((i + 1) + ". " + scores.get(i).name + " - " + scores.get(i).score);
            t.setTextSize(22);
            t.setTextColor(0xFFFFFFFF);
            t.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            highscoresContainer.addView(t);
        }

        highscoresScreen.setVisibility(View.VISIBLE);
    }

    // ---------- WORLD FIREBASE SCORES ----------
    private void showWorldScoresScreen() {
        worldScoresContainer.removeAllViews();

        db.collection("world_highscores")
                .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(query -> {
                    int rank = 1;
                    for (QueryDocumentSnapshot doc : query) {
                        String name = doc.getString("name");
                        Long score = doc.getLong("score");

                        TextView t = new TextView(this);
                        t.setText(rank + ". " + name + " - " + score);
                        t.setTextSize(22);
                        t.setTextColor(0xFFFFFFFF);
                        t.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        worldScoresContainer.addView(t);
                        rank++;
                    }
                });

        worldScoresScreen.setVisibility(View.VISIBLE);
    }

    // ---------- FIREBASE TOP-10 CHECK ----------
    private void checkAndUploadWorldScore(String name, int score) {
        db.collection("world_highscores")
                .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(query -> {
                    int lowest = 0;

                    List<com.google.firebase.firestore.DocumentSnapshot> docs = query.getDocuments();

                    if (docs.size() == 10) {
                        lowest = docs.get(9).getLong("score").intValue();
                    }

                    if (docs.size() < 10 || score > lowest) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("name", name);
                        entry.put("score", score);

                        db.collection("world_highscores").add(entry);
                        Toast.makeText(this, "World highscore updated!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------- LOCAL STORAGE ----------
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
        Collections.sort(list, (a, b) -> Integer.compare(b.score, a.score));

        if (list.size() > 10) list = list.subList(0, 10);

        saveHighscores(list);
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
