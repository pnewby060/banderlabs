package bander.taskman;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

/** Utility <code>ViewBinder</code> that can bind a 
  * <code>Drawable</code> to an <code>ImageView</code>.
  */
public class DrawableViewBinder implements SimpleAdapter.ViewBinder {

	// ViewBinder
	public boolean setViewValue(View view, Object data, String textRepresentation) {
		if (view instanceof ImageView) {
			ImageView imageView = (ImageView) view;
			imageView.setImageDrawable((Drawable) data); 
			return true;
		}
		return false;
	}

}
