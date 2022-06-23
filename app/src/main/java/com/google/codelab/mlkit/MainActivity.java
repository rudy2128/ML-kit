// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.codelab.mlkit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.codelab.mlkit.GraphicOverlay.Graphic;
import com.google.codelab.mlkit.camera.CamActivity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.interfaces.Detector;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import static com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";
    private ImageView mImageView;
    ImageButton btnCamera;
    private Button mTextButton;
    private Button mFaceButton;
    String person;
    private Uri imgUri;
    private Uri imageUri;
    private GraphicOverlay mGraphicOverlay;
    // Max width (portrait mode)
    private Integer mImageMaxWidth;
    // Max height (portrait mode)
    private Integer mImageMaxHeight;
    TextRecognizer recognizer;

    /**
     * Number of results to show in the UI.
     */
    private static final int RESULTS_TO_SHOW = 3;
    /**
     * Dimensions of inputs.
     */
    private static final int DIM_IMG_SIZE_X = 224;
    private static final int DIM_IMG_SIZE_Y = 224;

    private final PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    (o1, o2) -> (o1.getValue()).compareTo(o2.getValue()));

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    assert intent != null;
                    person = intent.getStringExtra("ImgPerson");
                    imgUri = Uri.parse(person);
                    CropImage.activity(imgUri)
                            .setAspectRatio(3,2)
                            .start(this);

                }
                //Menampilkan Gambar hasil Cropping Image
                if(result.getResultCode() == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                    CropImage.ActivityResult result1 = CropImage.getActivityResult(result.getData());
                    if (result.getResultCode() == RESULT_OK) {
                        if (result1 != null) {
                            imageUri = result1.getUri(); //Mengubah data image kedalam Uri
                        }

                        //Menampilkan Gambar pada ImageView
                        runTextRecognition(imageUri);

                    }else if(result.getResultCode() == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                        //Menangani Jika terjadi kesalahan
                        String error = null;
                        if (result1 != null) {
                            error = result1.getError().toString();
                        }
                        Log.d("Exception", error);
                        Toast.makeText(getApplicationContext(), "Crop Image Error", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.image_view);

        mTextButton = findViewById(R.id.button_text);
        mFaceButton = findViewById(R.id.button_face);
        btnCamera = findViewById(R.id.btn_camera);

        mGraphicOverlay = findViewById(R.id.graphic_overlay);

        btnCamera.setOnClickListener(view -> {

            Intent intent = new Intent(MainActivity.this, CamActivity.class);
            mStartForResult.launch(intent);
        });

        mTextButton.setOnClickListener(v -> runFaceContourDetection());
        mFaceButton.setOnClickListener(view -> runFaceContourDetection());


    }


    private void runTextRecognition(Uri imageUri) {
        InputImage image = null;
        try {
            image = InputImage.fromFilePath(getApplicationContext(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        recognizer = TextRecognition.getClient(DEFAULT_OPTIONS);
        mTextButton.setEnabled(false);
        assert image != null;
        recognizer.process(image)
                .addOnSuccessListener(
                        texts -> {
                            mTextButton.setEnabled(true);
                            processTextRecognitionResult(texts);
                        })
                .addOnFailureListener(
                        e -> {
                            // Task failed with an exception
                            mTextButton.setEnabled(true);
                            e.printStackTrace();
                        });
        recognizer.setProcessor(new Detector.Processor<Text.TextBlock>()
    }






    private void processTextRecognitionResult(Text texts) {
        List<Text.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            showToast("No text found");
            return;
        }
        mGraphicOverlay.clear();
        for (int i = 0; i < blocks.size(); i++) {
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {

                    Graphic textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
                    mGraphicOverlay.add(textGraphic);


                }
            }
        }
    }



    private void runFaceContourDetection() {
        InputImage image = null;
        try {
            image = InputImage.fromFilePath(getApplicationContext(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

        mFaceButton.setEnabled(false);
        FaceDetector detector = FaceDetection.getClient(options);
        assert image != null;
        detector.process(image)
                .addOnSuccessListener(
                        faces -> {
                            mFaceButton.setEnabled(true);
                            processFaceContourDetectionResult(faces);
                        })
                .addOnFailureListener(
                        e -> {
                            // Task failed with an exception
                            mFaceButton.setEnabled(true);
                            e.printStackTrace();
                        });

    }

    private void processFaceContourDetectionResult(List<Face> faces) {
        // Task completed successfully
        if (faces.size() == 0) {
            showToast("No face found");
            return;
        }
        mGraphicOverlay.clear();
        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.get(i);
            FaceContourGraphic faceGraphic = new FaceContourGraphic(mGraphicOverlay);
            mGraphicOverlay.add(faceGraphic);
            faceGraphic.updateFace(face);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Functions for loading images from app assets.

    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxWidth() {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = mImageView.getWidth();
        }

        return mImageMaxWidth;
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxHeight() {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxHeight =
                    mImageView.getHeight();
        }

        return mImageMaxHeight;
    }

    // Gets the targeted width / height.
    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;
        int maxWidthForPortraitMode = getImageMaxWidth();
        int maxHeightForPortraitMode = getImageMaxHeight();
        targetWidth = maxWidthForPortraitMode;
        targetHeight = maxHeightForPortraitMode;
        return new Pair<>(targetWidth, targetHeight);
    }

}
