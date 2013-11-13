/**
 * Copyright (C) 2013 The Simlar Authors.
 *
 * This file is part of Simlar. (http://www.simlar.org)
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
 */

package org.simlar;

public enum CreateAccountStatus {
	NONE(0),
	WAITING_FOR_SMS(1),
	SUCCESS(2);

	final int mValue;

	private CreateAccountStatus(final int i)
	{
		mValue = i;
	}

	public static CreateAccountStatus fromInt(final int i)
	{
		switch (i) {
		case 1:
			return WAITING_FOR_SMS;
		case 2:
			return SUCCESS;
		default:
			return NONE;
		}
	}

	public int toInt()
	{
		return mValue;
	}
}
