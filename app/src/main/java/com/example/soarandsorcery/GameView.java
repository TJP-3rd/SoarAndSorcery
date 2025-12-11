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

    public interface OnGameOverListener {
        void onGameOver(int score);
    }

    private Bitmap knight, topTube, bottomTube, coin;
    private int knightX, knightY, knightWidth, knightHeight;
    private int castleWidth, castleHeight;
    private int canvasWidth, canvasHeight;

    private int gap;
    private int minGap;
    private int gapShrinkStep;
    private int shrinkEvery = 12;
    private int setsPassed = 0;

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

    private final Handler handler = new Handler();
    private final Paint paint = new Paint();
    private final Random random = new Random();

    private int tubeSpacing;

    private boolean gameRunning = false;
    private boolean countdownRunning = false;
    private int countdownValue = 3;
    private Paint countdownPaint = new Paint();

    private OnGameOverListener onGameOverCallback;
    private boolean viewReady = false;

    private Runnable redrawRunnable;

    public GameView(Context context) {
        super(context);

        try {
            knight = BitmapFactory.decodeResource(getResources(), R.drawable.trueknight);
            topTube = BitmapFactory.decodeResource(getResources(), R.drawable.topcastle);
            bottomTube = BitmapFactory.decodeResource(getResources(), R.drawable.bottomcastle);
            coin = BitmapFactory.decodeResource(getResources(), R.drawable.coin);
        } catch (Exception e) {
            Log.e("GameView", "Error loading bitmaps", e);
        }

        scorePaint = new Paint();
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(80);
        scorePaint.setFakeBoldText(true);

        countdownPaint.setColor(Color.WHITE);
        countdownPaint.setTextSize(200);
        countdownPaint.setTextAlign(Paint.Align.CENTER);

        redrawRunnable = this::invalidate;

        getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (!viewReady && getWidth() > 0 && getHeight() > 0) {
                            canvasWidth = getWidth();
                            canvasHeight = getHeight();
                            viewReady = true;

                            setupBitmaps();
                            resetGame();

                            getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                }
        );
    }

    // ✅ Castles now scale to full screen height
    private void setupBitmaps() {
        if (knight != null) {
            int w = canvasWidth / 10;
            int h = w * knight.getHeight() / knight.getWidth();
            knight = Bitmap.createScaledBitmap(knight, w, h, false);
            knightWidth = knight.getWidth();
            knightHeight = knight.getHeight();
        }

        if (topTube != null && bottomTube != null) {
            int tubeWidth = canvasWidth / 6;
            int tubeHeight = canvasHeight;   // full screen height ✅

            topTube = Bitmap.createScaledBitmap(topTube, tubeWidth, tubeHeight, false);
            bottomTube = Bitmap.createScaledBitmap(bottomTube, tubeWidth, tubeHeight, false);

            castleWidth = topTube.getWidth();
            castleHeight = topTube.getHeight();
        }

        if (coin != null) {
            int w = canvasWidth / 15;
            int h = w * coin.getHeight() / coin.getWidth();
            coin = Bitmap.createScaledBitmap(coin, w, h, false);
            coinWidth = coin.getWidth();
            coinHeight = coin.getHeight();
        }

        gap = canvasHeight / 3;
        minGap = canvasHeight / 6;
        gapShrinkStep = canvasHeight / 40;

        tubeSpacing = canvasWidth / 2 + castleWidth;
    }

    public void resetGame() {
        knightX = canvasWidth / 4;
        knightY = canvasHeight / 2;
        velocity = 0;
        score = 0;
        tubesPassedCount = 0;
        setsPassed = 0;
        coinActive = false;

        gap = canvasHeight / 3;

        for (int i = 0; i < 2; i++) {
            tubeX[i] = canvasWidth + i * tubeSpacing;
            tubeY[i] = getRandomTubeY();
            passed[i] = false;
        }

        gameRunning = false;
        countdownRunning = false;
    }

    // ✅ Gap-center randomization
    private int getRandomTubeY() {
        int edgePadding = canvasHeight / 12;
        int minGapCenter = edgePadding + (gap / 2);
        int maxGapCenter = canvasHeight - edgePadding - (gap / 2);

        int gapCenter = random.nextInt(maxGapCenter - minGapCenter) + minGapCenter;

        return gapCenter - (gap / 2) - castleHeight;
    }

    private final Runnable countdownTick = new Runnable() {
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

    public void startCountdown(OnGameOverListener listener) {
        this.onGameOverCallback = listener;

        if (!viewReady) {
            post(() -> startCountdown(listener));
            return;
        }

        countdownValue = 3;
        countdownRunning = true;
        gameRunning = false;

        handler.removeCallbacks(countdownTick);
        handler.postDelayed(countdownTick, 1000);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!viewReady || knight == null) {
            handler.postDelayed(redrawRunnable, delay);
            return;
        }

        if (countdownRunning) {
            drawTubes(canvas, false);
            canvas.drawBitmap(knight, knightX, knightY, paint);
            canvas.drawText(
                    countdownValue == 0 ? "GO!" : String.valueOf(countdownValue),
                    canvasWidth / 2f,
                    canvasHeight / 2f,
                    countdownPaint
            );
            handler.postDelayed(redrawRunnable, delay);
            return;
        }

        if (!gameRunning) {
            canvas.drawBitmap(knight, knightX, knightY, paint);
            handler.postDelayed(redrawRunnable, delay);
            return;
        }

        drawTubes(canvas, true);
        drawCoins(canvas);

        velocity += gravity;
        knightY += velocity;
        canvas.drawBitmap(knight, knightX, knightY, paint);

        canvas.drawText("Score: " + score, 50, 150, scorePaint);

        checkCollision();
        handler.postDelayed(redrawRunnable, delay);
    }

    private void drawTubes(Canvas canvas, boolean move) {
        for (int i = 0; i < 2; i++) {
            canvas.drawBitmap(topTube, tubeX[i], tubeY[i], paint);
            canvas.drawBitmap(bottomTube, tubeX[i], tubeY[i] + castleHeight + gap, paint);

            if (move) {
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
                    setsPassed++;

                    if (setsPassed % shrinkEvery == 0) {
                        gap -= gapShrinkStep;
                        if (gap < minGap) gap = minGap;
                    }

                    if (tubesPassedCount % 5 == 0 && !coinActive) {
                        spawnCoin();
                    }
                }
            }
        }
    }

    private void checkCollision() {
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

        int maxY = canvasHeight - coinHeight;
        coinY = random.nextInt(maxY + 1);
    }

    private void triggerGameOver() {
        gameRunning = false;
        if (onGameOverCallback != null) {
            onGameOverCallback.onGameOver(score);
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
