package com.vusecurity.probandoopencv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    static {
        if (OpenCVLoader.initDebug()) {
            Log.e("Gunter", "OpenCV OK");
        } else {
            Log.e("Gunter", "OpenCV FAILED");
        }
    }

    CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraManager = new CameraManager(this);

        requestCameraPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraManager.stop();
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, 1);
            return;
        }

        View.OnClickListener listener = view -> ActivityCompat.requestPermissions(
                this,
                permissions,
                1
        );

        Snackbar.make(getWindow().getDecorView(), "Permisos",
                Snackbar.LENGTH_INDEFINITE)
                .setAction("Ok", listener)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 1) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // we have permission
            cameraManager.start();
            //viewModel.resetTimeOut();
            return;
        }


        DialogInterface.OnClickListener listener = (dialog, id) -> {
            //exit("Fin incorrecto: No hay permisos para utilizar la camara","CAMERA_PERMISSION_DENIED", 5);
            Toast.makeText(this, "No hay permisos para utilizar la cámara", Toast.LENGTH_SHORT).show();
            finish();
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name))
                .setMessage("Permiso de camara")
                .setPositiveButton("Ok", listener)
                .show();
    }
}