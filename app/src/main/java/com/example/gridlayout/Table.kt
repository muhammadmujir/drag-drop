package com.example.gridlayout

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Table(
    val id: Int? = null,
    val x: Int? = null,
    val y: Int? = null
): Parcelable