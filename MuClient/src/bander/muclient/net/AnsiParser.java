package bander.muclient.net;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

public class AnsiParser {
	private ForegroundColorSpan mForegroundColorSpan = new ForegroundColorSpan(Color.WHITE);
	private BackgroundColorSpan mBackgroundColorSpan = new BackgroundColorSpan(Color.BLACK);
	private int mIntensity = 0;
	
	/** The result of searching for ANSI escape sequences in a given text input.*/
	private class AnsiMatcher {
		private static final int STATE_CSI1		= 1;
		private static final int STATE_CSI2 	= 2;
		private static final int STATE_PARAM	= 3;
		private static final int STATE_COMMAND	= 6;
		private static final int STATE_RESET	= 7;
		
		private int mState			= STATE_CSI1;
		
		private String mText		= "";
		private int	mPos			= 0;
		private int mStart			= 0;
		private int mEnd			= 0;
		
		private char mCommand		= ' ';
		private String mParam		= "";
		
		private int[] mParams		= new int[3];
		private int mParamIndex		= -1;
		
		/** Returns the input text. */
		public String text() 		{ return mText; }
		/** Returns the index of the first character of the ANSI escape sequence. */
		public int start() 			{ return mStart; }
		/** Returns the index of the first character following the ANSI escape sequence. */
		public int end() 			{ return mEnd; }
		/** Returns the ANSI command identifier. */
		public char command() 		{ return mCommand; }
		/** Returns a given parameter. */
		public int param(int i) 	{ return mParams[i]; }
		/** Returns the amount of parameters in the ANSI escape sequence. */
		public int paramCount() 	{ return mParamIndex+1; }
		
		/** Sets the text to be used as input.
		 * @param text The text to be used as input.
		 */
		public void match(String text) {
			mText	= text;
			mPos 	= 0;
			mStart	= 0;
			mEnd	= 0;
		}
		
		/** Returns the next occurrence of an ANSI escape sequence in the input.
		 * @return true if an escape sequence has been found. 
		 */
		public boolean find() {
			while (mPos < mText.length()) {
				switch (mState) {
					case STATE_CSI1:
						mPos = mText.indexOf(0x001B, mPos);
						if (mPos != -1) { 
							mState = STATE_CSI2;  mStart = mPos;  mPos++;
						} else { mPos = mText.length(); }
						break;
					case STATE_CSI2:
						if (mText.charAt(mPos) == '[') {
							mState = STATE_PARAM;  mPos++;
						} else { mState = STATE_RESET; }
						break;
					case STATE_PARAM:
						if ((mText.charAt(mPos) >= '0') && (mText.charAt(mPos) <= '9')) {
							mParam = mParam + mText.charAt(mPos); 
							mPos++;
						} else { 
							mParamIndex++;
							if (mParamIndex < 3) {
								mParams[mParamIndex] = Integer.parseInt(mParam);
							}
							mParam = ""; 
							
							if (mText.charAt(mPos) == ';') {
								mPos++;
							} else { mState = STATE_COMMAND; }
						}
						break;
					case STATE_COMMAND:
						mCommand = mText.charAt(mPos);
						mPos++; mEnd = mPos;
						mState = STATE_RESET;
						return true;
					case STATE_RESET:
						mParam = "";  mParamIndex = -1;
						mState = STATE_CSI1;
						break;
				}
			}
			return false;
		}
		
		/** Returns whether a partial ANSI escape sequence has been detected.
		 * @return true if a partial escape sequence has been found.
		 */
		public boolean foundPartial() {
			return (mState != STATE_CSI1);
		}
		
	}
	private AnsiMatcher mMatcher = new AnsiMatcher();
	
	/** Scans the provided text and turns all occurrences of ANSI color codes into ColorSpans.
	 * @param text The text to be scanned for color codes.
	 * @return Spannable containing the input text with added color codes.
	 */
	public Spannable addColors(String text) {
		SpannableStringBuilder builder = new SpannableStringBuilder();
		
		if (mMatcher.foundPartial()) {
			String partial = new String(mMatcher.text().substring(mMatcher.start()));
			text = partial + text;
		}
		
		mMatcher.match(text);
		int pos = 0;
		while (mMatcher.find()) {
			if (mMatcher.start() > pos) {
				Spannable prefix = new SpannableString(text.substring(pos, mMatcher.start()));
				prefix.setSpan(mForegroundColorSpan, 0, prefix.length(), 0);
				prefix.setSpan(mBackgroundColorSpan, 0, prefix.length(), 0);
				builder.append(prefix);
			}
			char command = mMatcher.command();
			if (command == 'm') {
				for (int i = 0; i < mMatcher.paramCount(); i++) {
					int param = mMatcher.param(i);
					if ((param == 0) || (param == 1)) {
						mIntensity = param;
					} else
					if ((param > 29) && (param < 38)) {
						mForegroundColorSpan = new ForegroundColorSpan(getColor(param));
					} else 
					if ((param > 39) && (param < 48)) {
						mBackgroundColorSpan = new BackgroundColorSpan(getColor(param));
					}
				}
			}
			pos = mMatcher.end();
		}
		if (mMatcher.foundPartial() == false) {
			if (pos < text.length()) {
				Spannable postfix = new SpannableString(text.substring(pos));
				postfix.setSpan(mForegroundColorSpan, 0, postfix.length(), 0);
				postfix.setSpan(mBackgroundColorSpan, 0, postfix.length(), 0);
				builder.append(postfix);
			}
		}
		return builder;
	}
	
	private int getColor(int parameter) {
		// http://en.wikipedia.org/wiki/ANSI_escape_code - xterm colors.
		if (mIntensity == 0){
			switch (parameter % 10) {
				case 0: return Color.BLACK;
				case 1: return 0xffcc0000;
				case 2: return 0xff00cc00;
				case 3: return 0xffcccc00;
				case 4: return 0xff0000ee;
				case 5: return 0xffcc00cc;
				case 6: return 0xff00cccc;
				case 7: return Color.GRAY;
			}
		} else {
			switch (parameter % 10) {
				case 0: return Color.BLACK;
				case 1: return Color.RED;
				case 2: return Color.GREEN;
				case 3: return Color.YELLOW;
				case 4: return 0xff5555ff;
				case 5: return Color.MAGENTA;
				case 6: return Color.CYAN;
				case 7: return Color.WHITE;
			}
		}
		return Color.CYAN;
	}

}
