package com.mrguven.smartrecycling.data.model

import android.os.Parcel
import android.os.Parcelable

data class Packaging(
    val type: PackagingTypes,
    val id: String,
    val name: String,
    var count: Int = 0,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readSerializable() as PackagingTypes,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(type)
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeInt(count)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Packaging> {
        override fun createFromParcel(parcel: Parcel): Packaging {
            return Packaging(parcel)
        }

        override fun newArray(size: Int): Array<Packaging?> {
            return arrayOfNulls(size)
        }
    }
}

