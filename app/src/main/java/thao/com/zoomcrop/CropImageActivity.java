package thao.com.zoomcrop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.albinmathew.photocrop.cropoverlay.CropOverlayView;
import com.albinmathew.photocrop.cropoverlay.edge.Edge;
import com.albinmathew.photocrop.photoview.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.microedition.khronos.opengles.GL10;

import thao.com.zoomcrop.constant.PhotoConstant;

/**
 * Created by luu.phuong.thao on 11/23/2016.
 */

public class CropImageActivity extends Activity {

    protected static final int REQUEST_DIARY = 0;
    public static final String CAMERA_CLICKED = "camera_clicked";
    public static final String EXTRA_IS_REGISTRATION = "EXTRA_IS_REGISTRATION";
    private PhotoView mImageView;
    private RelativeLayout layout;
    private CropOverlayView mCropOverlayView;
    private TextView mTxtHintPhoto;
    String TAG = "THAO";

    private int mPhotoAction = PhotoConstant.PHOTO_ACTION_NONE;

    private float minScale = 1f;
    private Bitmap loadBitmap;
    private Bitmap realBitmap;
    private int realBitmapWidth;
    private int realBitmapHeight;
    private int deviceScreenWidth;
    private int deviceScreenHeight;
    private float cropSize;
    private int mTranslate;
    private RectF displayedImageRect;
    private float zoomTop;
    private float zoomLeft;
    private float zoomBottom;
    private float zoomRight;
    private float scaleCenterInside;
    private String imagePath;
    private float cropX;
    private float cropY;
    private float cropSquareSize;
    private float rectBoundTop;
    private float rectBoundLeft;
    private float rectBoundRight;
    private float rectBoundBottom;
    private float scaleGL = 1;
    private int texttureGLSize;
    private int mMarginTop;
    private String strIsRegistration = "";
    private boolean isCropped = true;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        strIsRegistration = getIntent().getStringExtra(EXTRA_IS_REGISTRATION);
        mContext = getApplicationContext();
    }

    @Override
    protected void onStart() {
        super.onStart();
/*        if (!getCheckPermission().hasPermission(
                CheckPermission.PERM_READ_EXTERNAL_STORAGE)) {
            getCheckPermission().requestStoragePermission();
        } else {*/
            setImageView();
     //   }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
       /* boolean isGranted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        switch (requestCode) {
            case CheckPermission.REQUEST_PERM_STORAGE:
                if (isGranted) {
                    setImageView();
                } else {
                    // User doesn't allow
                    finish();
                }
                break;
        }*/
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels > displayMetrics.heightPixels?displayMetrics.widthPixels:displayMetrics.heightPixels;
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels > displayMetrics.heightPixels?displayMetrics.heightPixels:displayMetrics.widthPixels;
    }

    public void setImageView() {
        int heightHeader = getScreenHeight(this) / 8;
        imagePath = getIntent().getStringExtra("EXTRA_DATA");
        File file = new File(imagePath);
        if (TextUtils.isEmpty(imagePath) || file == null || !file.exists()) {
            Toast.makeText(mContext, "error_file_invalid", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // the maximum bitmap resolution we can load is texttureGLSize x
        // textureGLSize
        texttureGLSize = GL10.GL_MAX_TEXTURE_SIZE;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        Log.d(TAG,
                "Image width=" + options.outWidth + ", height=" + options.outHeight);

        options.inSampleSize = calculateInSampleSize(options, texttureGLSize,
                texttureGLSize);
        options.inJustDecodeBounds = false;

        realBitmap = BitmapFactory.decodeFile(imagePath, options);
        if (realBitmap == null) {
            Toast.makeText(mContext, "error_file_invalid", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        realBitmap = processBitmapOrientation(realBitmap);

        if (getIntent().getBooleanExtra(CAMERA_CLICKED, false)) {
            realBitmap = cropToSquare(realBitmap);
        }

        realBitmapWidth = realBitmap.getWidth();
        realBitmapHeight = realBitmap.getHeight();
        deviceScreenWidth = getScreenWidth(this);
        deviceScreenHeight = getScreenHeight(this);
        cropSize = deviceScreenWidth;

        Log.d(TAG,
                "Image loaded width=" + realBitmapWidth + ", height=" + realBitmapHeight);
        Log.d(TAG, "cropSize=" + cropSize);

        mPhotoAction = getIntent().getIntExtra(PhotoConstant.EXTRA_PHOTO_ACTION,
                PhotoConstant.PHOTO_ACTION_NONE);
        setContentView(R.layout.zoom_crop_image);
        findViewById(R.id.iv_photo).setBackgroundColor(Color.BLACK);
        if (strIsRegistration != null && strIsRegistration.length() > 0) {
            findViewById(R.id.iv_photo).setBackgroundColor(Color.WHITE);
        }
        setResult(RESULT_CANCELED);

        RelativeLayout headerActivity = (RelativeLayout) findViewById(R.id.header_zoom_activty);
        LinearLayout.LayoutParams headerParam = (LinearLayout.LayoutParams) headerActivity.getLayoutParams();
        headerParam.height = heightHeader;
        headerActivity.setLayoutParams(headerParam);
        headerActivity.requestLayout();

        mCropOverlayView = (CropOverlayView) findViewById(R.id.crop_overlay);

        mMarginTop = getCropViewTopMargin();
        mCropOverlayView.init(this, mMarginTop);
        mCropOverlayView.invalidate();

        mTxtHintPhoto = (TextView) findViewById(R.id.text_hint_photo);
        mTxtHintPhoto.setHeight(mMarginTop);

        layout = (RelativeLayout) findViewById(R.id.pinch_layout);
        mImageView = (PhotoView) findViewById(R.id.iv_photo);

        mImageView.addListener(new PhotoViewAttacher.IGetImageBounds() {
            @Override
            public Rect getImageBounds() {
                return new Rect(
                        (int) Edge.LEFT.getCoordinate(),
                        (int) Edge.TOP.getCoordinate(),
                        (int) Edge.RIGHT.getCoordinate(),
                        (int) Edge.BOTTOM.getCoordinate());
            }
        });
        mTranslate = mMarginTop;
        init(imagePath, mTranslate);
     //   getActionBar().hide();

        //if (!getApp().isHintPhoto()) {
            showOverLay();
        //    getApp().setHintPhoto(true);
        //}
    }

    public float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    public boolean isNavigationBar(Context context) {
        Resources resources = context.getResources();
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            return resources.getBoolean(id);
        } else { // Check for keys
            boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
            boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            return !hasMenuKey && !hasBackKey;
        }
    }

    public static float getScreenDensity(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.density;
    }

    public int getCropViewTopMargin(){
        int mMarginTop = 0;
        int deviceScreenWidth = getScreenWidth(this);
        int deviceScreenHeight = getScreenHeight(this);
        int scaleScreen = (int)getResources().getDisplayMetrics().density;
        int heightHeader = (int) getResources().getDimension(R.dimen.dimen_header_activity);
        if (isNavigationBar(this))
            deviceScreenHeight = deviceScreenHeight + getNavigationBarHeight(this, Configuration.ORIENTATION_PORTRAIT);
        int coordinates_y_img = (deviceScreenHeight - deviceScreenWidth)/2 - heightHeader;
        mMarginTop = (int) (convertPixelsToDp(coordinates_y_img, this) * scaleScreen);
        return mMarginTop;
    }

    public int getStatusBarHeight() {
        int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0?this.getResources().getDimensionPixelSize(resourceId):(int)(25.0F * getScreenDensity(this));
    }

    public int getActionBarHeight() {
        if(this.getActionBar() != null && this.getActionBar().isShowing()) {
            TypedValue tv = new TypedValue();
            return this.getTheme().resolveAttribute(16843499, tv, true)?TypedValue.complexToDimensionPixelSize(tv.data, this.getResources().getDisplayMetrics()):(int)(45.0F * getScreenDensity(this));
        } else {
            return 0;
        }
    }

    public int getNavigationBarHeight(Context context, int orientation) {
        Resources resources = context.getResources();
        int id = resources.getIdentifier(orientation == 1?"navigation_bar_height":"navigation_bar_height_landscape", "dimen", "android");
        return id > 0?resources.getDimensionPixelSize(id):0;
    }

    private Bitmap processBitmapOrientation(Bitmap scaledBitmap) {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(imagePath);
        } catch (IOException e) {
            return scaledBitmap;
        }

        // Process orientation
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;

            default:
                break;
        }

        return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(),
                scaledBitmap.getHeight(), matrix, true);
    }

    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width) ? height - (height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0) ? 0 : cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0) ? 0 : cropH;
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             float reqWidth, float reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round(height / reqHeight);
            final int widthRatio = Math.round(width / reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private void init(String imagePath, final int translate) {
        float widthGLTexttureScale = (float) texttureGLSize / realBitmapWidth;
        float heighGLTexttureScale = (float) texttureGLSize / realBitmapHeight;
        if (isRealBitmapLarger()) {
            // decrease the loaded bitmap size to make it display faster in the
            // screen
            loadBitmap = realBitmap;
            if ((loadBitmap.getHeight() > 0.9 * texttureGLSize)
                    || (loadBitmap.getWidth() > 0.9 * texttureGLSize)) {
                scaleGL = (float) 0.7 * Math.min(1.0f,
                        Math.min(widthGLTexttureScale, heighGLTexttureScale));
                float scaledLoadBitmapWidth = realBitmapWidth * scaleGL;
                float scaledLoadBitmapHeight = realBitmapHeight * scaleGL;
                loadBitmap = Bitmap.createScaledBitmap(loadBitmap,
                        (int) scaledLoadBitmapWidth, (int) scaledLoadBitmapHeight, true);
            }
        } else {
            loadBitmap = resizeBitmap(realBitmap);
        }

        Drawable bitmap = new BitmapDrawable(getResources(), loadBitmap);
        int h = bitmap.getIntrinsicHeight();
        int w = bitmap.getIntrinsicWidth();
        final float cropWindowWidth = Edge.getWidth();
        final float cropWindowHeight = Edge.getHeight();
        @SuppressWarnings("unused")
        int checkHeight = 0;
        if (h <= w) {
            minScale = (cropWindowHeight + 1f) / h;
        } else if (w < h) {
            minScale = (cropWindowWidth + 1f) / w;
            checkHeight = 1;
        }

		/* Translates drawable to fit crop window */
        ViewTreeObserver viewTree = findViewById(
                R.id.wrap_crop_image).getViewTreeObserver();
        viewTree.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {

                findViewById(
                        R.id.wrap_crop_image).getViewTreeObserver().removeOnPreDrawListener(
                        this);
                if ((realBitmapWidth > cropSize) || (realBitmapHeight > cropSize)) {
                    mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }
                mImageView.setImageViewWithHeight((int) cropSize, translate);
                return true;
            }
        });
        mImageView.setMaximumScale(minScale * 7);
        mImageView.setMediumScale(minScale * 5);
        mImageView.setMinimumScale(1f);
        mImageView.setImageDrawable(bitmap);
        mImageView.setScale(1f);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
        if (requestCode != REQUEST_DIARY)
            return;

        setResult(RESULT_OK);
        finish();
    }

    /**
     * saveCroppedImage
     */
    private void saveCroppedImage() {
        /*File folder = getApp()// .getExternalAppFolder();
                .getInternalTemporarilyAppFolder();

        final String photoPath = folder.getAbsolutePath() + "/cropped_image.jpg";
        final Bitmap finalBitmap = getCroppedBitmap();

        //showProgressFullScreen(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                saveCropBitmapAndChangeScreen(photoPath, finalBitmap);
            }
        }).start();*/
    }

    /**
     * !!!! Link to View. Must be a public function.
     *
     * @param view
     */
    public void onSaveZoomCrop(View view) {
/*        if (!getCheckPermission().hasPermission(
                CheckPermission.PERM_WRITE_EXTERNAL_STORAGE)) {
            getCheckPermission().requestCameraPermission();
        }*/
        saveCroppedImage();
    }

    /**
     * getCroppedBitmap
     *
     * @return: bitmap
     */
    private Bitmap getCroppedBitmap() {
        Bitmap croppedBitmap = null;

        boolean isHeightMax = true;
        float zoomRatio = mImageView.getScale();
        if (isCropAll()) {
            zoomRatio = mImageView.getScale();
            if (isRealBitmapLarger())
                croppedBitmap = realBitmap;
            else
                croppedBitmap = loadBitmap;
            cropX = Math.abs(zoomLeft) / zoomRatio / (scaleGL * scaleCenterInside);

            cropY = (rectBoundTop - zoomTop) / zoomRatio / (scaleGL * scaleCenterInside);
            cropSquareSize = cropSize / zoomRatio / (scaleGL * scaleCenterInside);

        } else {

            if (!isRealBitmapLarger()) {
                isCropped = false;
                croppedBitmap = loadBitmap;
            } else {

                croppedBitmap = realBitmap;
            }
            if (realBitmapHeight == realBitmapWidth)
                return croppedBitmap;
            else if ((realBitmapHeight > realBitmapWidth)) {
                cropX = 0;
                isHeightMax = true;
                float width = displayedImageRect.width();
                cropY = (displayedImageRect.height() - width) / 2 / zoomRatio
                        / (scaleGL * scaleCenterInside);
                cropSquareSize = width / zoomRatio / (scaleGL * scaleCenterInside);
            } else {
                cropY = 0;
                isHeightMax = false;
                float height = displayedImageRect.height();
                cropX = (displayedImageRect.width() - height) / 2 / zoomRatio
                        / (scaleGL * scaleCenterInside);
                cropSquareSize = height / zoomRatio / (scaleGL * scaleCenterInside);

            }

        }

        if (strIsRegistration != null && strIsRegistration.length() > 0) {
            mImageView.buildDrawingCache();
            croppedBitmap = mImageView.getDrawingCache();
            try {


                float alpha = (croppedBitmap.getWidth() - cropSquareSize);
                Bitmap bm = Bitmap.createBitmap(croppedBitmap, (int) 0, (int) (mMarginTop),
                        (int) (cropSquareSize + alpha), (int) (cropSquareSize-Math.abs(alpha)));
                int height = bm.getHeight();
                int width = bm.getWidth();
                if (height != width) {
                    int tempSize = height > width ? height : width;
                    croppedBitmap = Bitmap.createBitmap(croppedBitmap, (int) 0, (int) (mMarginTop),
                            (int) (tempSize), (int) (tempSize));
                } else {
                    croppedBitmap = bm;
                }
            } catch (Exception ex) {
                if(com.albinmathew.photocrop.photoview.BuildConfig.DEBUG){
                    Toast.makeText(CropImageActivity.this,ex.getMessage(),Toast.LENGTH_SHORT).show();}
            }


        }

        return croppedBitmap;
    }

    /**
     * !!!! Link to View. Must be a public function.
     *
     * @param view
     */

    public void onBackZoomActivity(View view) {
        finish();
    }
    public  boolean writeToFile(String fileName, Bitmap image) {
        if(TextUtils.isEmpty(fileName)) {
            Log.d(TAG, "writeToFile: fileName is null or empty!");
            return false;
        } else if(image == null) {
            Log.d(TAG, "writeToFile: image is null!");
            return false;
        } else {
            FileOutputStream fileOutputStream = null;

            try {
                fileOutputStream = new FileOutputStream(fileName);
            } catch (FileNotFoundException var4) {
              //  Log.d(TAG, var4);
                return false;
            }

            return image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        }
    }
    private void saveCropBitmapAndChangeScreen(final String photoPath,
                                               final Bitmap bitmap) {

        /*writeToFile(photoPath, bitmap);
        BitmapCacher.getInstance().removeCached(photoPath);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                showProgressFullScreen(false);
                if (mPhotoAction == PhotoConstant.PHOTO_ACTION_CROP) {
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_DATA, photoPath);
                    setResult(RESULT_OK, intent);
                    finish();
                    return;
                }

                long date = getIntent().getLongExtra(EXTRA_DATE, -1);

                Calendar calendar = Calendar.getInstance();
                if (date != -1)
                    calendar.setTimeInMillis(date);

                DiaryPhoto diaryPhoto = new DiaryPhoto("", calendar);
                if (DateUtil.isToday(calendar)) {
                    DiaryPhoto diaryPhotoToday = getApp().getDiaryPhoto(
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH));
                    if (diaryPhotoToday != null) {
                        diaryPhoto.setServerId(diaryPhotoToday.getServerId());
                    }
                }

                diaryPhoto.setImagePath(photoPath);
                diaryPhoto.setThumbnailPath(photoPath);

                getApp().setSelectedDiaryPhoto(diaryPhoto);

                Intent intent = new Intent(ZoomAndCropActivity.this,
                        DetailDiaryPhotoActivity.class);
                intent.putExtra(BaseActivity.EXTRA_EDITTING, false);

                intent.putExtra(PhotoConstant.EXTRA_PHOTO_ACTION, mPhotoAction);
                startActivityForResult(intent, REQUEST_DIARY);

                // if (getIntent().getBooleanExtra(CAMERA_CLICKED, false)) {
                // setResult(RESULT_OK);
                // finish();
                // }
            }
        });*/
    }

    private void showOverLay() {
        layout.setVisibility(View.VISIBLE);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                layout.setVisibility(View.GONE);
            }
        });
    }

    // increase bitmap size when its size is smaller than the desired square
    // image
    private Bitmap resizeBitmap(Bitmap bitmap) {
        if (realBitmapHeight == cropSize && realBitmapWidth == cropSize)
            return realBitmap;

        float scaleWidth = cropSize / realBitmapWidth;
        float scaleHeight = cropSize / realBitmapHeight;
        float scale = Math.min(scaleWidth, scaleHeight);
        float newWidth = scale * realBitmapWidth;
        float newHeight = scale * realBitmapHeight;
        return Bitmap.createScaledBitmap(realBitmap, (int) newWidth, (int) newHeight,
                true);
    }

    // check whether the bitmap is cut all edges
    public boolean isCropAll() {
        displayedImageRect = mImageView.getDisplayRect();
        zoomTop = displayedImageRect.top;
        zoomBottom = displayedImageRect.bottom;
        zoomLeft = displayedImageRect.left;
        zoomRight = displayedImageRect.right;
        rectBoundLeft = Edge.LEFT.getCoordinate();
        rectBoundTop = Edge.TOP.getCoordinate();
        rectBoundRight = Edge.TOP.getCoordinate();
        rectBoundBottom = Edge.BOTTOM.getCoordinate();
        scaleCenterInside = mImageView.getScaleCenter();

        return zoomTop <= rectBoundTop && zoomBottom >= rectBoundBottom
                && zoomLeft <= rectBoundLeft && zoomRight >= rectBoundRight;
    }

    public boolean isRealBitmapLarger() {
        return realBitmapWidth > deviceScreenWidth
                || realBitmapHeight > deviceScreenHeight;
    }
}