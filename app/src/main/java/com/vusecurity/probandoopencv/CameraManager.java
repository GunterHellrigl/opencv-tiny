package com.vusecurity.probandoopencv;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class CameraManager {
    /**
     * Executor encargado del background thread para cameraX.
     */
    private final ExecutorService cameraExecutor;
    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private SurfaceView surfaceView;

    public CameraManager(AppCompatActivity activity) {
        this.activity = activity;

        surfaceView = activity.findViewById(R.id.surfaceView);
        previewView = activity.findViewById(R.id.preview);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @SuppressLint({"UnsafeOptInUsageError", "ClickableViewAccessibility"})
    public void start() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activity);

        cameraProviderFuture.addListener(() -> {
            Size resolution;
            if (activity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
                resolution = new Size(1920, 1080);
            } else {
                resolution = new Size(1080, 1920);
            }

            WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            int rotation = windowManager.getDefaultDisplay().getRotation();

            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .setTargetResolution(resolution)
                        .setTargetRotation(rotation)
                        .build();
                previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis tensorFlowAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(resolution)
                        .setTargetRotation(rotation)
                        .build();

                tensorFlowAnalysis.setAnalyzer(cameraExecutor, image -> {

                    JavaCamera2Frame frame = new JavaCamera2Frame(image.getImage());
                    Mat mat = frame.gray();
                    Mat dst = new Mat(mat.size(), mat.type());

                    Core.rotate(mat, dst, 0);

                    Mat laplacianMat = new Mat(dst.size(), dst.type());

                    Imgproc.Laplacian(dst, laplacianMat, COLOR_BGR2GRAY);

                    MatOfDouble median = new MatOfDouble();
                    MatOfDouble std = new MatOfDouble();
                    Core.meanStdDev(laplacianMat, median, std);
                    double variance = Math.pow(std.get(0, 0)[0], 2);

                    Log.e("variance", String.valueOf(variance));


                    Bitmap bmp = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(dst, bmp);

                    activity.runOnUiThread(() -> {
                        Canvas canvas = surfaceView.getHolder().lockCanvas();
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        canvas.drawBitmap(bmp, 0, 0, null);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setTextSize(30);
                        if (variance < 150) {
                            canvas.drawText("Blured", 20, 20, paint);
                        } else {
                            canvas.drawText("NOT Blured", 20, 20, paint);
                        }
                        canvas.drawText(String.valueOf(variance), 20, 50, paint);
                        surfaceView.getHolder().unlockCanvasAndPost(canvas);
                    });

                    frame.release();
                    image.close();
                });

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(
                        activity,
                        cameraSelector,
                        preview,
                        tensorFlowAnalysis
                );

                CameraControl cameraControl = camera.getCameraControl();

                previewView.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        MeteringPointFactory factory = previewView.getMeteringPointFactory();
                        MeteringPoint point = factory.createPoint(event.getX(), event.getY());

                        FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();

                        cameraControl.startFocusAndMetering(action);
                        return true;
                    }
                    return false;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    public void stop() {
        cameraExecutor.shutdown();
    }
}
