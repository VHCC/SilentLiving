package com.seetatech.silentliving.mvp;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.seeta.sdk.FaceAntiSpoofing;
import com.seeta.sdk.FaceLandmarker;
import com.seeta.sdk.SeetaDevice;
import com.seeta.sdk.SeetaModelSetting;
import com.seetatech.silentliving.config.AppConfig;
import com.seetatech.silentliving.utils.CachedStatusAndImage;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import  com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaRect;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.FaceDetector;
import com.seeta.sdk.FaceAntiSpoofing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.seetatech.silentliving.AntiSpoofingComponent.MODELPATH;
import static com.seetatech.silentliving.config.AppConfig.CAMERA_PREVIEW_HEIGHT;
import static com.seetatech.silentliving.config.AppConfig.CAMERA_PREVIEW_WIDTH;


public class PresenterImpl implements VerificationContract.Presenter {

    static {
        System.loadLibrary("opencv_java3");
    }
    private static final String TAG = "PresenterImpl";

    private VerificationContract.View mView;

//    public SeetaImageData imageData = new SeetaImageData(WIDTH, HEIGHT, 3); // original
    public SeetaImageData imageData = new SeetaImageData(HEIGHT, WIDTH, 3);
    FaceAntiSpoofing.Status state = FaceAntiSpoofing.Status.DETECTING;//初始状态

    static String fdModel = MODELPATH + "/face_detector.csta";
    static String pdModel = MODELPATH + "/face_landmarker_pts5.csta";
    static String fasModel1 = MODELPATH + "/fas_first.csta";
    static String fasModel2 = MODELPATH + "/fas_second.csta";

    FaceDetector faceDetector = null;
    FaceLandmarker faceLandmarker = null;
    FaceAntiSpoofing faceAntiSpoofing = null;

//    private static int WIDTH = 480;
    private static int WIDTH = CAMERA_PREVIEW_HEIGHT;
//    private static int HEIGHT = 640;
    private static int HEIGHT = CAMERA_PREVIEW_WIDTH;
    public static class TrackingInfo {
        public Mat matBgr;
        public Mat matGray;
        public SeetaRect faceInfo = new SeetaRect();
        public Rect faceRect = new Rect();
        public long birthTime;
        public long lastProccessTime;
    }

    private HandlerThread mFaceTrackThread;
    private HandlerThread mFasThread;

    {
        mFaceTrackThread = new HandlerThread("FaceTrackThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mFasThread = new HandlerThread("FasThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mFaceTrackThread.start();
        mFasThread.start();
    }

    public void SeetaInit()
    {
        try{
			
			if(faceDetector == null || faceLandmarker == null || faceAntiSpoofing == null)
			{
				faceDetector = new FaceDetector(new SeetaModelSetting(0, new String[]{fdModel}, SeetaDevice.SEETA_DEVICE_AUTO));

				faceLandmarker = new FaceLandmarker(new SeetaModelSetting(0, new String[]{pdModel}, SeetaDevice.SEETA_DEVICE_AUTO));

				SeetaModelSetting fasSetting = new SeetaModelSetting(0, new String[]{fasModel1, fasModel2}, SeetaDevice.SEETA_DEVICE_AUTO);
				faceAntiSpoofing = new FaceAntiSpoofing(fasSetting);
				float clarity = 0.30f;
				float fasThresh = 0.80f;
				faceAntiSpoofing.SetThreshold(clarity, fasThresh);
			}
				faceDetector.set(FaceDetector.Property.PROPERTY_MIN_FACE_SIZE, 80);
        }
        catch (Exception e)
        {
            Log.e(TAG, e.toString());
        }
    }
    public PresenterImpl(VerificationContract.View view) {
        mView = view;
        mView.setPresenter(this);
	
        SeetaInit();
    }


    private Handler mFaceTrackingHandler = new Handler(mFaceTrackThread.getLooper()) {

//        private SeetaImageData imageData = new SeetaImageData(WIDTH, HEIGHT, 3);
        private SeetaImageData imageData = new SeetaImageData(HEIGHT, WIDTH, 3);

        @Override
        public void handleMessage(Message msg) {

            final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;


//            trackingInfo.matBgr.create(new Size(WIDTH, HEIGHT), CvType.CV_8UC4);
            //當前Mat與Bitmap轉換，只支援ARGB_8888和RGB_565
            Bitmap bitmap = Bitmap.createBitmap(trackingInfo.matBgr.width(), trackingInfo.matBgr.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(trackingInfo.matBgr, bitmap,true);//新增透明度

            trackingInfo.matBgr.get(0, 0, imageData.data);
            SeetaRect[] faces = faceDetector.Detect(imageData);

            //设一个初始值
            trackingInfo.faceInfo.x = 0;
            trackingInfo.faceInfo.y = 0;
            trackingInfo.faceInfo.width = 0;
            trackingInfo.faceInfo.height = 0;

            if (faces.length != 0 ) {

                int maxIndex = 0;
                double maxWidth = 0;
                for(int i=0;i<faces.length;++i)
                {
                    if(faces[i].width > maxWidth)
                    {
                        maxIndex = i;
                        maxWidth = faces[i].width;
                    }
                }

                trackingInfo.faceInfo = faces[maxIndex];

                trackingInfo.faceRect.x = faces[maxIndex].x;
                trackingInfo.faceRect.y = faces[maxIndex].y;

                trackingInfo.faceRect.width = faces[maxIndex].width;
                trackingInfo.faceRect.height = faces[maxIndex].height;
                trackingInfo.lastProccessTime = System.currentTimeMillis();
                mView.drawFaceRect(trackingInfo.faceRect);
//                mView.drawTestMat(bitmap);
                mFasHandler.removeMessages(0);
                mFasHandler.obtainMessage(0, trackingInfo).sendToTarget();

            } else {
                mView.drawFaceRect(null);

                state = FaceAntiSpoofing.Status.DETECTING;

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mView.setStatus(FaceAntiSpoofing.Status.DETECTING, null, null);
                    }
                });
            }
        }
    };

    static int frameNumThreshold = 5;//超过该帧数后才进行活体识别，过渡摄像头曝光过程
    private Handler mFasHandler = new Handler(mFasThread.getLooper()) {

        @Override
        public void handleMessage(Message msg) {
            final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
            trackingInfo.matGray = new Mat();
            trackingInfo.matBgr.get(0, 0, imageData.data);

             //活体识别
            if(trackingInfo.faceInfo.width != 0)
            {//存在人脸

                //摄像头过渡
                if(CachedStatusAndImage.currentFrameNum < frameNumThreshold)
                {
                    ++CachedStatusAndImage.currentFrameNum;
                    Log.e("frame index", "" + CachedStatusAndImage.currentFrameNum);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            //null为正在检测中的标志
                            mView.setStatus(null, trackingInfo.matBgr, trackingInfo.faceRect);
                        }
                    });

                }
                else
                {
                    //特征点检测
                    SeetaPointF[] points = new SeetaPointF[5];
                    faceLandmarker.mark(imageData, trackingInfo.faceInfo, points);
                    SeetaRect faceInfo = trackingInfo.faceInfo;

                    state = faceAntiSpoofing.Predict(imageData, faceInfo, points);
                    CachedStatusAndImage.status = state;


                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if(state == FaceAntiSpoofing.Status.DETECTING)
                            {//没有人脸
                            }
                            else
                            {
                                if(CachedStatusAndImage.detectedBitmap != null)
                                {
//                            CachedStatusAndImage.detectedBitmap.recycle();
                                }
                                CachedStatusAndImage.detectedBitmap = Bitmap.createBitmap(trackingInfo.matBgr.width(), trackingInfo.matBgr.height(),
                                        Bitmap.Config.ARGB_8888);
                                Mat argb = new Mat();
                                Imgproc.cvtColor(trackingInfo.matBgr, argb, Imgproc.COLOR_BGR2RGBA);
                                Utils.matToBitmap(argb, CachedStatusAndImage.detectedBitmap);
                            }

                            mView.setStatus(state, trackingInfo.matBgr, trackingInfo.faceRect);
                            state = FaceAntiSpoofing.Status.DETECTING;//重置状态
                        }
                    });
                }
            }
            else
            {//没有人脸
                state = FaceAntiSpoofing.Status.DETECTING;
                CachedStatusAndImage.currentFrameNum = 0;
            }

            }
    };

    private Mat matNv21 = new Mat(CAMERA_PREVIEW_HEIGHT + CAMERA_PREVIEW_HEIGHT / 2,
            AppConfig.CAMERA_PREVIEW_WIDTH, CvType.CV_8UC1);

    @Override
    public void detect(byte[] data, int width, int height, int rotation) {
        TrackingInfo trackingInfo = new TrackingInfo();

        Log.d(TAG, "data:> " + data.length + ", matNv21.size:> " + matNv21.size());
        matNv21.put(0, 0, data);
//        trackingInfo.matBgr = new Mat(640, 720, CV_8UC3);
//        trackingInfo.matBgr = new Mat(640, 480, CV_8UC3);
        trackingInfo.matBgr = new Mat();
        trackingInfo.matGray = new Mat();

        Imgproc.cvtColor(matNv21, trackingInfo.matBgr, Imgproc.COLOR_YUV2BGR_NV21);
        Core.transpose(trackingInfo.matBgr, trackingInfo.matBgr);
//        Log.d(TAG, "CachedStatusAndImage.frontCamera:> " + CachedStatusAndImage.frontCamera);
        if (CachedStatusAndImage.frontCamera) {//如果是前向摄像头
            Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 0);
            Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 1);


//            double ratio =  trackingInfo.matBgr.height() / (double) trackingInfo.matBgr.width();
//
//            int rotatedHeight = trackingInfo.matBgr.height();
//            int rotatedWidth  = (int) Math.round(trackingInfo.matBgr.height() * ratio);

//        Log.d(TAG, "rotatedHeight:> " + rotatedHeight + ", rotatedWidth:> " + rotatedWidth);

//            Imgproc.resize(trackingInfo.matBgr, trackingInfo.matBgr, new Size(rotatedHeight, rotatedWidth));

//            # rotate 90º counter-clockwise
            Log.d(TAG, "trackingInfo.matBgr:> " + trackingInfo.matBgr.size());
//            Core.flip(trackingInfo.matBgr.t(), trackingInfo.matBgr, 0); //mRgba.t() is the transpose
            Core.flip(trackingInfo.matBgr.t(), trackingInfo.matBgr, 0); //mRgba.t() is the transpose
            Log.d(TAG, "after t, trackingInfo.matBgr:> " + trackingInfo.matBgr.size());
//            # rotate 90º clockwise
//            Core.flip(trackingInfo.matBgr.t(), trackingInfo.matBgr, 1);
        } else {//如果是后置摄像头
            Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 1);
        }

//        saveImgage(trackingInfo.matBgr.clone());//保存图像

        Imgproc.cvtColor(trackingInfo.matBgr, trackingInfo.matGray, Imgproc.COLOR_BGR2GRAY);

        trackingInfo.birthTime = System.currentTimeMillis();
        trackingInfo.lastProccessTime = System.currentTimeMillis();

        mFaceTrackingHandler.removeMessages(1);
        mFaceTrackingHandler.obtainMessage(1, trackingInfo).sendToTarget();
    }

    private void rotationTutorial(Mat mRgba){
        Mat mIntermediateMat = new Mat();
        double ratio =  mRgba.height() / (double) mRgba.width();

        int rotatedHeight = mRgba.height();
        int rotatedWidth  = (int) Math.round(mRgba.height() * ratio);

//        Log.d(TAG, "rotatedHeight:> " + rotatedHeight + ", rotatedWidth:> " + rotatedWidth);

        Imgproc.resize(mRgba, mIntermediateMat, new Size(rotatedHeight, rotatedWidth));

        Core.flip(mIntermediateMat.t(), mIntermediateMat, 0);

        Mat ROI = mRgba.submat(0, mIntermediateMat.rows(), 0, mIntermediateMat.cols());

        mIntermediateMat.copyTo(ROI);
    }



    public void saveImgage(Mat rgba)
    {
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_BGR2RGBA);

        Bitmap mBitmap = null;
        mBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, mBitmap);

        Log.e(TAG, "保存图片");
        File f = new File("/sdcard/seeta/", "test.jpg");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            Log.i(TAG, "已经保存");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    @Override
    public void destroy() {
        mFaceTrackThread.quitSafely();
        mFasThread.quitSafely();
    }

}
