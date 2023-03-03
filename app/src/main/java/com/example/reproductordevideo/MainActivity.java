package com.example.reproductordevideo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<VideoItem> videoItems;
    private ListView listView;
    private VideoAdapter videoAdapter;
    private ProgressBar progressBar;
    private TextView textViewCargando;
    private TextInputEditText searchEditText;
    private FloatingActionButton fabButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File externalStorageDirectory = Environment.getExternalStorageDirectory();

        fabButton = findViewById(R.id.fab);
        listView = findViewById(R.id.videoListView);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
        textViewCargando = findViewById(R.id.tv_cargando);
        searchEditText = findViewById(R.id.searchEditText);

        progressBar.setVisibility(View.VISIBLE);
        textViewCargando.setVisibility(View.VISIBLE);

        videoItems = new ArrayList<>();
        videoAdapter = new VideoAdapter(this, R.layout.list_item_video, videoItems);
        listView.setAdapter(videoAdapter);

        new GetAllVideosTask(this, videoAdapter, progressBar, textViewCargando).execute(externalStorageDirectory);

        // Agrega un evento para reproducir el video al hacer clic en un elemento del ListView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                VideoItem videoItem = videoItems.get(position);
                String videoPath = videoItem.getFullPath();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoPath));
                intent.setDataAndType(Uri.parse(videoPath), "video/mp4");
                startActivity(intent);
            }
        });


        // Agrega un evento para buscar videos en la listView cuando cambie los valores el search_Edit_Text
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Actualizar la lista de videos con el término de búsqueda ingresado por el usuario.
                videoAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Agrega un evento para recargar los videos de la listView
        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoAdapter.clear();
                videoAdapter.notifyDataSetChanged();
                new GetAllVideosTask(MainActivity.this, videoAdapter, progressBar, textViewCargando).execute(externalStorageDirectory);
            }
        });

    }

    public class GetAllVideosTask extends AsyncTask<File, Void, List<VideoItem>> {
        private Context context;
        private VideoAdapter videoAdapter;
        private ProgressBar progressBar;
        private TextView textViewCargando;

        public GetAllVideosTask(Context context, VideoAdapter videoAdapter, ProgressBar progressBar, TextView textViewCargando) {
            this.context = context;
            this.videoAdapter = videoAdapter;
            this.progressBar = progressBar;
            this.textViewCargando = textViewCargando;
        }

        @Override
        protected void onPreExecute() {
            searchEditText.setEnabled(false);
            fabButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            textViewCargando.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<VideoItem> doInBackground(File... params) {
            List<VideoItem> videoItems = new ArrayList<>();

            // Recorremos cada directorio pasado como argumento
            for (File dir : params) {
                // Llamamos al método recursivo que busca videos en el directorio y subdirectorios
                getAllVideos(dir, videoItems);
            }

            return videoItems;
        }

        @Override
        protected void onPostExecute(List<VideoItem> videoItems) {
            this.videoAdapter.clear();
            this.videoAdapter.addAll(videoItems);
            this.videoAdapter.notifyDataSetChanged();
            this.progressBar.setVisibility(View.GONE);
            this.textViewCargando.setVisibility(View.GONE);
            searchEditText.setEnabled(true);
            fabButton.setEnabled(true);
        }

        private void getAllVideos(File dir, List<VideoItem> videoItems) {
            File[] files = dir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Si encontramos un directorio, llamamos de forma recursiva al método getAllVideos
                        getAllVideos(file, videoItems);
                    } else {
                        String filePath = file.getAbsolutePath();
                        if (filePath.endsWith(".mp4") || filePath.endsWith(".avi")
                                || filePath.endsWith(".3gp") || filePath.endsWith(".mkv")) {
                            VideoItem videoItem = new VideoItem(file.getName(), filePath);
                            videoItems.add(videoItem);
                        }
                    }
                }
            }
        }
    }
}

/*
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private ListView videoListView;
    private VideoAdapter videoAdapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtener una referencia a la ListView y al ProgressBar
        videoListView = findViewById(R.id.videoListView);
        progressBar = findViewById(R.id.progressBar);

        // Solicitar permisos de almacenamiento si aún no se han otorgado
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permiso de almacenamiento ya otorgado, buscar videos
            getAllVideos();
        }

        // Manejar los clicks en los elementos de la ListView
        videoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Manejar el click en el video seleccionado
            }
        });
    }

    private void getAllVideos() {
        // Mostrar el ProgressBar
        progressBar.setVisibility(View.VISIBLE);

        // Obtener la ruta del directorio raíz del almacenamiento externo
        File externalStorageDirectory = Environment.getExternalStorageDirectory();

        // Obtener una lista de todos los videos en el almacenamiento interno
        List<VideoItem> videoItemsList = getAllVideoFiles(externalStorageDirectory);

        // Crear un adaptador para la lista de videos y establecerlo en la ListView
        videoAdapter = new VideoAdapter(this, videoItemsList);
        videoListView.setAdapter(videoAdapter);

        // Ocultar el ProgressBar
        progressBar.setVisibility(View.GONE);
    }

    private List<VideoItem> getAllVideoFiles(File directory) {
        List<VideoItem> videoList = new ArrayList<>();
        File[] files = directory.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                videoList.addAll(getAllVideoFiles(file));
            } else {
                String fileName = file.getName();
                if (fileName.endsWith(".mp4") || fileName.endsWith(".avi")
                        || fileName.endsWith(".3gp") || fileName.endsWith(".mkv")) {
                    Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(
                            fileName,
                            MediaStore.Video.Thumbnails.MICRO_KIND
                    );
                    videoList.add(new VideoItem(fileName , thumbnail,file.getAbsolutePath()));
                }
            }
        }

        return videoList;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso de almacenamiento otorgado, buscar videos
                getAllVideos();
            } else {
                // Permiso de almacenamiento no otorgado, mostrar un mensaje al usuario
            }
        }
    }
}*/
