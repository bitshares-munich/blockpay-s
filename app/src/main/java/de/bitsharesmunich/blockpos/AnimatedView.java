package de.bitsharesmunich.blockpos;

/**
 * Created by qasim on 5/13/16.
 */

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import de.bitsharesmunich.utils.Helper;

public class AnimatedView extends ImageView {

    private Context mContext;

    int x = -1;

    int y = -1;

    private int xVelocity = 10;

    private int yVelocity = 5;

    private Handler h;

    private final int FRAME_RATE = 30;

    boolean checkScreenSaver;


    public AnimatedView(Context context, AttributeSet attrs) {

        super(context, attrs);

        mContext = context;

        int screenSaver = Helper.fetchIntSharePref(mContext, mContext.getString(R.string.Pref_screen_saver), 2);
        if (screenSaver == 2) {
            checkScreenSaver = true;
        } else {
            checkScreenSaver = false;
        }

        h = new Handler();

    }

    private Runnable r = new Runnable() {

        @Override

        public void run() {

            invalidate();

        }

    };

    protected void onDraw(Canvas c) {

        if (checkScreenSaver) {

            Bitmap ball = loadImageFromStorage(mContext);

            if(ball!=null) {
                if (x < 0 && y < 0) {

                    x = this.getWidth() / 2;

                    y = this.getHeight() / 2;

                } else {

                    x += xVelocity;

                    y += yVelocity;

                    if ((x > this.getWidth() - ball.getWidth()) || (x < 0)) {

                        xVelocity = xVelocity * -1;

                    }

                    if ((y > this.getHeight() - ball.getHeight()) || (y < 0)) {

                        yVelocity = yVelocity * -1;

                    }

                }

                c.drawBitmap(ball, x, y, null);
                h.postDelayed(r, FRAME_RATE);
            }
        } else {
            BitmapDrawable ball = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.blank1);


            if (x < 0 && y < 0) {

                x = this.getWidth() / 2;

                y = this.getHeight() / 2;

            } else {

                x += xVelocity;

                y += yVelocity;

                if ((x > this.getWidth() - ball.getBitmap().getWidth()) || (x < 0)) {

                    xVelocity = xVelocity * -1;

                }
                if ((x > this.getWidth() - ball.getBitmap().getWidth()) || (x < 0)) {

                    yVelocity = yVelocity * -1;

                }

            }

            c.drawBitmap(ball.getBitmap(), x, y, null);

            h.postDelayed(r, FRAME_RATE);
        }

    }

    public static Bitmap loadImageFromStorage(Context context) {
        Bitmap bitmap = null;
        try {
            ContextWrapper cw = new ContextWrapper(context);
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            File f = new File(directory, "gravatar.jpg");
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bitmap;

    }

}
