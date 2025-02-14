package com.zj.album.graphy.views.gestures_view.frescoUtils.libs;

import android.content.Context;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;
import com.zj.album.graphy.views.gestures_view.frescoUtils.libs.zoomable.ZoomableDraweeView;

public class FrescoZoomImageView extends ZoomableDraweeView implements FrescoController, BaseFrescoImageView {

    private String mThumbnailUrl = null;

    private String mLowThumbnailUrl = null;//低分辨率Url

    private int mDefaultResID = 0;

    private ImageRequest mRequest;

    private String mThumbnailPath = null;

    private boolean mAnim = true;//默认开启动画

    private ImageRequest mLowResRequest;

    private ControllerListener mControllerListener;

    private Postprocessor mPostProcessor;

    private DraweeController mController;

    private boolean mTapToRetry = false;

    private boolean mAutoRotateEnabled = false;

    private boolean resize = false;

    private Point reSize;

    public FrescoZoomImageView(Context context) {
        this(context, null);
    }

    public FrescoZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrescoZoomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void setResourceController() {

        mRequest = (isResize()) ? FrescoFactory.buildImageRequestWithResource(this, reSize) :
                FrescoFactory.buildImageRequestWithResource(this);

        mController = FrescoFactory.buildDraweeController(this);

        this.setController(mController);
    }

    private void setSourceController() {

        mRequest = (isResize()) ? FrescoFactory.buildImageRequestWithSource(this, reSize) :
                FrescoFactory.buildImageRequestWithSource(this);

        mLowResRequest = FrescoFactory.buildLowImageRequest(this);

        mController = FrescoFactory.buildDraweeController(this);

        this.setController(mController);
    }

    @Override
    public int getDefaultResID() {
        return this.mDefaultResID;
    }

    @Override
    public void loadView(String lowUrl, String url, int defaultResID) {
        try {
            mThumbnailPath = null;
            mThumbnailUrl = url;
            mLowThumbnailUrl = url;
            mDefaultResID = defaultResID;
            if (!TextUtils.isEmpty(mThumbnailUrl)
                    && (mThumbnailUrl.startsWith(FrescoController.HTTP_PERFIX)
                    || mThumbnailUrl.startsWith(FrescoController.HTTPS_PERFIX))) {

                this.getHierarchy().setPlaceholderImage(defaultResID);

                this.setSourceController();

                return;
            }

            this.getHierarchy().setPlaceholderImage(defaultResID);
            this.setResourceController();

        } catch (OutOfMemoryError ignore) {
        }
    }

    @Override
    public void loadView(String url, int defaultResID) {
        this.loadView(null, url, defaultResID);
    }

    @Override
    public void loadLocalImage(String path, int defaultRes) {
        try {
            mThumbnailPath = path;
            mDefaultResID = defaultRes;
            mThumbnailUrl = null;
            mLowThumbnailUrl = null;

            this.getHierarchy().setPlaceholderImage(mDefaultResID);

            if (TextUtils.isEmpty(mThumbnailPath)) {
                this.setResourceController();
                return;
            }
            if (!mThumbnailPath.startsWith(FrescoController.FILE_PERFIX)) {
                mThumbnailPath = FrescoController.FILE_PERFIX + mThumbnailPath;
            }
            this.setSourceController();
        } catch (OutOfMemoryError ignore) {
        }
    }

    @Override
    public Postprocessor getPostProcessor() {
        return this.mPostProcessor;
    }

    @Override
    public void setPostProcessor(Postprocessor postProcessor) {
        this.mPostProcessor = postProcessor;
    }

    @Override
    public String getThumbnailUrl() {
        return this.mThumbnailUrl;
    }

    @Override
    public String getLowThumbnailUrl() {
        return this.mLowThumbnailUrl;
    }

    @Override
    public String getThumbnailPath() {
        return this.mThumbnailPath;
    }

    @Override
    public boolean getTapToRetryEnabled() {
        return this.mTapToRetry;
    }

    @Override
    public boolean getAutoRotateEnabled() {
        return this.mAutoRotateEnabled;
    }

    @Override
    public void asCircle() {
        setRoundingParmas(getRoundingParams().setRoundAsCircle(true));
    }

    @Override
    public void setBorder(int color, float width) {
        setRoundingParmas(getRoundingParams().setBorder(color, width));
    }

    @Override
    public void clearRoundingParams() {
        setRoundingParmas(null);
    }

    @Override
    public ControllerListener getControllerListener() {
        return this.mControllerListener;
    }

    @Override
    public DraweeController getDraweeController() {
        return this.getController();
    }

    @Override
    public ImageRequest getLowImageRequest() {
        return this.mLowResRequest;
    }

    @Override
    public ImageRequest getImageRequest() {
        return this.mRequest;
    }

    @Override
    public RoundingParams getRoundingParams() {
        RoundingParams roundingParams = this.getHierarchy().getRoundingParams();
        if (roundingParams == null) {
            roundingParams = new RoundingParams();
        }
        return roundingParams;
    }

    @Override
    public void setRoundingParmas(RoundingParams roundingParmas) {
        this.getHierarchy().setRoundingParams(roundingParmas);
    }

    @Override
    public void setControllerListener(ControllerListener controllerListener) {
        this.mControllerListener = controllerListener;
    }

    @Override
    public void setCircle(int overlay_color) {
        setRoundingParmas(getRoundingParams().setRoundAsCircle(true).
                setRoundingMethod(RoundingParams.RoundingMethod.OVERLAY_COLOR).
                setOverlayColor(overlay_color));
    }

    @Override
    public void setCornerRadius(float radius) {
        setRoundingParmas(getRoundingParams().setCornersRadius(radius));
    }

    @Override
    public void setCornerRadius(float radius, int overlay_color) {
        setRoundingParmas(getRoundingParams().setCornersRadius(radius).
                setRoundingMethod(RoundingParams.RoundingMethod.OVERLAY_COLOR).
                setOverlayColor(overlay_color));
    }

    @Override
    public boolean isAnim() {
        return mAnim;
    }

    @Override
    public void setAnim(boolean anim) {
        mAnim = anim;
    }

    @Override
    public void setTapToRetryEnabled(boolean tapToRetryEnabled) {
        this.mTapToRetry = tapToRetryEnabled;
    }

    @Override
    public void setAutoRotateEnabled(boolean autoRotateEnabled) {
        this.mAutoRotateEnabled = autoRotateEnabled;
    }

    @Override
    public void setActualImageScaleType(ScalingUtils.ScaleType scaleType) {
        this.getHierarchy().setActualImageScaleType(scaleType);
    }

    @Override
    public void setFadeTime(int time) {
        this.getHierarchy().setFadeDuration(time);
    }

    public boolean isResize() {
        return resize;
    }

    public void setResize(Point resize) {
        this.resize = true;
        this.reSize = resize;
    }
}
