package com.plugin.librarykotlin;

import android.os.Parcel;
import android.os.Parcelable;

public class JavaParcelable implements Parcelable {

    public int i = 0;
    public String string = "JavaParcelable";

    public JavaParcelable() {

    }

    public JavaParcelable(Parcel source) {
        this.i = source.readInt();
        this.string = source.readString();
    }

    public static final Creator<JavaParcelable> CREATOR = new Creator<JavaParcelable>() {
        @Override
        public JavaParcelable createFromParcel(Parcel source) {
            return new JavaParcelable(source);
        }

        @Override
        public JavaParcelable[] newArray(int size) {
            return new JavaParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(i);
        dest.writeString(string);
    }
}
