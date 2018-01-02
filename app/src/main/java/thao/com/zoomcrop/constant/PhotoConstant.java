package thao.com.zoomcrop.constant;

public final class PhotoConstant {

	/*
	 * Support to open camera or gallery to get a photo or crop a photo or open
	 * edit diary photo. That is in the order. Support to back to previous
	 * screen.
	 */

	public static final String EXTRA_PHOTO_ACTION = "EXTRA_PHOTO_ACTION";

	/**
	 * Action for nothing to do
	 */
	public static final int PHOTO_ACTION_NONE = -1;

	/**
	 * Action for selecting photo only, the result is the photo path.
	 */
	public static final int PHOTO_ACTION_SELECT = 0;

	/**
	 * Action for selecting & cropping photo, the result is the photo path of cropped one.
	 */
	public static final int PHOTO_ACTION_CROP = 1;

	/**
	 * Action for selecting & cropping photo & the end, move to Diary screen.
	 */
	public static final int PHOTO_ACTION_DIARY = 2;

}
