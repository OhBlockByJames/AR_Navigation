package arnavigation;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.TransformableNode;
import com.ustglobal.arcloudanchors.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
            getCamera();
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

    public void getCamera(){
        String tag = "ArCore";
        String msg = "Not Yet";
        Frame frame= arFragment.getArSceneView().getArFrame();
        Camera camera = frame.getCamera();

        try  {
            Image image = frame.acquireCameraImage();

            Log.d(image.getHeight()+"-->height ",image.getWidth()+"-->width ");

            WriteImageToSD(image);
            Log.d("save image","success AR.jpg");
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

    public static int[] YUVtoRGB(float y, float u, float v){
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
}
