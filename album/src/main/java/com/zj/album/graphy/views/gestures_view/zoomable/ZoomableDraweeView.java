package com.zj.album.graphy.views.gestures_view.zoomable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.support.annotation.Nullable;
import android.support.v4.view.ScrollingView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.facebook.common.internal.Preconditions;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeView;

import java.lang.ref.WeakReference;


@SuppressWarnings("unchecked")
public class ZoomableDraweeView extends DraweeView<GenericDraweeHierarchy>
        implements ScrollingView {


    private static final float HUGE_IMAGE_SCALE_FACTOR_THRESHOLD = 1.1f;
    private static final boolean DEFAULT_ALLOW_TOUCH_INTERCEPTION_WHILE_ZOOMED = true;

    private boolean mUseSimpleTouchHandling = false;
    private WeakReference<Context> ctx;
    private final RectF mImageBounds = new RectF();
    private final RectF mViewBounds = new RectF();

    private DraweeController mHugeImageController;
    private ZoomableController mZoomableController;
    private GestureDetector mTapGestureDetector;
    private boolean mAllowTouchInterceptionWhileZoomed =
            DEFAULT_ALLOW_TOUCH_INTERCEPTION_WHILE_ZOOMED;

    private final ControllerListener mControllerListener = new BaseControllerListener<Object>() {
        @Override
        public void onFinalImageSet(
                String id,
                @Nullable Object imageInfo,
                @Nullable Animatable animatable) {
            ZoomableDraweeView.this.onFinalImageSet();
        }

        @Override
        public void onRelease(String id) {
            ZoomableDraweeView.this.onRelease();
        }
    };

    private final ZoomableController.Listener mZoomableListener = new ZoomableController.Listener() {
        @Override
        public void onTransformChanged(Matrix transform) {
            ZoomableDraweeView.this.onTransformChanged(transform);
        }
    };

    private final GestureListenerWrapper mTapListenerWrapper = new GestureListenerWrapper();

    public ZoomableDraweeView(Context context, GenericDraweeHierarchy hierarchy) {
        super(new WeakReference<>(context).get());
        this.ctx = new WeakReference<>(context);
        setHierarchy(hierarchy);
        init();
    }

    public ZoomableDraweeView(Context context) {
        super(new WeakReference<>(context).get());
        this.ctx = new WeakReference<>(context);
        inflateHierarchy(null);
        init();
    }

    public ZoomableDraweeView(Context context, AttributeSet attrs) {
        super(new WeakReference<>(context).get(), attrs);
        this.ctx = new WeakReference<>(context);
        inflateHierarchy(attrs);
        init();
    }

    public ZoomableDraweeView(Context context, AttributeSet attrs, int defStyle) {
        super(new WeakReference<>(context).get(), attrs, defStyle);
        this.ctx = new WeakReference<>(context);
        inflateHierarchy(attrs);
        init();
    }

    protected void inflateHierarchy(@Nullable AttributeSet attrs) {
        Resources resources = ctx.get().getResources();
        GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(resources)
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
        GenericDraweeHierarchyInflater.updateBuilder(builder, ctx.get(), attrs);
        setAspectRatio(builder.getDesiredAspectRatio());
        setHierarchy(builder.build());
    }

    private void init() {
        mZoomableController = createZoomableController();
        mZoomableController.setListener(mZoomableListener);
        mTapGestureDetector = new GestureDetector(ctx.get(), mTapListenerWrapper);
    }

    /**
     * Gets the original image bounds, in view-absolute coordinates.
     * <p>
     * <p> The original image bounds are those reported by the hierarchy. The hierarchy itself may
     * apply scaling on its own (e.g. due to scale type) so the reported bounds are not necessarily
     * the same as the actual bitmap dimensions. In other words, the original image bounds correspond
     * to the image bounds within this view when no zoomable transformation is applied, but including
     * the potential scaling of the hierarchy.
     * Having the actual bitmap dimensions abstracted away from this view greatly simplifies
     * implementation because the actual bitmap may change (e.g. when a high-res image arrives and
     * replaces the previously set low-res image). With proper hierarchy scaling (e.g. FIT_CENTER),
     * this underlying change will not affect this view nor the zoomable transformation in any way.
     */
    protected void getImageBounds(RectF outBounds) {
        getHierarchy().getActualImageBounds(outBounds);
    }

    /**
     * Gets the bounds used to limit the translation, in view-absolute coordinates.
     * <p>
     * <p> These bounds are passed to the zoomable controller in order to limit the translation. The
     * image is attempted to be centered within the limit bounds if the transformed image is smaller.
     * There will be no empty spaces within the limit bounds if the transformed image is bigger.
     * This applies to each dimension (horizontal and vertical) independently.
     * <p> Unless overridden by a subclass, these bounds are same as the view bounds.
     */
    protected void getLimitBounds(RectF outBounds) {
        outBounds.set(0, 0, getWidth(), getHeight());
    }

    /**
     * Sets a custom zoomable controller, instead of using the default one.
     */
    public void setZoomableController(ZoomableController zoomableController) {
        Preconditions.checkNotNull(zoomableController);
        mZoomableController.setListener(null);
        mZoomableController = zoomableController;
        mZoomableController.setListener(mZoomableListener);
    }

    /**
     * Gets the zoomable controller.
     * <p>
     * <p> Zoomable controller can be used to zoom to point, or to map point from view to image
     * coordinates for instance.
     */
    public ZoomableController getZoomableController() {
        return mZoomableController;
    }

    /**
     * Check whether the parent view can intercept touch events while zoomed.
     * This can be used, for example, to swipe between images in a view pager while zoomed.
     *
     * @return true if touch events can be intercepted
     */
    public boolean allowsTouchInterceptionWhileZoomed() {
        return mAllowTouchInterceptionWhileZoomed;
    }

    /**
     * If this is set to true, parent views can intercept touch events while the view is zoomed.
     * For example, this can be used to swipe between images in a view pager while zoomed.
     *
     * @param allowTouchInterceptionWhileZoomed true if the parent needs to intercept touches
     */
    public void setAllowTouchInterceptionWhileZoomed(
            boolean allowTouchInterceptionWhileZoomed) {
        mAllowTouchInterceptionWhileZoomed = allowTouchInterceptionWhileZoomed;
    }

    /**
     * Sets the tap listener.
     */
    public void setTapListener(GestureDetector.SimpleOnGestureListener tapListener) {
        mTapListenerWrapper.setListener(tapListener);
    }

    /**
     * Sets whether long-press tap detection is enabled.
     * Unfortunately, long-press conflicts with onDoubleTapEvent.
     */
    public void setIsLongpressEnabled(boolean enabled) {
        mTapGestureDetector.setIsLongpressEnabled(enabled);
    }

    /**
     * Sets the image controller.
     */
    @Override
    public void setController(@Nullable DraweeController controller) {
        setControllers(controller, null);
    }

    /**
     * Sets the controllers for the normal and huge image.
     * <p>
     * <p> The huge image controller is used after the image gets scaled above a certain threshold.
     * <p>
     * <p> IMPORTANT: in order to avoid a flicker when switching to the huge image, the huge image
     * controller should have the normal-image-uri set as its low-res-uri.
     *
     * @param controller          controller to be initially used
     * @param hugeImageController controller to be used after the client starts zooming-in
     */
    public void setControllers(
            @Nullable DraweeController controller,
            @Nullable DraweeController hugeImageController) {
        setControllersInternal(null, null);
        mZoomableController.setEnabled(false);
        setControllersInternal(controller, hugeImageController);
    }

    private void setControllersInternal(
            @Nullable DraweeController controller,
            @Nullable DraweeController hugeImageController) {
        removeControllerListener(getController());
        addControllerListener(controller);
        mHugeImageController = hugeImageController;
        super.setController(controller);
    }

    private void maybeSetHugeImageController() {
        if (mHugeImageController != null &&
                mZoomableController.getScaleFactor() > HUGE_IMAGE_SCALE_FACTOR_THRESHOLD) {
            setControllersInternal(mHugeImageController, null);
        }
    }

    private void removeControllerListener(DraweeController controller) {
        if (controller instanceof AbstractDraweeController) {
            ((AbstractDraweeController) controller).removeControllerListener(mControllerListener);
        }
    }

    private void addControllerListener(DraweeController controller) {
        if (controller instanceof AbstractDraweeController) {
            ((AbstractDraweeController) controller)
                    .addControllerListener(mControllerListener);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int saveCount = canvas.save();
        canvas.concat(mZoomableController.getTransform());
        super.onDraw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int a = event.getActionMasked();
        if (mTapGestureDetector.onTouchEvent(event)) {
            return true;
        }

        if (mUseSimpleTouchHandling) {
            if (mZoomableController.onTouchEvent(event)) {
                return true;
            }
        } else if (mZoomableController.onTouchEvent(event)) {
            // Do not allow the parent to intercept touch events if:
            // - we do not allow swiping while zoomed and the image is zoomed
            // - we allow swiping while zoomed and the transform was corrected
            if ((!mAllowTouchInterceptionWhileZoomed && !mZoomableController.isIdentity()) ||
                    (!mAllowTouchInterceptionWhileZoomed && !mZoomableController.wasTransformCorrected())) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;
        }
        if (super.onTouchEvent(event)) {
            return true;
        }
        // None of our components reported that they handled the touch event. Upon returning false
        // from this method, our parent won't send us any more events for this gesture. Unfortunately,
        // some componentes may have started a delayed action, such as a long-press timer, and since we
        // won't receive an ACTION_UP that would cancel that timer, a false event may be triggered.
        // To prevent that we explicitly send one last cancel event when returning false.
        MotionEvent cancelEvent = MotionEvent.obtain(event);
        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
        mTapGestureDetector.onTouchEvent(cancelEvent);
        mZoomableController.onTouchEvent(cancelEvent);
        cancelEvent.recycle();
        return false;
    }

    @Override
    public int computeHorizontalScrollRange() {
        return mZoomableController.computeHorizontalScrollRange();
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return mZoomableController.computeHorizontalScrollOffset();
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return mZoomableController.computeHorizontalScrollExtent();
    }

    @Override
    public int computeVerticalScrollRange() {
        return mZoomableController.computeVerticalScrollRange();
    }

    @Override
    public int computeVerticalScrollOffset() {
        return mZoomableController.computeVerticalScrollOffset();
    }

    @Override
    public int computeVerticalScrollExtent() {
        return mZoomableController.computeVerticalScrollExtent();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateZoomableControllerBounds();
    }

    private void onFinalImageSet() {
        if (!mZoomableController.isEnabled()) {
            updateZoomableControllerBounds();
            mZoomableController.setEnabled(true);
        }
    }

    private void onRelease() {
        mZoomableController.setEnabled(false);
    }

    protected void onTransformChanged(Matrix transform) {
        maybeSetHugeImageController();
        invalidate();
    }

    protected void updateZoomableControllerBounds() {
        getImageBounds(mImageBounds);
        getLimitBounds(mViewBounds);
        if (mZoomableController != null && mZoomableController != null) {
            mZoomableController.setImageBounds(mImageBounds);
            mZoomableController.setViewBounds(mViewBounds);
        }
    }

    protected ZoomableController createZoomableController() {
        return AnimatedZoomableController.newInstance();
    }

    public void setExperimentalSimpleTouchHandlingEnabled(boolean enabled) {
        mUseSimpleTouchHandling = enabled;
    }
}
