package com.k09.lifegame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnTouchListener {
    private static final String LOG_TAG = "Lifegame";
    private static final int RECT_SIZE = 10;    /* px */
    private static final int ALIVE_COLOR = Color.RED;
    private static final int DEAD_COLOR = Color.GREEN;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private boolean mIsLifegameProcessing = false;
    private static final int INTERVAL_MILLISEC = 50;

    /* lifeCell info */
    private boolean[][] mIsLife;
    private int[][] mSums;
    private int mCellHeight;
    private int mCellWidth;
    private int mGeneration;
    private int mAliveNum;
    private int mDeadNum;

    private class UpdateRunnable implements Runnable {
        @Override
        public void run() {
            calcSums();
            drawLife();
            updateLife();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setOnTouchListener(this);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mHandlerThread = new HandlerThread("drawThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    protected void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }

    private void initLife() {
        if (mSurfaceHolder == null) {
            Log.e(LOG_TAG, "initLife: SurfaceHolder is null");
            return;
        }

        mGeneration = 0;

        Canvas canvas = mSurfaceHolder.lockCanvas();

        if (canvas == null) {
            Log.e(LOG_TAG, "initLife: Canvas is null");
            return;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < mCellHeight; i++) {
            for (int j = 0; j < mCellWidth; j++) {
                int rand = (int) (Math.random() * 2);
                int left = j * RECT_SIZE;
                int top = i * RECT_SIZE;
                int right = (j + 1) * RECT_SIZE;
                int bottom = (i + 1) * RECT_SIZE;

//                Log.d(LOG_TAG, "i:" + i + " j:" + j + " rand:" + rand +
//                        " left:" + left + " top:" + top + " right:" + right + " bottom:" + bottom);
                if (rand == 0) {
                    paint.setColor(DEAD_COLOR);
                    mIsLife[i][j] = false;
                } else {
                    mIsLife[i][j] = true;
                    paint.setColor(ALIVE_COLOR);
                }

                canvas.drawRect(left, top, right, bottom, paint);
            }
        }
        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

    private void calcSums() {
        for (int i = 0; i < mCellHeight; i++) {
            for (int j = 0; j < mCellWidth; j++) {
                int up = ((i - 1) < 0) ? (mCellHeight - 1) : (i - 1);
                int left = ((j - 1) < 0) ? (mCellWidth - 1) : (j - 1);
                int down = ((i + 1) >= mCellHeight) ? 0 : (i + 1);
                int right = ((j + 1) >= mCellWidth) ? 0 : (j + 1);

                mSums[i][j] = (mIsLife[up][left] ? 1 : 0)
                        + (mIsLife[up][j] ? 1 : 0)
                        + (mIsLife[up][right] ? 1 : 0)
                        + (mIsLife[i][left] ? 1 : 0)
                        + (mIsLife[i][right] ? 1 : 0)
                        + (mIsLife[down][left] ? 1 : 0)
                        + (mIsLife[down][j] ? 1 : 0)
                        + (mIsLife[down][right] ? 1 : 0);

            }
        }
    }

    private void drawLife() {
        if (mSurfaceHolder == null) {
            Log.e(LOG_TAG, "drawLife: SurfaceHolder is null");
            return;
        }

        Canvas canvas = mSurfaceHolder.lockCanvas();

        if (canvas == null) {
            Log.e(LOG_TAG, "drawLife: Canvas is null");
            return;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        mGeneration++;
        mAliveNum = 0;
        mDeadNum = 0;

        for (int i = 0; i < mCellHeight; i++) {
            for (int j = 0; j < mCellWidth; j++) {
                boolean isLife = mIsLife[i][j];
                int sum = mSums[i][j];
                int left = j * RECT_SIZE;
                int top = i * RECT_SIZE;
                int right = (j + 1) * RECT_SIZE;
                int bottom = (i + 1) * RECT_SIZE;

//                Log.d(LOG_TAG, "i:" + i + " j:" + j + " isLife:" + isLife + " sum:" + sum);
                if (isLife) {
                    if ((sum == 2) || (sum == 3)) {
                        isLife = true;
                    } else {
                        isLife = false;
                    }
                } else {
                    if ((sum == 3) || (sum == 6)) {
                        isLife = true;
                    } else {
                        isLife = false;
                    }
                }

                mIsLife[i][j] = isLife;
                if (!isLife) {
                    mDeadNum++;
                    paint.setColor(DEAD_COLOR);
                } else {
                    mAliveNum++;
                    paint.setColor(ALIVE_COLOR);
                }

                canvas.drawRect(left, top, right, bottom, paint);
            }
        }
        mSurfaceHolder.unlockCanvasAndPost(canvas);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView generationTextView = (TextView)(findViewById(R.id.generationTextView));
                TextView aliveTextView = (TextView)(findViewById(R.id.aliveTextView));
                TextView deadTextView = (TextView)(findViewById(R.id.deadTextView));

                generationTextView.setText(getString(R.string.generation, mGeneration));
                aliveTextView.setText(getString(R.string.alive, mAliveNum));
                deadTextView.setText(getString(R.string.dead, mDeadNum));
            }
        });
    }

    private void updateLife() {
        if (mIsLifegameProcessing) {
            mHandler.postDelayed(new UpdateRunnable(), INTERVAL_MILLISEC);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(LOG_TAG, "surfaceChanged");
        mCellWidth = width / RECT_SIZE;
        mCellHeight = height / RECT_SIZE;
        mIsLife = new boolean[mCellHeight][mCellWidth];
        mSums = new int[mCellHeight][mCellWidth];
        initLife();
        mIsLifegameProcessing = true;
        updateLife();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsLifegameProcessing = false;
    }

    private void onTouchDraw(int x, int y) {
        final int drawX = x / RECT_SIZE;
        final int drawY = y / RECT_SIZE;

        if ((drawX >= mCellWidth) || (drawX < 0)
                || (drawY >= mCellHeight) || (drawY < 0)) {
            Log.e(LOG_TAG, "onTouchDraw:invalid touch point. x:" + x + " drawX: " + drawX + " width:" + mCellWidth + "\n"
                    + "y:" + y + " drawY:" + drawY + " height:" + mCellHeight);
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Canvas canvas = mSurfaceHolder.lockCanvas();
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(ALIVE_COLOR);

                int left = drawX * RECT_SIZE;
                int top = drawY * RECT_SIZE;
                int right = (drawX + 1) * RECT_SIZE;
                int bottom = (drawY + 1) * RECT_SIZE;
                canvas.drawRect(left, top, right, bottom, paint);
                mIsLife[drawY][drawX] = true;
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int x = Math.round(event.getX());
        int y = Math.round(event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsLifegameProcessing = false;
                onTouchDraw(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchDraw(x, y);
                break;
            case MotionEvent.ACTION_UP:
                mIsLifegameProcessing = true;
                updateLife();
            default:
                /* nothing to do */
                break;
        }
        return true;
    }
}
