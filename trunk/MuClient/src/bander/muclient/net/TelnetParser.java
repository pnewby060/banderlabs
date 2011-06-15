package bander.muclient.net;

public class TelnetParser {
	private static final int 	STATE_IAC			= 1;
	private static final int 	STATE_COMMAND		= 2;
	private static final int 	STATE_ARGUMENT		= 3;
	
	private static final int	CMD_IAC		= 0x00FF;
	private static final int	CMD_SB		= 0x00FA;
	private static final int	CMD_WILL	= 0x00FB;
	private static final int	CMD_WONT	= 0x00FC;
	private static final int	CMD_DO		= 0x00FD;
	private static final int	CMD_DONT	= 0x00FE;
	private static final int	CMD_SE		= 0x00F0;

	private int		mState 			= STATE_IAC;
	private boolean	mSubNegotiate 	= false;
	
	/** Scans the provided text and interprets TELNET command codes.
	 * @param text The text to be scanned for TELNET commands.
	 * @return CharSequence containing the input text with TELNET commands interpreted.
	 */
	public CharSequence parse(String text) {
		// Currently, a do-nothing implementation.
		StringBuilder builder = new StringBuilder();
		int pos = 0;
		
		while (pos < text.length()) {
			switch (mState) {
				case STATE_IAC:
					int index = text.indexOf(CMD_IAC, pos);
					if (index != -1) { 
						mState = STATE_COMMAND;
						pos = index+1;
					} else {
						if (mSubNegotiate == false) {
							builder.append(text.substring(pos));
						}
						pos = text.length();
					}
					break;
				case STATE_COMMAND:
					switch (text.charAt(pos)) {
						case CMD_WILL:
						case CMD_WONT:
						case CMD_DO:
						case CMD_DONT:
							mState = STATE_ARGUMENT;
							pos++;
							break;
						case CMD_SB:
							mState = STATE_IAC;
							pos++;
							mSubNegotiate = true;
							break;
						case CMD_SE:
							mState = STATE_IAC;
							pos++;
							mSubNegotiate = false;
							break;
						default:
							mState = STATE_IAC;
							pos++;
							break;
					}
					break;
				case STATE_ARGUMENT:
					mState = STATE_IAC;
					pos++;
					break;
			}
		}
		return builder;
	}
}
