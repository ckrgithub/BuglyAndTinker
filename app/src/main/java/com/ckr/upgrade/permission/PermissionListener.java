package com.ckr.upgrade.permission;

/**
 * Created on 2018/10/27
 *
 * @author ckr
 */

public interface PermissionListener {

	void onPermissionGranted(int requestCode);

	void onPermissionPermanentlyDenied(int requestCode);

	void onPermissionDenied(int requestCode);
}
