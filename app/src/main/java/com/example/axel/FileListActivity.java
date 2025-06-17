package com.example.axel;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;

public class FileListActivity extends AppCompatActivity {

    private ListView fileListListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        fileListListView = findViewById(R.id.file_list);

        // Получите список всех сохраненных файлов CSV
        File directory = getFilesDir();
        FilenameFilter filter = (dir, name) -> name.endsWith(".csv");
        File[] files = directory.listFiles(filter);

        if (files != null) {
            List<String> fileNames = new ArrayList<>();
            for (File file : files) {
                fileNames.add(file.getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
            fileListListView.setAdapter(adapter);

            // Установите обработчик нажатий на элементы списка
            fileListListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Лог для отладки
                    Log.d("FileListActivity", "Item clicked: " + position);
                    String fileName = fileNames.get(position);
                    File selectedFile = new File(directory, fileName);
                    shareFile(selectedFile);
                }
            });
        } else {
            Log.e("FileListActivity", "Файлы не найдены");
            Toast.makeText(this, "Файлы не найдены", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        try {
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Поделитесь файлом через"));
        } catch (Exception e) {
            Log.e("FileSharing", "Общий доступ к файлу с ошибкой", e);
            Toast.makeText(this, "Общий доступ к файлу с ошибкой", Toast.LENGTH_SHORT).show();
        }
    }
}