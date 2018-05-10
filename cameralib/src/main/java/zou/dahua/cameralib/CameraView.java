package zou.dahua.cameralib;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 相机 Kotlin版移植
 * Created by Deep on 2018/5/10 0010.
 */

public class CameraView extends LinearLayout {

    private Context context;
    /**
     * 相机设备类
     * The CameraDevice class is a representation of a single camera connected to an
     * Android device, allowing for fine-grain control of image capture and
     * post-processing at high frame rates.
     */
    private CameraDevice cameraDevice;

    /**
     * <p>A system service manager for detecting, characterizing, and connecting to
     * {@link CameraDevice CameraDevices}.</p>
     **/
    private CameraManager cameraManager;

    /**
     * 调用相机设备id
     */
    private String mCameraID = "0";

    /**
     * 最佳尺寸
     */
    private Size mPreviewSize;

    /**
     * 配置
     * An immutable package of settings and outputs needed to capture a single
     * image from the camera device.
     */
    private CaptureRequest.Builder mPreviewBuilder;

    /**
     * 允许应用程序直接访问呈现表面的图像数据
     * The ImageReader class allows direct application access to image data
     * rendered into a {@link Surface}
     */
    private ImageReader mImageReader;

    /**
     * 消息机制
     */
    private Handler mHandler;

    private TextureView mPreviewView;

    public CameraView(Context context) {
        super(context);
        init(context);
    }

    public CameraView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;

        View view = LayoutInflater.from(context).inflate(R.layout.camera_our_layout, null);
        mPreviewView = view.findViewById(R.id.mPreviewView);

        addView(view);

        initLooper();

        initView();
    }

    /**
     * 很多过程都变成了异步的了，所以这里需要一个子线程的looper
     */
    private void initLooper() {
        /**
         * Google封装的
         * Handy class for starting a new thread that has a looper. The looper can then be
         * used to create handler classes. Note that start() must still be called.
         */
        HandlerThread mThreadHandler = new HandlerThread("CAMERA2");
        mThreadHandler.start();
        mHandler = new Handler(mThreadHandler.getLooper());
    }

    /**
     * 可以通过TextureView或者SurfaceView
     */
    private void initView() {

        /**
         * This listener can be used to be notified when the surface texture
         * associated with this texture view is available.
         * 当与此纹理视图关联的表面纹理可用时，可以使用此侦听器来通知
         */

        mPreviewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            /**
             * Invoked when a {@link TextureView}'s SurfaceTexture is ready for use.
             *
             * @param surface The surface returned by
             *                {@link TextureView#getSurfaceTexture()}
             * @param width The width of the surface
             * @param height The height of the surface
             */
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                try {
                    /**
                     * 获得所有摄像头的管理者CameraManager
                     */
                    cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                    /**
                     * 获得某个摄像头的特征，支持的参数
                     */
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(mCameraID);
                    /**
                     * 支持的STREAM CONFIGURATION
                     */
                    StreamConfigurationMap map = characteristics
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    /**
                     * 摄像头支持的预览Size数组
                     */
                    Size[] sizes = map != null ? map.getOutputSizes(SurfaceTexture.class) : new Size[0];
                    /**
                     * 获取全屏像素 坚果pro2 高度获取不准确
                     */
                    DisplayMetrics dm = new DisplayMetrics();
                    WindowManager windowManager = (WindowManager) context
                            .getSystemService(Context.WINDOW_SERVICE);
                    windowManager.getDefaultDisplay().getMetrics(dm);

                    /**
                     * 获取最佳预览尺寸
                     */
                    mPreviewSize = getCloselyPreSize(dm.heightPixels, dm.widthPixels, sizes);
                    /**
                     * 打开相机
                     */
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mHandler);

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            /**
             * Invoked when the {@link SurfaceTexture}'s buffers size changed.
             *
             * @param surface The surface returned by
             *                {@link TextureView#getSurfaceTexture()}
             * @param width The new width of the surface
             * @param height The new height of the surface
             */
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            /**
             * Invoked when the specified {@link SurfaceTexture} is about to be destroyed.
             * If returns true, no rendering should happen inside the surface texture after this method
             * is invoked. If returns false, the client needs to call {@link SurfaceTexture#release()}.
             * Most applications should return true.
             *
             * @param surface The surface about to be destroyed
             */
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            /**
             * 这个方法要注意一下，因为每有一帧画面，都会回调一次此方法
             * Invoked when the specified {@link SurfaceTexture} is updated through
             * {@link SurfaceTexture#updateTexImage()}.
             *
             * @param surface The surface just updated
             */
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     *
     * @param surfaceWidth  需要被进行对比的原宽
     * @param surfaceHeight 需要被进行对比的原高
     * @param preSizeList   需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    private Size getCloselyPreSize(int surfaceWidth, int surfaceHeight, Size[] preSizeList) {

        // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高
        //        if (mIsPortrait) {
        //            ReqTmpWidth = surfaceHeight;
        //            ReqTmpHeight = surfaceWidth;
        //        } else {
        //        }
        /**
         * 先查找preview中是否存在与surfaceView相同宽高的尺寸
         */
        for (Size size : preSizeList) {
            if (size.getWidth() == surfaceWidth && size.getHeight() == surfaceHeight) {
                return size;
            }
        }

        /**
         * 得到与传入的宽高比最接近的size
         */
        float reqRatio = (float) surfaceWidth / surfaceHeight;
        float curRatio;
        float deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Size retSize = null;
        for (Size size : preSizeList) {
            curRatio = (float) size.getWidth() / size.getHeight();
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }

        return retSize;
    }

    /**
     * 设备监听
     */
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview(cameraDevice);
            //Log.i("相机", "打开");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            //Log.i("相机", "断开");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            //Log.i("相机", "错误");
        }

        @Override
        public void onClosed(CameraDevice camera) {
            //Log.i("相机", "关闭");
        }

    };

    /**
     * 开始预览，主要是camera.createCaptureSession这段代码很重要，创建会话
     */
    private void startPreview(CameraDevice camera) {
        SurfaceTexture texture = mPreviewView.getSurfaceTexture();

        // 这里设置的就是预览大小
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        Surface surface = new Surface(texture);
        try {
            // 设置捕获请求为预览，这里还有拍照啊，录像等
            mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // 就是在这里，通过这个set(key,value)方法，设置曝光啊，自动聚焦等参数！！ 如下举例：
        // mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        mImageReader = ImageReader.newInstance(mPreviewSize.getHeight(), mPreviewSize.getWidth(),
                ImageFormat.JPEG/* 此处还有很多格式，比如我所用到YUV等 */, 2/*
                                                             * 最大的图片数，
															 * mImageReader里能获取到图片数
															 * ，
															 * 但是实际中是2+1张图片，就是多一张
															 */);

        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);
        // 这里一定分别add两个surface，一个Textureview的，一个ImageReader的，如果没add，会造成没摄像头预览，或者没有ImageReader的那个回调！！
        mPreviewBuilder.addTarget(surface);
        mPreviewBuilder.addTarget(mImageReader.getSurface());
        try {
            camera.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    mSessionStateCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 会话监听
     */
    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            updatePreview(session);
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    /**
     * 更新预览
     */
    private void updatePreview(CameraCaptureSession session) {
        try {
            session.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback interface for being notified that a new image is available.
     * <p>
     * <p>
     * The onImageAvailable is called per image basis, that is, callback fires for every new frame
     * available from ImageReader.
     * </p>
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {

            /**
             * 当有一张图片可用时会回调此方法，但有一点一定要注意： 一定要调用
             * reader.acquireNextImage()和close()方法，否则画面就会卡住！！！！！我被这个坑坑了好久！！！
             * 很多人可能写Demo就在这里打一个Log，结果卡住了，或者方法不能一直被回调。
             */
            Image img = reader.acquireNextImage();
            /**
             * 因为Camera2并没有Camera1的Priview回调！！！所以该怎么能到预览图像的byte[]呢？就是在这里了！！！
             * 我找了好久的办法！！！
             */
            /**
             * 因为Camera2并没有Camera1的Priview回调！！！所以该怎么能到预览图像的byte[]呢？就是在这里了！！！
             * 我找了好久的办法！！！
             */
            ByteBuffer buffer = img.getPlanes()[0].getBuffer();
            // 这里就是图片的byte数组了
            byte[] bytes = new byte[buffer.remaining()];

            img.close();
        }
    };


    public void onPause() {

        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    public void onResume() {

        if (cameraManager != null) {
            /**
             * 恢复打开相机
             */
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
