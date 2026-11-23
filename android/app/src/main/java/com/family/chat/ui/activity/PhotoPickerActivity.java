package com.family.chat.ui.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.family.chat.R;
import com.family.chat.ui.adapter.PhotoPickerAdapter;
import com.family.chat.util.AppExecutors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PhotoPickerActivity extends AppCompatActivity {

    private ImageView btnClose;
    private RecyclerView rvPhotos;
    private AppCompatButton btnSendPhotos;
    private PhotoPickerAdapter adapter;

    private List<String> selectedResult = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);

        initView();
        checkPermissionsAndLoad();
    }

    private void initView() {
        btnClose = findViewById(R.id.btn_close);
        rvPhotos = findViewById(R.id.rv_photos);
        btnSendPhotos = findViewById(R.id.btn_send_photos);

        // 设置 4 列网格布局
        rvPhotos.setLayoutManager(new GridLayoutManager(this, 4));
        adapter = new PhotoPickerAdapter();
        rvPhotos.setAdapter(adapter);

        btnClose.setOnClickListener(v -> finish());

        // 微信高亮控制：默认没有选择图片时，发送按钮不可点击
        btnSendPhotos.setEnabled(false);
        btnSendPhotos.setText("发送");

        btnSendPhotos.setOnClickListener(v -> {
            // 将选中的图片物理路径传回给 ChatActivity
            Intent intent = new Intent();
            intent.putExtra("selected_photos", (Serializable) selectedResult);
            setResult(RESULT_OK, intent);
            finish();
        });

        // 监听选择框变化
        adapter.setOnPhotoSelectListener((selectedCount, selectedPaths) -> {
            this.selectedResult = selectedPaths;
            if (selectedCount > 0) {
                btnSendPhotos.setEnabled(true);
                btnSendPhotos.setText(String.format("发送(%d)", selectedCount));
            } else {
                btnSendPhotos.setEnabled(false);
                btnSendPhotos.setText("发送");
            }
        });
    }

    private void checkPermissionsAndLoad() {
        // 请求本地存储读取权限
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        // 如果是 Android 13 及以上，请求专属媒体图片权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 201);
        } else {
            loadLocalPhotos(); // 权限已开，直接读库
        }
    }

    /**
     * 【核心读库】：一行代码不写多余缓存，直接读取系统媒体库并按日期降序排列
     */
    private void loadLocalPhotos() {
        AppExecutors.io().execute(() -> {
            List<String> paths = new ArrayList<>();
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver resolver = getContentResolver();

            String[] projection = {MediaStore.Images.Media.DATA};
            // 按照添加日期降序排列（最新添加在最上面）
            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

            try (Cursor cursor = resolver.query(uri, projection, null, null, sortOrder)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        String path = cursor.getString(index);
                        paths.add(path);
                    }
                }
            } catch (Exception e) {
                Log.e("PhotoPickerActivity", "读取系统媒体库图片失败", e);
            }

            // 回到主线程渲染
            runOnUiThread(() -> adapter.setData(paths));
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 201 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadLocalPhotos();
        } else {
            Toast.makeText(this, "需要读取存储权限才能选择图片", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}