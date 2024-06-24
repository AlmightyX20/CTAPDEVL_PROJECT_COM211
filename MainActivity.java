package com.example.faceshape;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.faceshape.ml.Vgg16Face2;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    Button camera;
    Button gallery;
    TextView resultText;
    TextView haircutRecoText;
    TextView classifiedText;
    int imageSize = 224;  // Correct image size for the model
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    // Haircut Recommendations based on face shapes
    Map<String, String> haircutReco = new HashMap<>();

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Initialization failed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Haircut Recommendations
        initializeHaircutReco();

        camera = findViewById(R.id.takeAPicButton);
        gallery = findViewById(R.id.launchGalButton);
        resultText = findViewById(R.id.resultText);
        imageView = findViewById(R.id.imageView);
        haircutRecoText = findViewById(R.id.tvHaircutRecommendation);
        classifiedText = findViewById(R.id.classifiedText);


        // Create a SpannableString to set different colors
        SpannableString spannableString = new SpannableString("Know Your Face Shape");

        // Set "Know Your" to black color
        spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set "Face Shape" to blue color
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.facefit_blue)), 10, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the text of the TextView
        classifiedText.setText(spannableString);



        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                }
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, REQUEST_IMAGE_GALLERY);
            }
        });
    }
    private void initializeHaircutReco() {
        // Initialize Haircut Recommendations based on face shapes
        haircutReco.put("Oblong", "Textured Crop, Side Part, Buzz Cut, Caesar Cut, Crew Cut with Faded Sides, Classic Taper, Medium Length with Side Part, Layered Cut with Fringe, Textured and Messy Styles");
        haircutReco.put("Round", "High and Tight, Crew Cut, Side Part with Short Sides, Pompadour, Quiff, Undercut with Slicked Back Hair, Textured Fringe, Longer Layers with Volume, Angular Fringe, Textured and Spiky Styles");
        haircutReco.put("Square", "Textured Crop, Buzz Cut, Caesar Cut, Crew Cut with Faded Sides, Classic Taper, Side Part, Pompadour, Quiff, Textured Fringe, Messy Waves");
        haircutReco.put("Oval", "Buzz Cut, Crew Cut, Textured Crop, Side Part, Classic Taper, Ivy League, Pompadour, Quiff, Layered Cut, Messy Waves");
        haircutReco.put("Heart", "Textured Crop, Side Part, Crew Cut, Classic Taper, Faux Hawk, Medium Length Layers, Wavy or Curly Hair, Longer Fringe, Messy Top, Undercut with volume");
    }



    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("MainActivity", "onActivityResult called with requestCode: " + requestCode + " and resultCode: " + resultCode);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                if (extras != null && extras.containsKey("data")) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        imageView.setImageBitmap(imageBitmap);
                        classifyImage(imageBitmap);
                        updateClassifiedText("Your Face Shape is classified as:");
                    } else {
                        Log.e("MainActivity", "Null bitmap received from camera");
                    }
                } else {
                    Log.e("MainActivity", "No data received from camera");
                }
            } else if (requestCode == REQUEST_IMAGE_GALLERY) {
                Log.d("MainActivity", "Gallery intent data: " + data);
                if (data != null && data.getData() != null) {
                    Uri selectedImageUri = data.getData();
                    Log.d("MainActivity", "Selected image URI: " + selectedImageUri);
                    try {
                        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        if (imageBitmap != null) {
                            imageView.setImageBitmap(imageBitmap);
                            classifyImage(imageBitmap);
                            updateClassifiedText("Your Face Shape is classified as:");
                        } else {
                            Log.e("MainActivity", "Null bitmap received from gallery");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("MainActivity", "Error loading image from gallery: " + e.getMessage());
                    }
                } else {
                    Log.e("MainActivity", "No valid data received from gallery");
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Log.d("MainActivity", "User cancelled the selection");
            Toast.makeText(this, "Selection cancelled", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("MainActivity", "Unexpected resultCode: " + resultCode);
        }
    }





    public void classifyImage(Bitmap image) {
        try {
            // Extract the face from the image
            Bitmap faceImage = extractFace(image);

            if (faceImage == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultText.setText("Face not found");
                        haircutRecoText.setText("No Recommendation");
                    }
                });
                return;
            }

            Vgg16Face2 model = Vgg16Face2.newInstance(getApplicationContext());

            // Define input and output tensor shapes
            int batchSize = 1;
            int numChannels = 3;

            // Create input tensor buffer
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{batchSize, imageSize, imageSize, numChannels}, DataType.FLOAT32);

            // Convert Bitmap to ByteBuffer
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(faceImage);

            // Load data into input tensor buffer
            inputFeature0.loadBuffer(byteBuffer);

            // Run inference
            Vgg16Face2.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            // Process the output
            float[] outputArray = outputFeature0.getFloatArray();
            String faceShape = getFaceShape(outputArray);
            float probability = outputArray[argMax(outputArray)];

            // Log the outputs for debugging
            Log.d("FaceShape", "Model output: " + Arrays.toString(outputArray));
            Log.d("FaceShape", "Predicted face shape: " + faceShape + " with probability: " + probability);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (faceShape != null) {
                        resultText.setText(faceShape + "\nProbability: " + String.format("%.2f", probability * 100) + "%");

                        int textColor = getResources().getColor(R.color.facefit_blue);
                        int ShadowColor = Color.BLACK;
                        float shadowRadius = 4;
                        float shadowDy = 4;
                        resultText.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
                        resultText.setTextSize(20);
                        resultText.setTextColor(textColor);
                        resultText.setShadowLayer(shadowRadius,0,shadowDy,ShadowColor);


                        if(haircutReco.containsKey(faceShape)){
                            haircutRecoText.setText("Recommended Haircuts:\n" + haircutReco.get(faceShape));
                        } else {
                            haircutRecoText.setText("No recommendations");
                        }


                    } else {
                        resultText.setText("Face Shape: Unknown");
                        haircutRecoText.setText("No Recommendation");
                    }
                }
            });

            // Close the model to release resources
            model.close();
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    resultText.setText("Face Shape: Unknown");
                }
            });
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int numBytesPerChannel = 4; // Float.SIZE / Byte.SIZE
        int inputChannels = 3; // RGB

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(imageSize * imageSize * inputChannels * numBytesPerChannel);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[imageSize * imageSize];
        bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true);
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < imageSize; ++i) {
            for (int j = 0; j < imageSize; ++j) {
                int pixelValue = intValues[pixel++];
                byteBuffer.putFloat(((pixelValue >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((pixelValue >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((pixelValue & 0xFF) / 255.0f);
            }
        }
        return byteBuffer;
    }

    private Bitmap extractFace(Bitmap img) {
        // Convert Bitmap to Mat
        Mat matImage = new Mat();
        Utils.bitmapToMat(img, matImage);

        // Convert to Grayscale
        Mat grayImage = new Mat();
        Imgproc.cvtColor(matImage, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Load Haar Cascade
        CascadeClassifier faceDetector = new CascadeClassifier();
        File cascadeFile = new File(getFilesDir(), "haarcascade_frontalface_default.xml");
        if (!cascadeFile.exists()) {
            try (InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                 FileOutputStream os = new FileOutputStream(cascadeFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        faceDetector.load(cascadeFile.getAbsolutePath());

        // Detect Faces
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(grayImage, faceDetections);

        Rect[] facesArray = faceDetections.toArray();
        if (facesArray.length == 0) {
            return null;
        }

        // Get the first detected face
        Rect face = facesArray[0];
        Log.d("FaceDetection", "Face detected at: " + face.x + ", " + face.y);

        // Expand the bounding box slightly
        int adj_h = 10;
        int new_y1 = Math.max(face.y - adj_h, 0);
        int new_y2 = Math.min(face.y + face.height + adj_h, matImage.rows());

        int new_height = new_y2 - new_y1;
        int adj_w = (new_height - face.width) / 2;
        int new_x1 = Math.max(face.x - adj_w, 0);
        int new_x2 = Math.min(face.x + face.width + adj_w, matImage.cols());

        // Crop the image to the new bounding box
        Rect expandedFace = new Rect(new_x1, new_y1, new_x2 - new_x1, new_height);
        Mat faceMat = new Mat(matImage, expandedFace);

        // Convert back to Bitmap
        Bitmap faceBitmap = Bitmap.createBitmap(faceMat.cols(), faceMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(faceMat, faceBitmap);

        // Resize to target size
        return Bitmap.createScaledBitmap(faceBitmap, imageSize, imageSize, false);
    }

    private String getFaceShape(float[] output) {
        // Map the output to the corresponding face shape label
        String[] labels = {"Heart", "Oblong", "Oval", "Round", "Square"};
        float maxThreshold = 0.5f; // Adjust this threshold as needed
        int maxIdx = argMax(output);
        if (output[maxIdx] < maxThreshold) {
            return "Unknown";
        } else {
            return labels[maxIdx];
        }
    }

    private int argMax(float[] array) {
        int maxIdx = -1;
        float max = Float.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    private void updateClassifiedText(String text){
        classifiedText.setText(text);
    }

}



