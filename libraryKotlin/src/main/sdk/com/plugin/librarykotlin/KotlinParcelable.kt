package com.plugin.librarykotlin

import android.os.Parcel
import android.os.Parcelable

class KotlinParcelable : Parcelable {

    var int: Int = 0

    var string: String = "KotlinParcelable"

    constructor()

    constructor(source: Parcel) {
        source.writeInt(int)
        source.writeString(string)
    }

    companion object{

        @JvmField
        val CREATOR: Parcelable.Creator<KotlinParcelable> = object : Parcelable.Creator<KotlinParcelable> {
            override fun createFromParcel(source: Parcel): KotlinParcelable {
                return KotlinParcelable(source)
            }

            override fun newArray(size: Int): Array<KotlinParcelable> {
                return Array(size) {KotlinParcelable()}
            }
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(int)
        dest?.writeString(string)
    }
}