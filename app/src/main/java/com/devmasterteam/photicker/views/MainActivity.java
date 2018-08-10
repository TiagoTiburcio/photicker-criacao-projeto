package com.devmasterteam.photicker.views;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.devmasterteam.photicker.R;
import com.devmasterteam.photicker.utils.ImageUtil;
import com.devmasterteam.photicker.utils.LongEventType;
import com.devmasterteam.photicker.utils.PermissionUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {

    private static final int REQUEST_TAKE_PHOTO = 2;
    private final ViewHolder mViewHolder = new ViewHolder();
    private ImageView mImageSelected;
    private boolean mAutoIncrement;
    private LongEventType mLongEventType;
    private Handler mRptUpdaterHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        List<Integer> mListImages = ImageUtil.getImageList();

        this.mViewHolder.mRelativePhotoContentDraw = (RelativeLayout) findViewById(R.id.relative_photo_content_draw);
        final LinearLayout content = (LinearLayout) findViewById(R.id.linear_horizontal_scroll_content);

        for (Integer imageId : mListImages) {

            ImageView image = new ImageView(this);

            image.setImageBitmap(ImageUtil.decodeSampledBitmapFromResource(getResources(), imageId, 70, 70));

            BitmapFactory.Options dimensions = new BitmapFactory.Options();
            dimensions.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), imageId, dimensions);

            final int width = dimensions.outWidth;
            final int height = dimensions.outHeight;

            image.setOnClickListener(onClickImageOption(this.mViewHolder.mRelativePhotoContentDraw, imageId, width, height));

            content.addView(image);
        }

        this.mViewHolder.mLinearControlPanel = (LinearLayout) findViewById(R.id.linear_control_panel);
        this.mViewHolder.mLinearSharePanel = (LinearLayout) findViewById(R.id.linear_share_panel);
        this.mViewHolder.mImageZoomIn = (ImageView) findViewById(R.id.image_zoom_in);
        this.mViewHolder.mImageZoomOut = (ImageView) findViewById(R.id.image_zoom_out);
        this.mViewHolder.mImageRotateLeft = (ImageView) findViewById(R.id.image_rotate_left);
        this.mViewHolder.mImageRotateRight = (ImageView) findViewById(R.id.image_rotate_right);
        this.mViewHolder.mImageFinish = (ImageView) findViewById(R.id.image_finish);
        this.mViewHolder.mImageRemove = (ImageView) findViewById(R.id.image_remove);
        this.mViewHolder.mImagePhoto = (ImageView) findViewById(R.id.image_photo);

        this.setListeners();
    }

    private void setListeners() {
        this.findViewById(R.id.image_zoom_in).setOnClickListener(this);
        this.findViewById(R.id.image_zoom_out).setOnClickListener(this);
        this.findViewById(R.id.image_rotate_left).setOnClickListener(this);
        this.findViewById(R.id.image_rotate_right).setOnClickListener(this);
        this.findViewById(R.id.image_finish).setOnClickListener(this);
        this.findViewById(R.id.image_remove).setOnClickListener(this);
        this.findViewById(R.id.image_take_photo).setOnClickListener(this);

        this.findViewById(R.id.image_zoom_in).setOnLongClickListener(this);
        this.findViewById(R.id.image_zoom_out).setOnLongClickListener(this);
        this.findViewById(R.id.image_rotate_left).setOnLongClickListener(this);
        this.findViewById(R.id.image_rotate_right).setOnLongClickListener(this);

        this.findViewById(R.id.image_zoom_in).setOnTouchListener(this);
        this.findViewById(R.id.image_zoom_out).setOnTouchListener(this);
        this.findViewById(R.id.image_rotate_left).setOnTouchListener(this);
        this.findViewById(R.id.image_rotate_right).setOnTouchListener(this);
    }

    private View.OnClickListener onClickImageOption(final RelativeLayout relativeLayout, final Integer imageId, int width, int height) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ImageView image = new ImageView(MainActivity.this);
                image.setBackgroundResource(imageId);
                relativeLayout.addView(image);

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) image.getLayoutParams();
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);

                mImageSelected = image;

                toogleControlPanel(true);

                image.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent motionEvent) {

                        float x, y;

                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                mImageSelected = image;
                                toogleControlPanel(true);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                int coords[] = {0, 0};
                                relativeLayout.getLocationOnScreen(coords);

                                x = (motionEvent.getRawX() - (image.getWidth() / 2));
                                y = (motionEvent.getRawY() - ((coords[1] + 50) + (image.getHeight() / 2)));
                                image.setX(x);
                                image.setY(y);
                                break;
                            case MotionEvent.ACTION_UP:
                                break;
                        }

                        return true;
                    }
                });
            }
        };
    }

    private void toogleControlPanel(boolean showControl) {
        if (showControl) {
            this.mViewHolder.mLinearSharePanel.setVisibility(View.GONE);
            this.mViewHolder.mLinearControlPanel.setVisibility(View.VISIBLE);
        } else {
            this.mViewHolder.mLinearSharePanel.setVisibility(View.VISIBLE);
            this.mViewHolder.mLinearControlPanel.setVisibility(View.GONE);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_take_photo:
                if (!PermissionUtil.hasCameraPermission(this)){
                    PermissionUtil.asksCameraPermission(this);
                }
                dispatchTakePictureIntent();
                break;
            case R.id.image_zoom_in:
                ImageUtil.handleZoomIn(this.mImageSelected);
                break;
            case R.id.image_zoom_out:
                ImageUtil.handleZoomOut(this.mImageSelected);
                break;
            case R.id.image_rotate_left:
                ImageUtil.handleRotateLeft(this.mImageSelected);
                break;
            case R.id.image_rotate_right:
                ImageUtil.handleRotateRight(this.mImageSelected);
                break;
            case R.id.image_finish:
                toogleControlPanel(false);
                break;
            case R.id.image_remove:
                this.mViewHolder.mRelativePhotoContentDraw.removeView(this.mImageSelected);
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK){
            this.setPhotoAsBackGround();
        }
    }

    private void setPhotoAsBackGround() {
        int targetW = this.mViewHolder.mImagePhoto.getWidth();
        int targetH = this.mViewHolder.mImagePhoto.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds =  true;
        BitmapFactory.decodeFile(this.mViewHolder.mUriPhotoPath.getPath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min( photoW / targetW , photoH / targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(this.mViewHolder.mUriPhotoPath.getPath(), bmOptions);

        Bitmap bitmapRotated = ImageUtil.rotateImageIfRquired(bitmap, this.mViewHolder.mUriPhotoPath);
        this.mViewHolder.mImagePhoto.setImageBitmap(bitmap);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.CAMERA_PERMISSION){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                dispatchTakePictureIntent();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.without_permission_camera_explanation))
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
            }

        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = ImageUtil.createImageFile(this);
                this.mViewHolder.mUriPhotoPath = Uri.fromFile(photoFile);
            } catch (IOException ex) {
                Toast.makeText(this,"Não Foi Possível Iniciar a Camera!!!", Toast.LENGTH_SHORT).show();
            }

            if(photoFile != null){
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == R.id.image_zoom_in) this.mLongEventType = LongEventType.ZoomIn;
        if (view.getId() == R.id.image_zoom_out) this.mLongEventType = LongEventType.ZoomOut;
        if (view.getId() == R.id.image_rotate_left) this.mLongEventType = LongEventType.RotateLeft;
        if (view.getId() == R.id.image_rotate_right)
            this.mLongEventType = LongEventType.RotateRight;
        mAutoIncrement = true;
        new RptUpdater().run();
        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int id = view.getId();
        if (id == R.id.image_zoom_in || id == R.id.image_zoom_out || id == R.id.image_rotate_left || id == R.id.image_rotate_right) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP && mAutoIncrement) {
                mAutoIncrement = false;
                this.mLongEventType = null;

            }
        }
        return false;
    }

    private static class ViewHolder {
        LinearLayout mLinearSharePanel;
        LinearLayout mLinearControlPanel;

        ImageView mImageZoomIn;
        ImageView mImageZoomOut;
        ImageView mImageRotateLeft;
        ImageView mImageRotateRight;
        ImageView mImageFinish;
        ImageView mImageRemove;
        ImageView mImagePhoto;

        RelativeLayout mRelativePhotoContentDraw;
        Uri mUriPhotoPath;
    }


    private class RptUpdater implements Runnable {

        @Override
        public void run() {
            if (mAutoIncrement)
                mRptUpdaterHandler.postDelayed(new RptUpdater(), 50);

            if (mLongEventType != null) {
                switch (mLongEventType) {
                    case ZoomIn:
                        ImageUtil.handleZoomIn(mImageSelected);
                        break;
                    case ZoomOut:
                        ImageUtil.handleZoomOut(mImageSelected);
                        break;
                    case RotateLeft:
                        ImageUtil.handleRotateLeft(mImageSelected);
                        break;
                    case RotateRight:
                        ImageUtil.handleRotateRight(mImageSelected);
                        break;
                }
            }
        }
    }
}
