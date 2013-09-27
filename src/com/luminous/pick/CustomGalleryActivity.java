package com.luminous.pick;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class CustomGalleryActivity extends Activity {

	GridView gridGallery;
	Handler handler;
	GalleryAdapter adapter;

	ImageView imgNoMedia;
	Button btnGalleryOk;

	String action;
    private ImageLoader imageLoader;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.gallery);

		action = getIntent().getAction();
		if (action == null) {
			finish();
		}
        initImageLoader();
		init();
	}

    private void initImageLoader() {
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .build();
        
        int width = getResources().getDimensionPixelSize(R.dimen.photo_width);
        int height = getResources().getDimensionPixelSize(R.dimen.photo_height);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
        		.defaultDisplayImageOptions(defaultOptions)
        		.memoryCacheExtraOptions(width, height)
        		.build();

        imageLoader = ImageLoader.getInstance();
        imageLoader.init(config);
    }

	private void init() {

		handler = new Handler();
		gridGallery = (GridView) findViewById(R.id.gridGallery);
		gridGallery.setFastScrollEnabled(true);
		adapter = new GalleryAdapter(getApplicationContext(), imageLoader);

		if (action.equalsIgnoreCase(Action.ACTION_MULTIPLE_PICK)) {

			findViewById(R.id.llBottomContainer).setVisibility(View.VISIBLE);
			gridGallery.setOnItemClickListener(mItemMulClickListener);
			adapter.setMultiplePick(true);

		} else if (action.equalsIgnoreCase(Action.ACTION_PICK)) {

			findViewById(R.id.llBottomContainer).setVisibility(View.GONE);
			gridGallery.setOnItemClickListener(mItemSingleClickListener);
			adapter.setMultiplePick(false);

		}

		gridGallery.setAdapter(adapter);
		imgNoMedia = (ImageView) findViewById(R.id.imgNoMedia);

		btnGalleryOk = (Button) findViewById(R.id.btnGalleryOk);
		btnGalleryOk.setOnClickListener(mOkClickListener);

		new Thread() {

			@Override
			public void run() {
				Looper.prepare();
				handler.post(new Runnable() {

					@Override
					public void run() {
						adapter.addAll(getGalleryPhotos());
						checkImageStatus();
					}
				});
				Looper.loop();
			};

		}.start();

	}

	private void checkImageStatus() {
		if (adapter.isEmpty()) {
			imgNoMedia.setVisibility(View.VISIBLE);
		} else {
			imgNoMedia.setVisibility(View.GONE);
		}
	}

	View.OnClickListener mOkClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			ArrayList<CustomGallery> selected = adapter.getSelected();

			String[] allPath = new String[selected.size()];
			for (int i = 0; i < allPath.length; i++) {
				allPath[i] = selected.get(i).sdcardPath;
			}

			Intent data = new Intent().putExtra("all_path", allPath);
			setResult(RESULT_OK, data);
			finish();

		}
	};
	AdapterView.OnItemClickListener mItemMulClickListener = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
			adapter.changeSelection(v, position);

		}
	};

	AdapterView.OnItemClickListener mItemSingleClickListener = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
			CustomGallery item = adapter.getItem(position);
			Intent data = new Intent().putExtra("single_path", item.sdcardPath);
			setResult(RESULT_OK, data);
			finish();
		}
	};

	private ArrayList<CustomGallery> getGalleryPhotos() {
		ArrayList<CustomGallery> galleryList = new ArrayList<CustomGallery>();

		try {
			final String[] columns = {
					MediaStore.Images.Media._ID,
					MediaStore.Images.Media.DATA
			};
			final String orderBy = MediaStore.Images.Media._ID;

			Cursor imagecursor = getContentResolver().query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
					null, null, orderBy);
			try {
				if (imagecursor != null && imagecursor.getCount() > 0) {
	
					while (imagecursor.moveToNext()) {
						CustomGallery item = new CustomGallery();
	
						int idIndex = imagecursor.getColumnIndex(MediaStore.Images.Media._ID);
						int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);
	
						item.sdcardPath = imagecursor.getString(dataColumnIndex);
						
						final String[] thumbnailColumns = {
								MediaStore.Images.Thumbnails._ID,
								MediaStore.Images.Thumbnails.DATA
						};
						int originalImageId = imagecursor.getInt(idIndex);
						Cursor thumbnailCursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
								getContentResolver(), originalImageId, MediaStore.Images.Thumbnails.MINI_KIND, thumbnailColumns
						);
						try {
							if (thumbnailCursor != null) {
								int thumbnailDataColumnIndex = thumbnailCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA);
								if (thumbnailCursor.moveToFirst()) {
									item.thumbnailPath = thumbnailCursor.getString(thumbnailDataColumnIndex);
								}
							}
						} finally {
							if (thumbnailCursor != null) {
								thumbnailCursor.close();
							}
						}
						
						galleryList.add(item);
					}
				}
			} finally {
				if (imagecursor != null) {
					imagecursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

        // show newest photo at beginning of the list
		Collections.reverse(galleryList);
        return galleryList;
	}

}
