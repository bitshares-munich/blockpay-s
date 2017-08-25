package de.bitsharesmunich.blockpos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import java.io.File;
import java.io.FileFilter;

import de.bitsharesmunich.utils.BlockpayApplication;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.PermissionManager;

public class ScreenSaverActivity extends BaseActivity {
    private final String TAG = this.getClass().getName();

    /**
     * Amount of time in milliseconds each image should persist
     */
    private final int IMAGE_PERIOD = 5000;

    /**
     * Default time in milliseconds to wait before triggering the screen saver.
     */
    public static final int DEFAULT_SCREENSAVER_TRIGGER_TIME = 60000;

    /**
     * Default value for the screen saver type
     */
    public static final int DEFAULT_SCREENSAVER_TYPE = 1;

    int numberOfImage = 0;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Makes the activity take the full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_screen_saver);

        // Notifies the BlockpayApplication instance that the screensaver is on
        ((BlockpayApplication) this.getApplication()).toggleScreensaver(true);

        int screenSaver = Helper.fetchIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver), DEFAULT_SCREENSAVER_TYPE);

        if (screenSaver != 1 && screenSaver != 2) {
            AnimatedView bounceView = (AnimatedView) findViewById(R.id.anim_view);
            bounceView.setVisibility(View.GONE);
            bounceView.clearAnimation();
        }

        //final String path = Environment.getExternalStorageDirectory().toString() + "/Pictures/JAC Pictures";
        if (screenSaver == 3) {
            PermissionManager Manager = new PermissionManager();

            if (!Manager.isStoragePermissionGranted(this)) {
                exitScreenSaver();
                return;
            }

            String folderPath = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_screen_saver_folder_path));
            startImageSlider(folderPath);
        }
    }

    private void changeImage(final File file[], final String path) {
        try {
            if (file != null) {
                if (file.length > 0) {
                    if (numberOfImage >= file.length) {
                        numberOfImage = 0;
                    }
                    int row = numberOfImage;
                    numberOfImage++;
                    ImageFileFilter imageFileFilter = new ImageFileFilter();
                    if (imageFileFilter.accept(file[row])) {
                        final String imagepath = file[row].getName();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                changeImage(file, path);
                            }
                        }, IMAGE_PERIOD);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                String appPath = Environment.getExternalStorageDirectory() + File.separator + getResources().getString(R.string.folder_name);
                                pasteImageOnScreenSaver(path + File.separator + imagepath);
                            }
                        });
                    } else {
                        changeImage(file, path);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception. Msg: "+e.getMessage());
        }
    }

    private void startImageSlider(String folderPath) {
        File f = new File(folderPath);
        final File file[] = f.listFiles();
        changeImage(file, folderPath);

        ImageView myImage = (ImageView) findViewById(R.id.imageView1);
        myImage.setVisibility(View.VISIBLE);
    }



    private void exitScreenSaver() {
        ((BlockpayApplication) this.getApplication()).toggleScreensaver(false);

        handler.removeCallbacksAndMessages(null);
        ImageView myImage = (ImageView) findViewById(R.id.imageView1);
        myImage.clearAnimation();
        myImage = null;

        AnimatedView bounceView = (AnimatedView) findViewById(R.id.anim_view);
        bounceView.clearAnimation();
        bounceView = null;

        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        exitScreenSaver();
        return super.onTouchEvent(event);
    }

    private void pasteImageOnScreenSaver(final String imageUri) {
        try {
            ImageView myImage = (ImageView) findViewById(R.id.imageView1);

            Animation animationFadeOut = AnimationUtils.loadAnimation(this, R.anim.fadeout);
            myImage.startAnimation(animationFadeOut);
            animationFadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    ImageView myImage = (ImageView) findViewById(R.id.imageView1);
                    File imgFile = new File(imageUri);
                    myImage.setScaleType(ImageView.ScaleType.FIT_XY);
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    myImage.setImageBitmap(myBitmap);


                    Animation animationFadeIn = AnimationUtils.loadAnimation(ScreenSaverActivity.this, R.anim.fadein);
                    myImage.startAnimation(animationFadeIn);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        } catch (Exception e) {
            Log.e(TAG,"Exception. Msg: "+e.getMessage());
        }
    }

    public class ImageFileFilter implements FileFilter {
        private final String[] okFileExtensions =
                new String[]{"jpg", "png", "gif"};

        public boolean accept(File file) {
            for (String extension : okFileExtensions) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG,"onLowMemory");
    }
}
