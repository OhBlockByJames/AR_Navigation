package arnavigation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.ar.core.exceptions.NotYetAvailableException;
import com.ustglobal.arcloudanchors.R;

public class DrawImage extends AppCompatActivity {
    private static final String TAG ="image";
    private myView customCanvas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_image);
        customCanvas=new myView(this,null);

        setContentView(customCanvas);
//        Button button = findViewById(R.id.clearBtn);
//        button.setOnClickListener(view->{
//          clearCanvas(view);
//        });
    }

    public void clearCanvas(View v) {
        Button button = findViewById(R.id.clearBtn);
        button.setOnClickListener(view->{
        customCanvas.clearCanvas();});
    }
}