package com.quexs.compatlib.compat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Created by Android Studio.
 * <p>
 * author: Quexs
 * <p>
 * Date: 2023/09/03
 * <p>
 * Time: 0:24
 * <p>
 * 备注：调用系统相机拍照-严格模式
 */
public class TakeCameraSMCompat {

    private final ActivityResultLauncher<Object> takeCameraLauncher;
    private final ActivityResultLauncher<String[]> cameraPermissionLauncher;
    private TakeCameraSMCompatListener mTakeCameraSMCompatListener;

    public TakeCameraSMCompat(ActivityResultCaller resultCaller){
        cameraPermissionLauncher = resultCaller.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), takePermCallback());
        takeCameraLauncher = resultCaller.registerForActivityResult(takeCameraContract(), takeCameraCallback());
    }

    /**
     * 拍照
     * @param takeCameraSMCompatListener
     */
    public void takeCamera(TakeCameraSMCompatListener takeCameraSMCompatListener){
        this.mTakeCameraSMCompatListener = takeCameraSMCompatListener;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            cameraPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }else {
            cameraPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }
    }

    /**
     * 未授予的权限
     * @param perms
     */
    public void onPermissionsDenied(List<String> perms){

    }

    private ActivityResultCallback<Map<String, Boolean>> takePermCallback(){
        return new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                List<String> perms = new ArrayList<>();
                for(Map.Entry<String, Boolean> entry : result.entrySet()){
                    if(!entry.getValue()){
                        perms.add(entry.getKey());
                    }
                }
                if(perms.isEmpty()){
                    //已获取全部权限
                    takeCameraLauncher.launch(null);
                }else {
                    //有权限被拒绝
                    onPermissionsDenied(perms);
                }
            }
        };
    }

    private ActivityResultContract<Object, Uri> takeCameraContract(){
        return new ActivityResultContract<Object, Uri>() {
            Uri resultUri;
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Object o) {
                File file = getPictureFile(context, (String)o);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N){
                    resultUri = Uri.fromFile(file);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, resultUri);
                }else if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q){
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    resultUri = Uri.fromFile(file);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, resultUri);
                }else {
                    String authorities = context.getApplicationContext().getPackageName() + ".fileProvider";
                    Uri uri = FileProvider.getUriForFile(context, authorities, file);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    resultUri = Uri.fromFile(file);
                }
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                return intent;
            }

            @Override
            public Uri parseResult(int i, @Nullable Intent intent) {
                if(i == Activity.RESULT_OK){
                    return resultUri;
                }
                return null;
            }
        };
    }

    private File getPictureFile(Context context, String directoryPath){
        String fileName = Calendar.getInstance().getTimeInMillis() + ".jpg";
        if(!TextUtils.isEmpty(directoryPath)){
            File directory = new File(directoryPath);
            if(!directory.exists()){
                directory.mkdirs();
            }
            return new File(directory, fileName);
        }else {
            //默认使用外部存储目录
            return new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
        }
    }

    private ActivityResultCallback<Uri> takeCameraCallback(){
        return new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                if(mTakeCameraSMCompatListener != null){
                    mTakeCameraSMCompatListener.onResult(result);
                }
            }
        };
    }

    public interface TakeCameraSMCompatListener{
        void onResult(Uri uri);
    }
}
