package bander.taskman;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

/** Main activity for TaskMan, shows a list of running tasks. */
public class Main extends ListActivity {
	private static final String ICON		= "icon";
	private static final String TITLE 		= "title";
	private static final String DESCRIPTION = "desc";

	private static final int REFRESH_ID 	= Menu.FIRST + 0;
	private static final int SHOWALL_ID 	= Menu.FIRST + 1;

	private static final int KILL_ID 		= Menu.FIRST + 2;
	private static final int HIDE_ID 		= Menu.FIRST + 3;
	private static final int DETAILS_ID 	= Menu.FIRST + 4;

	private static final String HIDDENPACKAGES_FILENAME = "hiddenpackages";

	private List<ActivityManager.RunningTaskInfo> mRunningTasks;
	private List<String> mHiddenPackages;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		loadHiddenPackages();

		registerForContextMenu(getListView());
	}

	@Override
	protected void onResume() {
		super.onResume();
		listTasks();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		menu.add(0, REFRESH_ID, 0, R.string.menu_refresh)
			.setIcon(R.drawable.ic_menu_refresh);

		menu.add(0, SHOWALL_ID, 0, R.string.menu_showall)
			.setIcon(android.R.drawable.ic_menu_view);

		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case REFRESH_ID:
				listTasks();
				return true;
			case SHOWALL_ID:
				mHiddenPackages.clear();
				saveHiddenPackages();
				listTasks();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		int index = (int) id;
		killTask(index);
		listTasks();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		PackageManager packageManager = getPackageManager();
		ActivityManager.RunningTaskInfo taskInfo = mRunningTasks.get(info.position);
		try {
			ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
				taskInfo.baseActivity.getPackageName(), 0
			);
			menu.setHeaderTitle(
				packageManager.getApplicationLabel(applicationInfo).toString()
			);
		} catch (NameNotFoundException e) {
			// Fail silently.
		}

		menu.add(0, KILL_ID, 0, R.string.menu_kill);
		menu.add(0, HIDE_ID, 0, R.string.menu_hide);
		menu.add(0, DETAILS_ID, 0, R.string.menu_details);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		String packageName = mRunningTasks.get(info.position).baseActivity.getPackageName();
		switch (item.getItemId()) {
			case KILL_ID:
				killTask(info.position);
				return true;
			case HIDE_ID:
				mHiddenPackages.add(packageName);
				saveHiddenPackages();
				listTasks();
				return true;
			case DETAILS_ID:
				Intent intent = new Intent(this, Detail.class);
				intent.putExtra(Detail.PACKAGE_NAME, packageName);
				startActivity(intent);
				return true;
		}
		return super.onContextItemSelected(item);
	}

	/** Updates the mRunningTasks list and fills the Activity's List. */
	private void listTasks() {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> runningTasks = activityManager.getRunningTasks(128);

		PackageManager packageManager = getPackageManager();

		mRunningTasks = new ArrayList<RunningTaskInfo>();
		ArrayList<Map<String, Object>> listEntries = new ArrayList<Map<String, Object>>();
		for (RunningTaskInfo taskInfo : runningTasks) {
			if (mHiddenPackages.contains(taskInfo.baseActivity.getPackageName()))
				continue;

			mRunningTasks.add(taskInfo);

			Map<String, Object> entry = new HashMap<String, Object>();
			try {
				ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
					taskInfo.baseActivity.getPackageName(), 0
				);
				entry.put(ICON, packageManager.getApplicationIcon(applicationInfo));
				entry.put(TITLE, packageManager.getApplicationLabel(applicationInfo).toString());
			} catch (NameNotFoundException e) {
				entry.put(TITLE, taskInfo.baseActivity.getClassName());
			}
			entry.put(DESCRIPTION,
				getString((taskInfo.numRunning > 0) ? R.string.state_running : R.string.state_stopped)
			);
			listEntries.add(entry);
		}
		SimpleAdapter adapter = new SimpleAdapter(
			this, listEntries, R.layout.row, 
			new String[] { ICON, TITLE, DESCRIPTION },
			new int[] { R.id.icon, R.id.title, R.id.description }
		);
		adapter.setViewBinder(new DrawableViewBinder());

		setListAdapter(adapter);
	}

	/** Kills a specified task.
	 * @param index Index into the mRunningTasks list of the activity to kill.
	 */
	private void killTask(int index) {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.restartPackage(mRunningTasks.get(index).baseActivity.getPackageName());
	}

	/** Loads the list of hidden packages from private storage. */
	@SuppressWarnings("unchecked")
	private void loadHiddenPackages() {
		try {
			FileInputStream fis = openFileInput(HIDDENPACKAGES_FILENAME);
			ObjectInputStream ois = new ObjectInputStream(fis);
			mHiddenPackages = (List<String>) ois.readObject();
			ois.close();
			fis.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}
		if (mHiddenPackages == null)
			mHiddenPackages = new ArrayList<String>();
	}

	/** Saves the list of hidden packages to private storage. */
	private void saveHiddenPackages() {
		try {
			FileOutputStream fos = openFileOutput(HIDDENPACKAGES_FILENAME, Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(mHiddenPackages);
			oos.close();
			fos.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

}
