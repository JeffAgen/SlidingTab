package org.xhz.slidingtab;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PagerTitleBar extends HorizontalScrollView {
    private static final String TAG = PagerTitleBar.class.getName();
    private static final int TEXT_SIZE_NOT_SET = -1;
    private static final int TEXT_COLOR_NOT_SET = -1;
    private static int mIndicatorMarginLeft;
    private static final boolean DEBUG = false;

    private LinearLayout mContainer;
    private LayoutInflater mLayoutInflater;
    private ViewPager mViewPager;
    private int mCurrentSelectedPosition;
    private int mCurrentPosition;
    private float mCurrentPositionOffset;
    private float mTitleTextSize;
    private int mTitleTextColor;
    private int mTitleTextColorActive;
    private Paint mSeparatorLinePaint;
    private int mSeparatorLineWidth;
    private Paint mIndicatorLinePaint;
    private boolean mIndicatorFillTab;
    private int mIndicatorLineWidth; //mIndicatorFillTab=false时，mIndicatorLineWidth才有效
    private int mIndicatorLineHeight;
    private boolean mFillViewPoint;
    private Paint mDebugPaint;
    private Integer mClickedItemPosition;

    public PagerTitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PagerTitleBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (0 == mContainer.getChildCount()) {
            return;
        }

        View view = mContainer.getChildAt(mCurrentPosition);
        if (null == view) {
            return;
        }

        // 底部分隔线
        int top = mContainer.getBottom();
        canvas.drawRect(0, top, getIndicatorLineHeight(), top + mSeparatorLineWidth, mSeparatorLinePaint);

        // 初始化所有文字
        for (int i = 0; i < mContainer.getChildCount(); i++) {
            TextView titleTextView = (TextView) mContainer.getChildAt(i).findViewById(R.id.tv_title);
            ImageView imageView = (ImageView) mContainer.getChildAt(i).findViewById(R.id.iv_icon);
            if (titleTextView != null) {
                setTitleTextColor(titleTextView, (i == mCurrentSelectedPosition));
            }
            if (imageView != null) {
                imageView.setSelected(i == mCurrentSelectedPosition);
            }
        }

        // 计算指示条的位置以及宽度
        View currentItem = mContainer.getChildAt(mCurrentPosition);
//        View currentItem = mContainer.getChildAt(mCurrentPosition).findViewById(R.id.tv_title);
        float left = currentItem.getLeft();
        float width = currentItem.getWidth();
        if (!mIndicatorFillTab) {
            left += ((currentItem.getWidth() - mIndicatorLineWidth) / 2);
        }

        if ((mCurrentPositionOffset > 0) && (mCurrentPosition < mContainer.getChildCount() - 1)) {
            View nextItem = mContainer.getChildAt(mCurrentPosition + 1);
            left += (nextItem.getLeft() - currentItem.getLeft()) * mCurrentPositionOffset;
            width += (nextItem.getWidth() - currentItem.getWidth()) * mCurrentPositionOffset;
        }

        // 指示条
        if (!mIndicatorFillTab) {
            canvas.drawRect(left, top, left + mIndicatorLineWidth, top + mIndicatorLineHeight, mIndicatorLinePaint);
        } else {
            canvas.drawRect(left, top, left + width, top + mIndicatorLineHeight, mIndicatorLinePaint);
        }
    }

    public void setViewPager(ViewPager viewPager) {
        mViewPager = viewPager;
        mViewPager.addOnPageChangeListener(new OnViewPageChangeListener());
        notifyDataSetChanged();
    }

    public void notifyDataSetChanged() {
        mContainer.removeAllViews();

        PagerAdapter pagerAdapter = mViewPager.getAdapter();
        if (0 == pagerAdapter.getCount()) {
            return;
        }

        LinearLayout.LayoutParams layoutParams;
        if (mFillViewPoint) {
            layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        } else {
            layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        for (int i = 0; i < pagerAdapter.getCount(); i++) {
            String title;
            CharSequence charSequence = pagerAdapter.getPageTitle(i);

            // =>ADD
            ICustomTab customTab = null;
            if (pagerAdapter instanceof ICustomTab) {
                customTab = (ICustomTab) pagerAdapter;
            }
            // <=ADD

            if (null == charSequence) {
                title = String.valueOf(i);
            } else {
                title = charSequence.toString();
            }

            View view;
            if (customTab == null) {
                view = inflateTabItem(mContainer, title, i);
            } else {
                Drawable icon = customTab.getIcon(i);
                view = inflateCustomTabItem(mContainer, title, icon, i);
            }
            mContainer.addView(view, layoutParams);
        }

        if (mCurrentPosition >= pagerAdapter.getCount()) {
            mCurrentPosition = pagerAdapter.getCount() - 1;
            scrollToChild(mCurrentPosition, 0);
        }
    }

    public LinearLayout getTabContainer() {
        return mContainer;
    }

    private void init(Context context, AttributeSet attributeSet) {
        mLayoutInflater = getLayoutInflater(context);

        mContainer = initContainer(context);

        addView(mContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attributeSet, R.styleable.PagerTitleBar, 0, 0);

        mTitleTextSize = initTextSize(typedArray);
        mTitleTextColor = initTextColor(typedArray);
        mTitleTextColorActive = initActiveTextColor(typedArray);
        mSeparatorLinePaint = initSeparatorLinePaint(typedArray);
        mSeparatorLineWidth = initSeparatorLineWidth(typedArray);
        mIndicatorLinePaint = initIndicatorLinePaint(typedArray);
        mIndicatorLineWidth = initIndicatorLineWidth(typedArray);
        mIndicatorLineHeight = initIndicatorLineHeight(typedArray);
        mIndicatorFillTab = initIndicatorFillTab(typedArray);
        mFillViewPoint = initFillViewPoint(typedArray);
        setFillViewport(mFillViewPoint);
        setHorizontalScrollBarEnabled(false);

        typedArray.recycle();

        int indicatorAreaHeight = Math.max(mSeparatorLineWidth, mIndicatorLineHeight);
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom() + indicatorAreaHeight);

        mIndicatorMarginLeft = getScreenWidth(context) / 5;

        if (DEBUG) {
            mDebugPaint = new Paint();
            mDebugPaint.setColor(0xFFFF0000);
            mDebugPaint.setStyle(Paint.Style.FILL);
        }
    }

    private int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    private LinearLayout initContainer(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        return container;
    }

    private float initTextSize(TypedArray typedArray) {
        int resId = R.styleable.PagerTitleBar_title_text_size;
        if (!typedArray.hasValue(resId)) {
            return TEXT_SIZE_NOT_SET;
        }

        return typedArray.getDimensionPixelSize(resId, TEXT_SIZE_NOT_SET);
    }

    private int initTextColor(TypedArray typedArray) {
        int resId = R.styleable.PagerTitleBar_title_text_color;
        if (!typedArray.hasValue(resId)) {
            return TEXT_COLOR_NOT_SET;
        }

        return typedArray.getColor(resId, TEXT_COLOR_NOT_SET);
    }

    private int initActiveTextColor(TypedArray typedArray) {
        int resId = R.styleable.PagerTitleBar_title_text_color_active;
        if (!typedArray.hasValue(resId)) {
            return TEXT_COLOR_NOT_SET;
        }

        return typedArray.getColor(resId, TEXT_COLOR_NOT_SET);
    }

    private Paint initSeparatorLinePaint(TypedArray typedArray) {
        if (!typedArray.hasValue(R.styleable.PagerTitleBar_separator_line_color)) {
            throw new RuntimeException("R.styleable.TabBar_separator_line_color must be supplied.");
        }

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(typedArray.getColor(R.styleable.PagerTitleBar_separator_line_color, 0xFFE5E5E5));
        return paint;
    }

    private int initSeparatorLineWidth(TypedArray typedArray) {
        final int DEFAULT_WIDTH = 2;
        return typedArray.getDimensionPixelSize(R.styleable.PagerTitleBar_separator_line_width, DEFAULT_WIDTH);
    }

    private Paint initIndicatorLinePaint(TypedArray typedArray) {
        if (!typedArray.hasValue(R.styleable.PagerTitleBar_separator_line_color)) {
            throw new RuntimeException("R.styleable.TabBar_indicator_line_color must be supplied.");
        }

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(typedArray.getColor(R.styleable.PagerTitleBar_indicator_line_color, 0XFF2D7DB7));
        return paint;
    }

    private int initIndicatorLineWidth(TypedArray typedArray) {
        final int DEFAULT_WIDTH = 2;
        return typedArray.getDimensionPixelSize(R.styleable.PagerTitleBar_indicator_line_width, DEFAULT_WIDTH);
    }

    private int initIndicatorLineHeight(TypedArray typedArray) {
        final int DEFAULT_HEIGHT = 2;
        return typedArray.getDimensionPixelSize(R.styleable.PagerTitleBar_indicator_line_height, DEFAULT_HEIGHT);
    }

    private boolean initIndicatorFillTab(TypedArray typedArray) {
        return typedArray.getBoolean(R.styleable.PagerTitleBar_indicator_fill_tab, false);
    }

    private boolean initFillViewPoint(TypedArray typedArray) {
        return typedArray.getBoolean(R.styleable.PagerTitleBar_fill_view_point, false);
    }

    private void scrollToChild(int position, int offset) {
        int scrollX = mContainer.getChildAt(position).getLeft() + offset - mIndicatorMarginLeft;
        scrollTo(scrollX, 0);
    }

    private int getIndicatorLineHeight() {
        int width = 0;
        for (int i = 0; i < mContainer.getChildCount(); i++) {
            width += mContainer.getChildAt(i).getWidth();
        }

        if (width < getWidth()) {
            width = getWidth();
        }

        return width;
    }

    private LayoutInflater getLayoutInflater(Context context) {
        return LayoutInflater.from(context);
    }

    private View inflateCustomTabItem(ViewGroup tabBar, String title, Drawable drawable, int index) {
        View view = mLayoutInflater.inflate(R.layout.item_custom_tab, tabBar, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.iv_icon);
        TextView textView = (TextView) view.findViewById(R.id.tv_title);
        if (drawable != null) {
            imageView.setImageDrawable(drawable);
        }
        setTitleTextColor(textView, false);
        textView.setText(title);
        if (TEXT_SIZE_NOT_SET != mTitleTextSize) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTitleTextSize);
        }
        view.setTag(new CustomTabItemViewHolder(imageView, textView, index));
        view.setOnClickListener(new OnTabItemClickListener());
        return view;
    }

    private View inflateTabItem(ViewGroup tabBar, String title, int index) {
        TextView view = (TextView) mLayoutInflater.inflate(R.layout.item_text_tab, tabBar, false);
        setTitleTextColor(view, false);
        if (TEXT_SIZE_NOT_SET != mTitleTextSize) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTitleTextSize);
        }
        view.setTag(new TabItemViewHolder(view, title, index));
        view.setOnClickListener(new OnTabItemClickListener());
        return view;
    }

    private void setTitleTextColor(TextView view, boolean active) {
        if (active) {
            if (TEXT_COLOR_NOT_SET != mTitleTextColorActive) {
                view.setTextColor(mTitleTextColorActive);
            }
        } else {
            if (TEXT_COLOR_NOT_SET != mTitleTextColor) {
                view.setTextColor(mTitleTextColor);
            }
        }
    }

    private class TabViewHolder {
        int mIndex;
    }

    private class TabItemViewHolder extends TabViewHolder {
        TextView mTitleTextView;

        public TabItemViewHolder(View view, String title, int index) {
            mTitleTextView = (TextView) view;
            mTitleTextView.setText(title);
            mIndex = index;
        }
    }

    private class CustomTabItemViewHolder extends TabViewHolder {
        ImageView mIconImageView;
        TextView mTitleTextView;

        public CustomTabItemViewHolder(ImageView icon, TextView title, int index) {
            mIconImageView = icon;
            mTitleTextView = title;
            mIndex = index;
        }
    }

    private void debugDrawCircle(Canvas canvas, View containerView, View view) {
        canvas.drawCircle(view.getX(), view.getY(), 10, mDebugPaint);
    }

    private class OnTabItemClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            TabViewHolder holder = (TabViewHolder) v.getTag();
            mClickedItemPosition = holder.mIndex;
            mViewPager.setCurrentItem(holder.mIndex);
        }
    }

    private class OnViewPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            mCurrentPosition = position;
            mCurrentPositionOffset = positionOffset;

            if (0 == mContainer.getChildCount()) {
                return;
            }

            View tab = mContainer.getChildAt(position);
            int offset = (int) (positionOffset * tab.getWidth());
            if (DEBUG) Log.d(TAG, "position=" + position + ", positionOffset=" + positionOffset +
                    ", positionOffsetPixels=" + positionOffsetPixels + ", offset=" + offset);

            if (null == mClickedItemPosition) {
                // 非点击触发的滑动（即：用户在viewpager上滑动）。需要同步滑动TabBar。
                scrollToChild(position, offset);
                invalidate();
            } else if ((mClickedItemPosition == position) && (0 == positionOffsetPixels)) {
                // 点击触发的滑动，并且已经滑动到目标page。
                invalidate();
                mClickedItemPosition = null;
            } else {
                // 点击触发的滑动，正在滑动过程中。无需滑动TabBar。
                invalidate();
            }
        }

        @Override
        public void onPageSelected(int position) {
            mCurrentSelectedPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    }

    public interface ICustomTab {
        Drawable getIcon(int position);
    }
}