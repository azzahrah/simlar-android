/*
 * Copyright (C) 2013 - 2015 The Simlar Authors.
 *
 * This file is part of Simlar. (https://www.simlar.org)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package org.simlar.helper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import org.simlar.R;
import org.simlar.logging.Lg;
import org.simlar.utils.Util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class PermissionsHelper
{
	private static final int REQUEST_CODE = 23;

	private PermissionsHelper()
	{
		throw new AssertionError("This class was not meant to be instantiated");
	}

	public enum Type{
		CONTACTS(Manifest.permission.READ_CONTACTS, false, R.string.permission_explain_text_contacts),
		MICROPHONE(Manifest.permission.RECORD_AUDIO, true, R.string.permission_explain_text_record_audio),
		PHONE(Manifest.permission.READ_PHONE_STATE, true, R.string.permission_explain_text_phone_state),
		SMS(Manifest.permission.READ_SMS, false, R.string.permission_explain_text_sms),
		STORAGE(storagePermission(), false, R.string.permission_explain_text_storage);

		private final String mPermission;
		private final boolean mMajor;
		private final int mRationalMessageId;

		Type(final String permission, final boolean major, final int rationalMessageId)
		{
			mPermission = permission;
			mMajor = major;
			mRationalMessageId = rationalMessageId;
		}

		@SuppressLint("InlinedApi")
		private static String storagePermission()
		{
			return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ? Manifest.permission.READ_EXTERNAL_STORAGE : "";
		}

		String getPermission()
		{
			return mPermission;
		}

		int getRationalMessageId()
		{
			return mRationalMessageId;
		}

		static Set<Type> getMajorPermissions(final boolean needsExternalStorage)
		{
			final Set<Type> majorTypes = EnumSet.noneOf(Type.class);
			for (final Type type : Type.values()) {
				if (type.mMajor) {
					majorTypes.add(type);
				}
			}

			if (needsExternalStorage) {
				majorTypes.add(STORAGE);
			}

			return majorTypes;
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean hasPermission(final Context context, final Type type)
	{
		return ContextCompat.checkSelfPermission(context, type.getPermission()) == PackageManager.PERMISSION_GRANTED;
	}

	public static boolean checkAndRequestPermissions(final Activity activity, final Type type)
	{
		return checkAndRequestPermissions(activity, EnumSet.of(type));
	}

	public static void requestMajorPermissions(final Activity activity, final boolean needsExternalStorage)
	{
		checkAndRequestPermissions(activity, Type.getMajorPermissions(needsExternalStorage));
	}

	public static boolean shouldShowRationale(final Activity activity, final Type type)
	{
		return ActivityCompat.shouldShowRequestPermissionRationale(activity, type.getPermission());
	}

	private static boolean checkAndRequestPermissions(final Activity activity, final Set<Type> types)
	{
		final Set<Type> requestTypes = EnumSet.noneOf(Type.class);
		final Set<String> rationalMessages = new HashSet<>();
		for (final Type type : types) {
			if (!hasPermission(activity, type)) {
				requestTypes.add(type);
				if (shouldShowRationale(activity, type)) {
					rationalMessages.add(activity.getString(type.getRationalMessageId()));
				}
			}
		}

		if (requestTypes.isEmpty()) {
			// all permissions granted
			return true;
		}

		if (rationalMessages.isEmpty()) {
			requestPermissions(activity, requestTypes);
		} else {
			showPermissionsRationaleAlert(activity, TextUtils.join("\n\n", rationalMessages), requestTypes);
		}

		return false;
	}

	@SuppressLint("NewApi")
	private static void showPermissionsRationaleAlert(final Activity activity, final String message, final Set<Type> types)
	{
		new AlertDialog.Builder(activity)
				.setMessage(message)
				.setOnDismissListener(new DialogInterface.OnDismissListener()
				{
					@Override
					public void onDismiss(final DialogInterface dialog)
					{
						requestPermissions(activity, types);
					}
				})
				.create().show();
	}

	public static void requestContactPermission(final Fragment fragment)
	{
		Lg.i("requesting contact permission");
		FragmentCompat.requestPermissions(fragment, new String[] { Type.CONTACTS.getPermission() }, REQUEST_CODE);
	}

	private static void requestPermissions(final Activity activity, final Set<Type> types)
	{
		final Set<String> permissions = new HashSet<>();
		for (final Type type : types) {
			permissions.add(type.getPermission());
		}
		Lg.i("requesting permissions: ", TextUtils.join(", ", permissions));

		ActivityCompat.requestPermissions(activity, permissions.toArray(new String[permissions.size()]), REQUEST_CODE);
	}

	@SuppressWarnings("SameParameterValue")
	public static boolean isGranted(final Type type, @NonNull final String[] permissions, @NonNull final int[] grantResults)
	{
		if (permissions.length != 1) {
			Lg.w("expected exactly one permission but got: ", TextUtils.join(", ", permissions));
		}

		final String permission = permissions[0];
		if (!Util.equalString(permission, type.getPermission())) {
			Lg.w("expected permission: ", type.getPermission(), " but got: ", permission);
			return false;
		}

		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			Lg.i("permission granted: ", permission);
			return true;
		} else {
			Lg.i("permission denied: ", permission);
			return false;
		}
	}

	public static boolean needsExternalStoragePermission(final Context context, final Uri uri)
	{
		if (context == null) {
			return false;
		}

		if (uri == null) {
			return false;
		}

		try {
			final FileInputStream stream = new FileInputStream(uri.getPath());
			stream.close();
			return false;
		} catch (final FileNotFoundException e) {
			return true;
		} catch (final IOException e) {
			return true;
		}
	}

	public static boolean isNotificationPolicyAccessGranted(final Context context)
	{
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
				((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted();
	}

	public static void checkAndRequestNotificationPolicyAccess(final Activity activity)
	{
		if (isNotificationPolicyAccessGranted(activity)) {
			return;
		}

		new AlertDialog.Builder(activity)
				.setMessage(activity.getString(R.string.permission_explain_text_notification_policy_access))
				.setPositiveButton(R.string.permission_explain_text_notification_policy_access_button, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(final DialogInterface dialogInterface, final int i)
					{
						openNotificationPolicyAccessSettings(activity);
					}
				})
				.create().show();
	}

	public static void openNotificationPolicyAccessSettings(final Context context)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			context.startActivity(
					new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		}
	}

	public static void openAppSettings(final Context context)
	{
		context.startActivity(
				new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.getPackageName(), null))
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}
}
