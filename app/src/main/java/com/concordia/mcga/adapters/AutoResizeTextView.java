package com.concordia.mcga.adapters;


import android.annotation.TargetApi;
        import android.content.Context;
        import android.content.res.Resources;
        import android.graphics.RectF;
        import android.os.Build;
        import android.text.Layout.Alignment;
        import android.text.StaticLayout;
        import android.text.TextPaint;
        import android.util.AttributeSet;
    import android.util.SparseIntArray;
        import android.util.TypedValue;
        import android.widget.TextView;

public class AutoResizeTextView extends TextView {
    private interface SizeTester {
        /**
         *
         * @param suggestedSize
         *            Size of text to be tested
         * @param availableSpace
         *            available space in which text must fit
         * @return an integer < 0 if after applying {@code suggestedSize} to
         *         text, it takes less space than {@code availableSpace}, > 0
         *         otherwise
         */
        public int onTestSize(int suggestedSize, RectF availableSpace);
    }

    private RectF mTextRect = new RectF();

    private RectF mAvailableSpaceRect;

    private SparseIntArray mTextCachedSizes;

    private TextPaint mPaint;

    private float mMaxTextSize;

    private float mSpacingMult = 1.0f;

    private float mSpacingAdd = 0.0f;

    private float mMinTextSize = 20;

    private int mWidthLimit;

    private static final int NO_LINE_LIMIT = -1;
    private int mMaxLines;

    private boolean mEnableSizeCache = true;
    private boolean mInitiallized;

    /**
     * Constructor
     * @param context
     */
    public AutoResizeTextView(Context context) {
        super(context);
        initialize();
    }

    /**
     * Constructor
     * @param context
     * @param attrs attributes
     */
    public AutoResizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    /**
     * Constructor
     * @param context
     * @param attrs attributes
     * @param defStyle custom style
     */
    public AutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    /**
     * Initialize default text box paramaters
     */
    private void initialize() {
        mPaint = new TextPaint(getPaint());
        mMaxTextSize = getTextSize();
        mAvailableSpaceRect = new RectF();
        mTextCachedSizes = new SparseIntArray();
        if (mMaxLines == 0) {
            // no value was assigned during construction
            mMaxLines = NO_LINE_LIMIT;
        }
        mInitiallized = true;
    }

    /**
     * Override the method to write text
     * @param text represented as a string
     * @param type
     */
    @Override
    public void setText(final CharSequence text, BufferType type) {
        super.setText(text, type);
        adjustTextSize(text.toString());
    }

    /**
     * Overrite text Size method
     * @param size
     */
    @Override
    public void setTextSize(float size) {
        mMaxTextSize = size;
        mTextCachedSizes.clear();
        adjustTextSize(getText().toString());
    }

    /**
     * Sets maximum amount of lines possible in the textbox
     * @param maxlines
     */
    @Override
    public void setMaxLines(int maxlines) {
        super.setMaxLines(maxlines);
        mMaxLines = maxlines;
        reAdjust();
    }

    /**
     * @return maximum amount of lines a textbox can have
     */
    public int getMaxLines() {
        return mMaxLines;
    }

    /**
     * Sets maximum amount of lines to 1
     */
    @Override
    public void setSingleLine() {
        super.setSingleLine();
        mMaxLines = 1;
        reAdjust();
    }

    /**
     * Setting this to false will allow for an infinite amount of lines in a textbox
     * @param singleLine
     */
    @Override
    public void setSingleLine(boolean singleLine) {
        super.setSingleLine(singleLine);
        if (singleLine) {
            mMaxLines = 1;
        } else {
            mMaxLines = NO_LINE_LIMIT;
        }
        reAdjust();
    }

    /**
     * Sets user defined amount of lines per text box
     * @param lines
     */
    @Override
    public void setLines(int lines) {
        super.setLines(lines);
        mMaxLines = lines;
        reAdjust();
    }

    /**
     * Sets size of text
     * @param unit
     * @param size
     */
    @Override
    public void setTextSize(int unit, float size) {
        Context context = getContext();
        Resources resources;

        if (context == null)
            resources = Resources.getSystem();
        else
            resources = context.getResources();
        mMaxTextSize = TypedValue.applyDimension(unit, size,
                resources.getDisplayMetrics());
        mTextCachedSizes.clear();
        adjustTextSize(getText().toString());
    }

    /**
     * Sets spacing between lines
     * @param add
     * @param mult
     */
    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        mSpacingMult = mult;
        mSpacingAdd = add;
    }

    /**
     * Set the lower text size limit and invalidate the view
     *
     * @param minTextSize
     */
    public void setMinTextSize(float minTextSize) {
        mMinTextSize = minTextSize;
        reAdjust();
    }

    /**
     * Readjust dynamically the text using an element of type Text and converting it to String
     */
    private void reAdjust() {
        adjustTextSize(getText().toString());
    }


    /**
     * Adjust textSize depending on string length
     * @param string
     */
    private void adjustTextSize(String string) {
        if (!mInitiallized) {
            return;
        }

        // text begins at the maximum size
        int startSize = (int) mMinTextSize;
        int heightLimit = getMeasuredHeight() - getCompoundPaddingBottom() - getCompoundPaddingTop();
        mWidthLimit = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();

        // Get the limits on arbituary point. --> bottom right of rectangle
        mAvailableSpaceRect.right = mWidthLimit;
        mAvailableSpaceRect.bottom = heightLimit;

        // Text
        // Best Text size
        super.setTextSize(TypedValue.COMPLEX_UNIT_PX, efficientTextSizeSearch(startSize, (int) mMaxTextSize, mSizeTester, mAvailableSpaceRect));
    }

    /**
     * Method only used for testing, the verify that the Size algorithm works*
     * @param suggestedSize
     * @param availableSPace
     * @return -1 for to small and 1 for too big
     */
    public int getmSizeTester(int suggestedSize, RectF availableSPace){
        return mSizeTester.onTestSize(suggestedSize, availableSPace);
    }

    /**
     * Tests to see if size is ok
     * The binary search uses this method repeatedly
     */
    private final SizeTester mSizeTester = new SizeTester() {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public int onTestSize(int suggestedSize, RectF availableSPace) {
            mPaint.setTextSize(suggestedSize);
            String text = getText().toString();
            boolean singleline = getMaxLines() == 1;
            if (singleline) {
                mTextRect.bottom = mPaint.getFontSpacing();
                mTextRect.right = mPaint.measureText(text);
            } else {
                StaticLayout layout = new StaticLayout(text, mPaint,
                        mWidthLimit, Alignment.ALIGN_NORMAL, mSpacingMult,
                        mSpacingAdd, true);
                // return early if we have more lines
                if (getMaxLines() != NO_LINE_LIMIT
                        && layout.getLineCount() > getMaxLines()) {
                    return 1;
                }
                mTextRect.bottom = layout.getHeight();
                int maxWidth = -1;
                for (int i = 0; i < layout.getLineCount(); i++) {
                    if (maxWidth < layout.getLineWidth(i)) {
                        maxWidth = (int) layout.getLineWidth(i);
                    }
                }
                mTextRect.right = maxWidth;
            }

            mTextRect.offsetTo(0, 0);
            if (availableSPace.contains(mTextRect)) {
                // may be too small, don't worry we will find the best match
                return -1;
            } else {
                // too big
                return 1;
            }
        }
    };


    /**
     * Uses binary search to find most optimal text size
     * It will return the biggest fittable text size
     * @param start Starting size of text --> biggest one
     * @param end End size of text --> starts with biggest one and gets reduced through binary search
     * @param sizeTester
     * @param availableSpace
     * @return
     */
    private int efficientTextSizeSearch(int start, int end, SizeTester sizeTester, RectF availableSpace) {
        if (!mEnableSizeCache) {
            return binarySearch(start, end, sizeTester, availableSpace);
        }
        String text = getText().toString();
        int key = text == null ? 0 : text.length();
        int size = mTextCachedSizes.get(key);
        if (size != 0) {
            return size;
        }
        size = binarySearch(start, end, sizeTester, availableSpace);
        mTextCachedSizes.put(key, size);
        return size;
    }

    /**
     * Binary search verifies best text size
     * @param start
     * @param end
     * @param sizeTester
     * @param availableSpace
     * @return
     */
    private static int binarySearch(int start, int end, SizeTester sizeTester, RectF availableSpace) {
        int lastBest = start;
        int lo = start;
        int hi = end - 1;
        int mid = 0;

        // Binary search
        while (lo <= hi) {
            // >>> is a bitshift operator
            // Returns correct answer in Log(N) time
            mid = (lo + hi) >>> 1;
            int midValCmp = sizeTester.onTestSize(mid, availableSpace);
            if (midValCmp < 0) {
                lastBest = lo;
                lo = mid + 1;
            } else if (midValCmp > 0) {
                hi = mid - 1;
                lastBest = hi;
            } else {
                return mid;
            }
        }
        // make sure to return last best
        // this is what should always be returned
        return lastBest;

    }

    /**
     * Override textChanged listener
     * @param text
     * @param start
     * @param before
     * @param after
     */
    @Override
    protected void onTextChanged(final CharSequence text, final int start,
                                 final int before, final int after) {
        super.onTextChanged(text, start, before, after);
        reAdjust();
    }

    /**
     * Override size changed listener
     * @param width
     * @param height
     * @param oldwidth
     * @param oldheight
     */
    @Override
    protected void onSizeChanged(int width, int height, int oldwidth,
                                 int oldheight) {
        mTextCachedSizes.clear();
        super.onSizeChanged(width, height, oldwidth, oldheight);
        if (width != oldwidth || height != oldheight) {
            reAdjust();
        }
    }

}