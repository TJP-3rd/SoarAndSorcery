package com.example.soarandsorcery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.Random;

public class GameView extends View {

    private Bitmap knight, topTube, bottomTube, coin;
    private int knightX, knightY, knightWidth, knightHeight;
    private int castleWidth, castleHeight;
    private int canvasWidth, canvasHeight;
    private int gap = 400;
    private int gravity = 3;
    private int velocity = 0;
    private int castleVelocity = 8;
    private int delay = 40;

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

    private boolean gameRunning = false;
    private boolean countdownRunning = false;
    private int countdownValue = 3;
    private Paint countdownPaint = new Paint();
    private Runnable onGameOverCallback;

    private boolean viewReady = false;

    public GameView(Context context) {
        super(context);

        try {
            knight = BitmapFactory.decodeResource(getResources(), R.drawable.flappybirdup);
            topTube = BitmapFactory.decodeResource(getResources(), R.drawable.toptube);
            bottomTube = BitmapFactory.decodeResource(getResources(), R.drawable.bottomtube);
            coin = BitmapFactory.decodeResource(getResources(), R.drawable.coin);
        } catch (Exception e) {
            Log.e("GameView", "Error loading bitmaps", e);
        }

        scorePaint = new Paint();
        scorePaint.setColor(Color.BLACK);
        scorePaint.setTextSize(50);
        scorePaint.setFakeBoldText(true);

        countdownPaint.setColor(Color.WHITE);
        countdownPaint.setTextSize(200);
        countdownPaint.setTextAlign(Paint.Align.CENTER);

        runnable = this::invalidate;

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!viewReady && getWidth() > 0 && getHeight() > 0) {
                    canvasWidth = getWidth();
                    canvasHeight = getHeight();
                    viewReady = true;

                    setupBitmaps();

                    knightX = canvasWidth / 4;
                    knightY = canvasHeight / 2;
                    resetGame();

                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    private void setupBitmaps() {
        try {
            if (knight != null) {
                int birdTargetWidth = canvasWidth / 10;
                int birdTargetHeight = birdTargetWidth * knight.getHeight() / knight.getWidth();
                knight = Bitmap.createScaledBitmap(knight, birdTargetWidth, birdTargetHeight, false);
                knightWidth = knight.getWidth();
                knightHeight = knight.getHeight();
            }

            if (topTube != null && bottomTube != null) {
                int tubeTargetWidth = canvasWidth / 6;
                int tubeTargetHeight = tubeTargetWidth * topTube.getHeight() / topTube.getWidth();
                topTube = Bitmap.createScaledBitmap(topTube, tubeTargetWidth, tubeTargetHeight, false);
                bottomTube = Bitmap.createScaledBitmap(bottomTube, tubeTargetWidth, tubeTargetHeight, false);
                castleWidth = topTube.getWidth();
                castleHeight = topTube.getHeight();
            }

            if (coin != null) {
                int coinTargetWidth = canvasWidth / 15;
                int coinTargetHeight = coinTargetWidth * coin.getHeight() / coin.getWidth();
                coin = Bitmap.createScaledBitmap(coin, coinTargetWidth, coinTargetHeight, false);
                coinWidth = coin.getWidth();
                coinHeight = coin.getHeight();
            }

            gap = canvasHeight / 3;
            tubeSpacing = canvasWidth / 2 + castleWidth;
        } catch (Exception e) {
            Log.e("GameView", "Error scaling bitmaps", e);
        }
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

        if (!viewReady) {
            post(() -> startCountdown(onGameOver));
            return;
        }

        countdownValue = 3;
        countdownRunning = true;
        gameRunning = false;

        handler.removeCallbacks(countdownTick);
        handler.postDelayed(countdownTick, 1000);

        invalidate();
    }

    private Runnable countdownTick = new Runnable() {
        @Override
        public void run() {
            if (countdownValue > 0) {
                countdownValue--;
                invalidate();

                if (countdownValue > 0) {
                    handler.postDelayed(this, 1000);
                } else {
                    countdownRunning = false;
                    gameRunning = true;
                }
            }
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        if (!viewReady || knight == null || topTube == null || bottomTube == null || coin == null) {
            handler.postDelayed(runnable, delay);
            return;
        }

        // Countdown state
        if (countdownRunning) {
            drawTubes(canvas, false); // DRAW WITHOUT MOVING
            canvas.drawBitmap(knight, knightX, knightY, paint);

            canvas.drawText(
                    countdownValue == 0 ? "GO!" : String.valueOf(countdownValue),
                    canvasWidth / 2f,
                    canvasHeight / 2f,
                    countdownPaint
            );

            handler.postDelayed(runnable, delay);
            return;
        }

        // Paused/menu
        if (!gameRunning) {
            canvas.drawBitmap(knight, knightX, knightY, paint);
            handler.postDelayed(runnable, delay);
            return;
        }

        // Game running
        drawTubes(canvas, true); // MOVE TUBES ONLY WHEN GAME IS RUNNING
        drawCoins(canvas);

        velocity += gravity;
        knightY += velocity;
        canvas.drawBitmap(knight, knightX, knightY, paint);

        canvas.drawText("Score: " + score, 50, 150, scorePaint);

        // Collision detection
        for (int i = 0; i < 2; i++) {
            if (knightX + knightWidth > tubeX[i] && knightX < tubeX[i] + castleWidth) {
                if (knightY < tubeY[i] + castleHeight || knightY + knightHeight > tubeY[i] + castleHeight + gap) {
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

    private void drawTubes(Canvas canvas, boolean moveTubes) {
        for (int i = 0; i < 2; i++) {
            canvas.drawBitmap(topTube, tubeX[i], tubeY[i], paint);
            canvas.drawBitmap(bottomTube, tubeX[i], tubeY[i] + castleHeight + gap, paint);

            if (moveTubes) {
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
        }
    }

    private void drawCoins(Canvas canvas) {
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
    }

    private void spawnCoin() {
        coinActive = true;

        int rightmost = Math.max(tubeX[0], tubeX[1]);
        coinX = rightmost + (tubeSpacing / 2);

        int minY = 0;
        int maxY = canvasHeight - coinHeight;
        coinY = random.nextInt(maxY - minY + 1) + minY;
    }

    private void triggerGameOver() {
        gameRunning = false;
        if (onGameOverCallback != null) {
            onGameOverCallback.run();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameRunning) {
            velocity = -40;
        }
        return true;
    }
}
