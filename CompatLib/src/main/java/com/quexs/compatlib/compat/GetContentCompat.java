package com.quexs.compatlib.compat;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by Android Studio.
 * <p>
 * author: Quexs
 * <p>
 * Date: 2022/02/16
 * <p>
 * Time: 0:34
 * <p>
 * 备注：调用文件管理器获取文件
 */
public class GetContentCompat {

    private final ActivityResultLauncher<String[]> permissionsLauncher;
    private final ActivityResultLauncher<Intent> contentLauncher;
    private GetContentCompatListener mGetContentCompatListener;
    private boolean isWorking;
    private String[] mimeTypes;
    private int maxSelectCount;

    public GetContentCompat(ActivityResultCaller resultCaller){
        permissionsLauncher = resultCaller.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), mGetContentPermCallback());
        contentLauncher = resultCaller.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), mGetContentResult());
    }

    public void openContent(int selectCount, GetContentCompatListener getContentCompatListener, String... mimeTypes){
        if(isWorking) return;
        maxSelectCount = Math.max(selectCount, 1);
        mGetContentCompatListener = getContentCompatListener;
        isWorking = true;
        this.mimeTypes = mimeTypes;
        permissionsLauncher.launch(getPermissionForInputs(mimeTypes));
    }

    /**
     * 未授予的权限
     * @param perms
     */
    public void onPermissionsDenied(List<String> perms){

    }

    private ActivityResultCallback<Map<String, Boolean>> mGetContentPermCallback(){
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
                    Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    //文件选择类型
                    contentIntent.setType("*/*");
                    //过滤文件类型
                    contentIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    //启动多选
                    contentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, maxSelectCount > 1);
                    contentLauncher.launch(contentIntent);
                }else {
                    //有权限被拒绝
                    isWorking = false;
                    onPermissionsDenied(perms);
                }

            }
        };
    }

    private ActivityResultCallback<ActivityResult> mGetContentResult(){
        return new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                isWorking = false;
                Intent intent = result.getData();
                List<Uri> uriList = null;
                if (intent != null && result.getResultCode() == Activity.RESULT_OK) {
                    if(result.getData().getData() != null){
                        uriList = new ArrayList<>();
                        uriList.add(result.getData().getData());
                    }else if(result.getData().getClipData() != null){
                        ClipData clipData = result.getData().getClipData();
                        int lastCount = clipData.getItemCount();
                        LinkedHashSet<Uri> resultSet = new LinkedHashSet<>();
                        for (int i = 0; i < lastCount; i++) {
                            if (i >= maxSelectCount) {
                                break;
                            }
                            resultSet.add(clipData.getItemAt(i).getUri());
                        }
                        uriList = new ArrayList<>(resultSet);
                    }
                }
                if (mGetContentCompatListener != null) {
                    mGetContentCompatListener.onGetContentResult(uriList);
                }
            }
        };
    }

    private String[] getPermissionForInputs(String... mimeTypes){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(mimeTypes == null || mimeTypes.length == 0){
                return new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO};
            }
            Set<String> set = new HashSet<>();
            for(String input : mimeTypes){
                if(Pattern.compile("image/*").matcher(input).find()){
                    set.add(Manifest.permission.READ_MEDIA_IMAGES);
                }else if(Pattern.compile("video/*").matcher(input).find()){
                    set.add(Manifest.permission.READ_MEDIA_VIDEO);
                }else if(Pattern.compile("audio/*").matcher(input).find()){
                    set.add(Manifest.permission.READ_MEDIA_AUDIO);
                }
            }
            if(!set.isEmpty()){
                return set.toArray(new String[set.size()]);
            }
            return null;
        }
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }


    public interface GetContentCompatListener{
        void onGetContentResult(List<Uri> results);
    }

}
