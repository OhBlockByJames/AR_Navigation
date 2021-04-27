package arnavigation;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.TransformableNode;
import com.ustglobal.arcloudanchors.R;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private CloudAnchorFragment arFragment;
    private ArrayList anchorList;
    private String FROM, MODE;

    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED
    }

    private Anchor anchor;
    private AnchorNode anchorNode;
    private AppAnchorState appAnchorState = AppAnchorState.NONE;
    private String APARTMENT18 = "apartment18_DB";
    private String APARTMENT30 = "apartment18_DB";
    private String PACKENHAMHOUSE = "packenhamHouse_DB";
    private String FIREEXIT = "fireExit_DB";
    private TextView text;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                FROM = null;
            } else {
                FROM = extras.getString(Home.FROM);
                MODE = extras.getString(Home.MODE);
            }
        }

        setContentView(R.layout.activity_main);
        anchorList = new ArrayList();
        // Context of the entire application is passed on to TinyDB
        Storage storage = new Storage(getApplicationContext());
        Button resolve = findViewById(R.id.resolve);

        //add
        Button photo = findViewById(R.id.photo);

        arFragment = (CloudAnchorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        // This part of the code will be executed when the user taps on a plane
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            //Will be used in Admin Mode
            if(MODE.equalsIgnoreCase("admin"))
            {
                Log.d("HIT_RESULT:", hitResult.toString());
                // Used to render 3D model on the top of this anchor
                anchor = arFragment.getArSceneView().getSession().hostCloudAnchor(hitResult.createAnchor());
                appAnchorState = AppAnchorState.HOSTING;
                showToast("Adding Arrow to the scene");
                create3DModel(anchor);
            } else {
                showToast("3D model can be added in Admin mode only");
            }

        });

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

            //add
            try {
                onSceneUpdate(frameTime);
            } catch (NotYetAvailableException e) {
               Log.d("catch","frametime error");
            }

            if (appAnchorState != AppAnchorState.HOSTING)
                return;
            Anchor.CloudAnchorState cloudAnchorState = anchor.getCloudAnchorState();

            if (cloudAnchorState.isError()) {
                showToast(cloudAnchorState.toString());
            } else if (cloudAnchorState == Anchor.CloudAnchorState.SUCCESS) {
                appAnchorState = AppAnchorState.HOSTED;

                String anchorId = anchor.getCloudAnchorId();
                anchorList.add(anchorId);

                if (FROM.equalsIgnoreCase(Home.APARTMENT18)) {
                    storage.addListString(APARTMENT18, anchorList);
                } else if (FROM.equalsIgnoreCase(Home.APARTMENT30)) {
                    storage.addListString(APARTMENT30, anchorList);
                } else if (FROM.equalsIgnoreCase(Home.PACKENHAMHOUSE)) {
                    storage.addListString(PACKENHAMHOUSE, anchorList);
                } else if (FROM.equalsIgnoreCase(Home.FIREEXIT)) {
                    storage.addListString(FIREEXIT, anchorList);
                }

                showToast("Anchor hosted successfully. Anchor Id: " + anchorId);
            }

            //add 0425

        });


        resolve.setOnClickListener(view -> {
            ArrayList<String> stringArrayList = new ArrayList<>();
            if (FROM.equalsIgnoreCase(Home.APARTMENT18)) {
                stringArrayList = storage.getListString(APARTMENT18);
            } else if (FROM.equalsIgnoreCase(Home.APARTMENT30)) {
                stringArrayList = storage.getListString(APARTMENT30);
            } else if (FROM.equalsIgnoreCase(Home.PACKENHAMHOUSE)) {
                stringArrayList = storage.getListString(PACKENHAMHOUSE);
            } else if (FROM.equalsIgnoreCase(Home.FIREEXIT)) {
                stringArrayList = storage.getListString(FIREEXIT);
            }

            for (int i = 0; i < stringArrayList.size(); i++) {
                String anchorId = stringArrayList.get(i);
                if (anchorId.equals("null")) {
                    Toast.makeText(this, "No anchor Id found", Toast.LENGTH_LONG).show();
                    return;
                }

                Anchor resolvedAnchor = arFragment.getArSceneView().getSession().resolveCloudAnchor(anchorId);
                create3DModel(resolvedAnchor);
            }
        });


        photo.setOnClickListener(view->{
            try {
                getCamera();
            } catch (NotYetAvailableException e) {
                Log.d("none","none");
            }
        });


    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    /**
     * Used to build a 3D model
     * @param anchor
     */
    private void create3DModel(Anchor anchor) {
        ModelRenderable
                .builder()
                .setSource(this, Uri.parse("model-triangulated.sfb"))
                .build()
                .thenAccept(modelRenderable -> addModelToScene(anchor, modelRenderable))
                .exceptionally(throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage()).show();
                    return null;
                });

    }
    /**
     * Used to add the 3D model created in create3Dmodel to the scene
     * @param anchor
     * @param modelRenderable
     */
    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
        // anchorNode will position itself based on anchor
        anchorNode = new AnchorNode(anchor);
        // AnchorNode cannot be zoomed in or moved so a TransformableNode is created where AnchorNode is the parent
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        // Setting the angle of 3D model
        transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 180));
        transformableNode.setParent(anchorNode);
        //adding the model to the transformable node
        transformableNode.setRenderable(modelRenderable);
        //adding this to the scene
        arFragment.getArSceneView().getScene().addChild(anchorNode);
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void getCamera() throws NotYetAvailableException {
        String tag = "ArCore";
        String msg = "Not Yet";
        Frame frame= arFragment.getArSceneView().getArFrame();
//        Camera camera = frame.getCamera();
        //PointCloud point = frame.acquirePointCloud();
        //FloatBuffer buf = point.getPoints();
//        try {
////            for(int i =0;i<=buf.array().length;i++){
////                Log.v(i+" ","buffertest"+buf.get(i));
////            }
//            Log.v("buff",""+buf.get(0)+"next"+buf.get(1));
//        }
//        catch(IndexOutOfBoundsException e){
//            Log.v("ARRAY","out of bound");
//        }
//        catch(UnsupportedOperationException e){
//           Log.v("BUFFER","FAILURE");
//        }


        //showToast("tx"+camera.getPose().tx()+"ty"+camera.getPose().ty()+"tz"+camera.getPose().tz());
        try  {
            Image image = frame.acquireCameraImage();

            Log.d(image.getHeight()+"-->height ",image.getWidth()+"-->width ");
            YuvImage yuv = transImage(image);
            Log.d("test YUV"+yuv.getHeight(),yuv.getWidth()+"");

            //add 0426
            final byte[] pixels = NV21toJPEG(YUV_420_888toNV21(image), image.getWidth(), image.getHeight());
            Log.d("pixel length",pixels.length+"");

            //frame.getImageMetadata().getByteArray()
            //WriteImageToSD(image);

            //0426 多了image.getHeight()-1
            //Bitmap RGB = convertYuvImageToRgb(yuv,image.getHeight()-1,image.getWidth()-1,1);

            //Log.d("save image",""+RGB.getHeight()+RGB.getWidth());
            image.close();
        }
        catch (NotYetAvailableException e)
        {
            Log.d(tag,msg);
        }
    }

    private void WriteImageToSD(Image img)
    {
        File root=getExternalFilesDir(null).getAbsoluteFile();
        File path=new File(root.getAbsolutePath()+"/AR");

        if(path.exists()==false)
        {
            path.mkdir();
        }
        File imgFile=new File(path,String.format("AR_%d.jpg",img.getTimestamp()));

        try(FileOutputStream out=new FileOutputStream(imgFile)){
            byte[] nv21;
            ByteBuffer yBuffer = img.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = img.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = img.getPlanes()[2].getBuffer();
            //此方法返回此緩衝區中剩餘的元素數 .remaining()
            int ySize = yBuffer.remaining() ;
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            nv21 = new byte[ySize + uSize + vSize];
            //U and V are swapped
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);
            YuvImage yuv=new YuvImage(nv21, ImageFormat.NV21,img.getWidth(),img.getHeight(),null);
            yuv.compressToJpeg(new Rect(0,0,img.getWidth(),img.getHeight()),100,out);
        } catch (FileNotFoundException e) {
        Log.d("File not found","LOL");
        } catch (IOException e) {
            Log.d("IO ","Crashed");
        }

    }

    public Bitmap convertYuvImageToRgb(YuvImage yuvImage, int width, int height, int downSample){
        //downSample通常是1-4之間 壓縮圖檔
        try{
        Bitmap rgbImage;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 0, out);
        byte[] imageBytes = out.toByteArray();

        BitmapFactory.Options opt;
        opt = new BitmapFactory.Options();

        opt.inSampleSize = downSample;

        // get image and rotate it so (0,0) is in the bottom left
        Bitmap tmpImage;
        Matrix matrix = new Matrix();
        matrix.postRotate(90); // to rotate the camera images so (0,0) is in the bottom left
        tmpImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, opt);
        rgbImage=Bitmap.createBitmap(tmpImage , 0, 0, tmpImage.getWidth(), tmpImage.getHeight(), matrix, true);

        return rgbImage;}
        catch (IllegalArgumentException e){
            Log.d("Bitmap","illegal");
            return null;

        }
    }

    public YuvImage transImage(Image img) {
        File root = getExternalFilesDir(null).getAbsoluteFile();
        File path = new File(root.getAbsolutePath() + "/AR");
        if (path.exists() == false) {
            path.mkdir();
        }
        File imgFile = new File(path, String.format("AR_%d.jpg", img.getTimestamp()));
        try (FileOutputStream out = new FileOutputStream(imgFile)) {
            byte[] nv21;
            ByteBuffer yBuffer = img.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = img.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = img.getPlanes()[2].getBuffer();
            //此方法返回此緩衝區中剩餘的元素數 .remaining()
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            nv21 = new byte[ySize + uSize + vSize];
            //U and V are swapped
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);
            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, img.getWidth(), img.getHeight(), null);
//            yuv.compressToJpeg(new Rect(0, 0, img.getWidth(), img.getHeight()), 100, out);
            return yuv;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static int[] YUVtoRGB(float y, float u, float v){
        //byte buffer值域0-255 method值域0-1
        int[] rgb = new int[3];
        float r,g,b;

        r = (float)((y + 0.000 * u + 1.140 * v) * 255);
        g = (float)((y - 0.396 * u - 0.581 * v) * 255);
        b = (float)((y + 2.029 * u + 0.000 * v) * 255);

        rgb[0] = (int)r;
        rgb[1] = (int)g;
        rgb[2] = (int)b;

        return rgb;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }

    public static void WriteImageInformation(Image image, String path) throws IOException {
        byte[] data = null;
        data = NV21toJPEG(YUV_420_888toNV21(image),
                image.getWidth(), image.getHeight());
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
        bos.write(data);
        bos.flush();
        bos.close();
    }

    private static byte[] YUV_420_888toNV21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    private void onSceneUpdate(FrameTime frameTime) throws NotYetAvailableException {
        try {
            Frame currentFrame = arFragment.getArSceneView().getArFrame();
            Image currentImage = currentFrame.acquireCameraImage();
            int imageFormat = currentImage.getFormat();
            if (imageFormat == ImageFormat.YUV_420_888) {
                Log.d("ImageFormat", "Image format is YUV_420_888");
            }
        }catch (NotYetAvailableException E){
        Log.d("Failed","Photo");
        }
    }
}
