package bander.fileman;

/** Utility class containing MIME type-related helper functions. */
public class MimeUtils {

	/** Determines the MIME type for a given filename.
	 * @param filename The file to determine the MIME type of. 
	 * @return The MIME type of the file, or a wildcard if none could be determined. 
	 */
	public static String getType(final String filename) {
		// There does not seem to be a way to ask the OS or file itself for this 
		// information, so unfortunately resorting to extension sniffing. 

		int pos = filename.lastIndexOf('.'); 
		if (pos != -1) {
			String ext = filename.substring(filename.lastIndexOf('.')+1, filename.length());
			
			if (ext.equalsIgnoreCase("mp3")) return "audio/mpeg";
			if (ext.equalsIgnoreCase("aac")) return "audio/aac";
			if (ext.equalsIgnoreCase("wav")) return "audio/wav";
			if (ext.equalsIgnoreCase("mid")) return "audio/midi";
			if (ext.equalsIgnoreCase("midi")) return "audio/midi";
			if (ext.equalsIgnoreCase("wma")) return "audio/x-ms-wma";
			
			if (ext.equalsIgnoreCase("mp4")) return "video/mp4";
			if (ext.equalsIgnoreCase("avi")) return "video/x-msvideo";				
			if (ext.equalsIgnoreCase("wmv")) return "video/x-ms-wmv";				
			
			if (ext.equalsIgnoreCase("png")) return "image/png";
			if (ext.equalsIgnoreCase("jpg")) return "image/jpeg";
			if (ext.equalsIgnoreCase("jpe")) return "image/jpeg";
			if (ext.equalsIgnoreCase("jpeg")) return "image/jpeg";
			if (ext.equalsIgnoreCase("gif")) return "image/gif";
			
			if (ext.equalsIgnoreCase("xml")) return "text/xml";
			if (ext.equalsIgnoreCase("txt")) return "text/plain";
			if (ext.equalsIgnoreCase("cfg")) return "text/plain";
			if (ext.equalsIgnoreCase("csv")) return "text/plain";
			if (ext.equalsIgnoreCase("conf")) return "text/plain";
			if (ext.equalsIgnoreCase("rc")) return "text/plain";
			if (ext.equalsIgnoreCase("htm")) return "text/html";
			if (ext.equalsIgnoreCase("html")) return "text/html";
			
			if (ext.equalsIgnoreCase("pdf")) return "application/pdf";
			if (ext.equalsIgnoreCase("apk")) return "application/vnd.android.package-archive";
			
			// Additions and corrections are welcomed.
		}
		return "*/*";
	}
	
	/** Determines the file type icon for a given filename.
	 * @param filename The file to determine the file type icon of. 
	 * @return The file type icon of the file, or default document icon if none could be determined. 
	 */
	public static int getIconResource(final String filename) {
		// There does not seem to be a way to ask the OS or file itself for this 
		// information, so unfortunately resorting to extension sniffing.
		
		int pos = filename.lastIndexOf('.'); 
		if (pos != -1) {
			String ext = filename.substring(filename.lastIndexOf('.')+1, filename.length());
				
			if (ext.equalsIgnoreCase("mp3")) return R.drawable.audio;
			if (ext.equalsIgnoreCase("aac")) return R.drawable.audio;
			if (ext.equalsIgnoreCase("wav")) return R.drawable.audio;
			if (ext.equalsIgnoreCase("mid")) return R.drawable.audio;
			if (ext.equalsIgnoreCase("midi")) return R.drawable.audio;
			if (ext.equalsIgnoreCase("wma")) return R.drawable.audio;
			
			if (ext.equalsIgnoreCase("mp4")) return R.drawable.video;
			if (ext.equalsIgnoreCase("avi")) return R.drawable.video;				
			if (ext.equalsIgnoreCase("wmv")) return R.drawable.video;				
			
			if (ext.equalsIgnoreCase("png")) return R.drawable.image;
			if (ext.equalsIgnoreCase("jpg")) return R.drawable.image;
			if (ext.equalsIgnoreCase("jpe")) return R.drawable.image;
			if (ext.equalsIgnoreCase("jpeg")) return R.drawable.image;
			if (ext.equalsIgnoreCase("gif")) return R.drawable.image;
			
			if (ext.equalsIgnoreCase("xml")) return R.drawable.text;
			if (ext.equalsIgnoreCase("txt")) return R.drawable.text;
			if (ext.equalsIgnoreCase("conf")) return R.drawable.text;
			if (ext.equalsIgnoreCase("rc")) return R.drawable.text;
			if (ext.equalsIgnoreCase("htm")) return R.drawable.text;
			if (ext.equalsIgnoreCase("html")) return R.drawable.text;
			
			if (ext.equalsIgnoreCase("pdf")) return R.drawable.pdf;
			if (ext.equalsIgnoreCase("apk")) return R.drawable.apk;
			
			// Additions and corrections are welcomed.
		}
		return R.drawable.document;
	}
	
}
