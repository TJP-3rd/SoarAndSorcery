package com.example.soarandsorcery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

public class GameView extends View {

    private Bitmap knight, topTube, bottomTube, coin;
    private int knightX, knightY, knightWidth, knightHeight;
    private int castleWidth, castleHeight;
    private int canvasWidth, canvasHeight;
    private int gap = 400;
    private int gravity = 3;          // slower falling
    private int velocity = 0;
    private int castleVelocity = 8;   // slower tubes
    private int delay = 40;           // slower frame refresh

    private int[] tubeX = new int[2];
    private int[] tubeY = new int[2];
    private boolean[] passed = new boolean[2];

    private int coinX, coinY;
    private int coinWidth, coinHeight;
    private boolean coinActive = false;
    private int tubesPassedCount = 0;

    private int score = 0;
    private Paint scorePaint;

    private Handler handler = new Handler();
    private Runnable runnable;

    private Paint paint = new Paint();
    private Random random = new Random();

    private int tubeSpacing;

    // Game state
    private boolean gameRunning = false;
    private boolean countdownRunning = false;
    private int countdownValue = 3;
    private Paint countdownPaint = new Paint();
    private Runnable onGameOverCallback;

    // Ensure countdown waits for view
    private boolean viewReady = false;

    public GameView(Context context) {
        super(context);

        knight = BitmapFactory.decodeResource(getResources(), R.drawable.flappybirdup);
        topTube = BitmapFactory.decodeResource(getResources(), R.drawable.toptube);
        bottomTube = BitmapFactory.decodeResource(getResources(), R.drawable.bottomtube);
        coin = BitmapFactory.decodeResource(getResources(), R.drawable.coin);

        scorePaint = new Paint();
        scorePaint.setColor(Color.BLACK);
        scorePaint.setTextSize(50);
        scorePaint.setFakeBoldText(true);

        countdownPaint.setColor(Color.WHITE);
        countdownPaint.setTextSize(200);
        countdownPaint.setTextAlign(Paint.Align.CENTER);

        runnable = this::invalidate;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasWidth = w;
        canvasHeight = h;

        int birdTargetWidth = canvasWidth / 10;
        int birdTargetHeight = birdTargetWidth * knight.getHeight() / knight.getWidth();
        knight = Bitmap.createScaledBitmap(knight, birdTargetWidth, birdTargetHeight, false);
        knightWidth = knight.getWidth();
        knightHeight = knight.getHeight();

        int tubeTargetWidth = canvasWidth / 6;
        int tubeTargetHeight = tubeTargetWidth * topTube.getHeight() / topTube.getWidth();
        topTube = Bitmap.createScaledBitmap(topTube, tubeTargetWidth, tubeTargetHeight, false);
        bottomTube = Bitmap.createScaledBitmap(bottomTube, tubeTargetWidth, tubeTargetHeight, false);
        castleWidth = topTube.getWidth();
        castleHeight = topTube.getHeight();

        int coinTargetWidth = canvasWidth / 15;
        int coinTargetHeight = coinTargetWidth * coin.getHeight() / coin.getWidth();
        coin = Bitmap.createScaledBitmap(coin, coinTargetWidth, coinTargetHeight, false);
        coinWidth = coin.getWidth();
        coinHeight = coin.getHeight();

        gap = canvasHeight / 3;
        tubeSpacing = canvasWidth / 2 + castleWidth;

        viewReady = true;
        resetGame();
    }

    public void resetGame() {
        knightX = canvasWidth / 4;
        knightY = canvasHeight / 2;
        velocity = 0;
        score = 0;
        tubesPassedCount = 0;
        coinActive = false;

        for (int i = 0; i < 2; i++) {
            tubeX[i] = canvasWidth + i * tubeSpacing;
            tubeY[i] = getRandomTubeY();
            passed[i] = false;
        }

        gameRunning = false;
        countdownRunning = false;

        handler.removeCallbacks(countdownTick);
        invalidate();
    }

    private int getRandomTubeY() {
        int maxOffset = canvasHeight / 4;
        return -random.nextInt(maxOffset);
    }

    public void startCountdown(Runnable onGameOver) {
        this.onGameOverCallback = onGameOver;

        // Reset countdown and game state
        countdownValue = 3;
        countdownRunning = true;
        gameRunning = false;

        // Remove any previous callbacks
        handler.removeCallbacks(countdownTick);

        // Post the countdown to run after the view is fully laid out
        post(() -> handler.post(countdownTick));

        // Force redraw
        invalidate();
    }


    private Runnable countdownTick = new Runnable() {
        @Override
        public void run() {
            if (countdownValue > 0) {
                countdownValue--;
                invalidate();
                handler.postDelayed(this, 1000);
            } else {
                countdownRunning = false;
                gameRunning = true;
            }
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {

        // ==== COUNTDOWN STATE ====
        if (countdownRunning) {
            // Draw tubes (frozen)
            for (int i = 0; i < 2; i++) {
                canvas.drawBitmap(topTube, tubeX[i], tubeY[i], paint);
                canvas.drawBitmap(bottomTube, tubeX[i], tubeY[i] + castleHeight + gap, paint);
            }

            // Draw bird
            canvas.drawBitmap(knight, knightX, knightY, paint);

            // Draw countdown text
            canvas.drawText(
                    countdownValue == 0 ? "GO!" : String.valueOf(countdownValue),
                    canvasWidth / 2f,
                    canvasHeight / 2f,
                    countdownPaint
            );

            handler.postDelayed(runnable, delay);
            return;
        }

        // ==== PAUSED (menu) ====
        if (!gameRunning) {
            canvas.drawBitmap(knight, knightX, knightY, paint);
            handler.postDelayed(runnable, delay);
            return;
        }

        // ==== GAME RUNNING ====
        for (int i = 0; i < 2; i++) {
            canvas.drawBitmap(topTube, tubeX[i], tubeY[i], paint);
            canvas.drawBitmap(bottomTube, tubeX[i], tubeY[i] + castleHeight + gap, paint);

            tubeX[i] -= castleVelocity;

            if (tubeX[i] + castleWidth < 0) {
                int other = (i == 0) ? 1 : 0;
                tubeX[i] = tubeX[other] + tubeSpacing;
                tubeY[i] = getRandomTubeY();
                passed[i] = false;
            }

            if (tubeX[i] + castleWidth < knightX && !passed[i]) {
                passed[i] = true;
                score++;
                tubesPassedCount++;

                if (tubesPassedCount % 5 == 0 && !coinActive) {
                    spawnCoin();
                }
            }
        }

        if (coinActive) {
            coinX -= castleVelocity;
            canvas.drawBitmap(coin, coinX, coinY, paint);

            if (knightX < coinX + coinWidth && knightX + knightWidth > coinX &&
                    knightY < coinY + coinHeight && knightY + knightHeight > coinY) {
                score += 5;
                coinActive = false;
            }

            if (coinX + coinWidth < 0) {
                coinActive = false;
            }
        }

        velocity += gravity;
        knightY += velocity;

        canvas.drawBitmap(knight, knightX, knightY, paint);
        canvas.drawText("Score: " + score, 50, 150, scorePaint);

        // Collision detection
        for (int i = 0; i < 2; i++) {
            if (knightX + knightWidth > tubeX[i] && knightX < tubeX[i] + castleWidth) {
                if (knightY < tubeY[i] + castleHeight ||
                        knightY + knightHeight > tubeY[i] + castleHeight + gap) {
                    triggerGameOver();
                    return;
                }
            }
        }

        if (knightY + knightHeight > canvasHeight || knightY < 0) {
            triggerGameOver();
            return;
        }

        handler.postDelayed(runnable, delay);
    }

    private void triggerGameOver() {
        gameRunning = false;
        if (onGameOverCallback != null) {
            onGameOverCallback.run();
        }
    }

    private void spawnCoin() {
        coinActive = true;

        int rightmost = Math.max(tubeX[0], tubeX[1]);
        coinX = rightmost + (tubeSpacing / 2);

        int minY = 0;
        int maxY = canvasHeight - coinHeight;
        coinY = random.nextInt(maxY - minY + 1) + minY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameRunning) {
            velocity = -40;
        }
        return true;
    }
}
