package thao.com.zoomcrop;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import thao.com.zoomcrop.constant.PhotoConstant;

public class MainActivity extends AppCompatActivity {
    int REQUEST_CROP_PHOTO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onBtnClick(View view){
        Intent intent = new Intent(getApplicationContext(), CropImageActivity.class);
        String mFilePath = "/storage/emulated/0/avril-lavigne-press-650b.jpg";
        intent.putExtra("EXTRA_DATA", mFilePath);
        intent.putExtra("EXTRA_IS_REGISTRATION", "EXTRA_IS_REGISTRATION");
        intent.putExtra(PhotoConstant.EXTRA_PHOTO_ACTION,
                PhotoConstant.PHOTO_ACTION_CROP);
        startActivityForResult(intent, REQUEST_CROP_PHOTO);
    }
}
