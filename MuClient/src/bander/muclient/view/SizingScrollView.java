package bander.muclient.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class SizingScrollView extends ScrollView {
	
	/** Interface definition for a callback to be invoked when the size of this view has changed. */
	public interface OnSizeListener {
		/** Called when the size of this view has changed. 
		 * @param w Current width of this view.
		 * @param h Current height of this view.
		 * @param oldw Old width of this view.
		 * @param oldh Old height of this view.
		 */
		abstract void onSizeChanged(int w, int h, int oldw, int oldh);
	}
	
	private OnSizeListener	mSizeListener	= null;
	
	public SizingScrollView(Context context) {
		super(context);
	}
	public SizingScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public SizingScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	/** Register a callback to be invoked when the size of this view has changed.
	 * @param listener The listener to attach to this view. 
	 */
	public void setOnSizeListener(OnSizeListener listener) {
		mSizeListener = listener;
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (mSizeListener != null) 
			mSizeListener.onSizeChanged(w, h, oldw, oldh);
	}

}
