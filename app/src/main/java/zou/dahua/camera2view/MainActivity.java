package zou.dahua.camera2view;

import android.Manifest;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import zou.dahua.cameralib.CameraView;

public class MainActivity extends AppCompatActivity {

    private CameraView cameraView;
    private TextView takePhotoLin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPer(this);

        cameraView = findViewById(R.id.cameraView);

        cameraView.show(this);

        takePhotoLin = findViewById(R.id.takePhotoLin);

        takePhotoLin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.capture();
            }
        });
    }

    /**
     * 申请照相机权限
     *
     * @param activity
     */
    public void initPer(Activity activity) {
        RxPermissions rxPermissions = new RxPermissions(activity);
        // Must be done during an initialization phase like onCreate
        rxPermissions.request(Manifest.permission.CAMERA).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Boolean aBoolean) {
                if (aBoolean) {
                    Toast.makeText(getBaseContext(), "申请成功", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getBaseContext(), "申请失败", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.onPause();
    }
}
