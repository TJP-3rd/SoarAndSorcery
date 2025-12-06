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
    private int gravity = 5;
    private int velocity = 0;
    private int castleVelocity = 15;

    private int[] tubeX = new int[2];
    private int[] tubeY = new int[2];
    private boolean[] passed = new boolean[2];

    // Coin variables
    private int coinX, coinY;
    private int coinWidth, coinHeight;
    private boolean coinActive = false;
    private int tubesPassedCount = 0;

    private int score = 0;
    private Paint scorePaint;

    private Handler handler = new Handler();
    private Runnable runnable;
    private int delay = 30;

    private Paint paint = new Paint();
    private Random random = new Random();

    // Tube spacing constant
    private int tubeSpacing;

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

        runnable = this::invalidate;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasWidth = w;
        canvasHeight = h;

        // Scale knight
        int birdTargetWidth = canvasWidth / 10;
        int birdTargetHeight = birdTargetWidth * knight.getHeight() / knight.getWidth();
        knight = Bitmap.createScaledBitmap(knight, birdTargetWidth, birdTargetHeight, false);
        knightWidth = knight.getWidth();
        knightHeight = knight.getHeight();

        // Scale tubes
        int tubeTargetWidth = canvasWidth / 6;
        int tubeTargetHeight = tubeTargetWidth * topTube.getHeight() / topTube.getWidth();
        topTube = Bitmap.createScaledBitmap(topTube, tubeTargetWidth, tubeTargetHeight, false);
        bottomTube = Bitmap.createScaledBitmap(bottomTube, tubeTargetWidth, tubeTargetHeight, false);
        castleWidth = topTube.getWidth();
        castleHeight = topTube.getHeight();

        // Scale coin
        int coinTargetWidth = canvasWidth / 15;
        int coinTargetHeight = coinTargetWidth * coin.getHeight() / coin.getWidth();
        coin = Bitmap.createScaledBitmap(coin, coinTargetWidth, coinTargetHeight, false);
        coinWidth = coin.getWidth();
        coinHeight = coin.getHeight();

        gap = canvasHeight / 3;
        tubeSpacing = canvasWidth / 2 + castleWidth;

        resetPositions();
    }

    private void resetPositions() {
        knightX = canvasWidth / 4;
        knightY = canvasHeight / 2;
        velocity = 0;
        score = 0;
        tubesPassedCount = 0;
        coinActive = false;

        // Place tubes evenly and predictably
        for (int i = 0; i < 2; i++) {
            tubeX[i] = canvasWidth + i * tubeSpacing;
            tubeY[i] = getRandomTubeY();
            passed[i] = false;
        }
    }

    private int getRandomTubeY() {
        // Limit vertical offset for consistent spacing
        int maxOffset = canvasHeight / 4;
        return -random.nextInt(maxOffset);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw and move tubes
        for (int i = 0; i < 2; i++) {
            canvas.drawBitmap(topTube, tubeX[i], tubeY[i], paint);
            canvas.drawBitmap(bottomTube, tubeX[i], tubeY[i] + castleHeight + gap, paint);

            tubeX[i] -= castleVelocity;

            // Reset tube when off-screen, keeping even spacing
            if (tubeX[i] + castleWidth < 0) {
                int otherIndex = (i == 0) ? 1 : 0;
                tubeX[i] = tubeX[otherIndex] + tubeSpacing;
                tubeY[i] = getRandomTubeY();
                passed[i] = false;
            }

            // Count score and trigger coins
            if (tubeX[i] + castleWidth < knightX && !passed[i]) {
                passed[i] = true;
                score++;
                tubesPassedCount++;

                if (tubesPassedCount % 5 == 0 && !coinActive) {
                    spawnCoinBetweenTubes();
                }
            }
        }

        // Handle coin logic
        if (coinActive) {
            coinX -= castleVelocity;
            canvas.drawBitmap(coin, coinX, coinY, paint);

            // Collect coin
            if (knightX < coinX + coinWidth && knightX + knightWidth > coinX &&
                    knightY < coinY + coinHeight && knightY + knightHeight > coinY) {
                score += 5;
                coinActive = false;
            }

            // Remove if off-screen
            if (coinX + coinWidth < 0) {
                coinActive = false;
            }
        }

        // Knight motion
        velocity += gravity;
        knightY += velocity;
        canvas.drawBitmap(knight, knightX, knightY, paint);

        // Score text
        canvas.drawText("Score: " + score, 50, 150, scorePaint);

        // Collision detection
        for (int i = 0; i < 2; i++) {
            if (knightX + knightWidth > tubeX[i] && knightX < tubeX[i] + castleWidth) {
                if (knightY < tubeY[i] + castleHeight ||
                        knightY + knightHeight > tubeY[i] + castleHeight + gap) {
                    resetPositions();
                }
            }
        }

        // Ceiling/floor collisions
        if (knightY + knightHeight > canvasHeight || knightY < 0) {
            resetPositions();
        }

        handler.postDelayed(runnable, delay);
    }

    /**
     * Spawns the coin in between two tube sets, centered horizontally between them,
     * and at any vertical height on screen.
     */
    private void spawnCoinBetweenTubes() {
        coinActive = true;

        // Find the rightmost tube to place the coin between tube sets
        int rightmostX = Math.max(tubeX[0], tubeX[1]);
        coinX = rightmostX + (tubeSpacing / 2);

        // Vertical spawn range: anywhere on screen (not offscreen)
        int minY = 0;
        int maxY = canvasHeight - coinHeight;
        coinY = random.nextInt(maxY - minY + 1) + minY;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            velocity = -40;
        }
        return true;
    }
}
