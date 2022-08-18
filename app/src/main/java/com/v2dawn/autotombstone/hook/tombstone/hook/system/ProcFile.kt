package com.v2dawn.autotombstone.hook.tombstone.hook.system;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

open class ProcFile : File, Parcelable {
    val content: String?

    protected constructor(path: String?) : super(path) {
        content = readFile(path)
    }

    protected constructor(`in`: Parcel) : super(`in`.readString()) {
        content = `in`.readString()
    }

    override fun length(): Long {
        return content!!.length.toLong()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(absolutePath)
        dest.writeString(content)
    }

    companion object {
        @Throws(IOException::class)
        protected fun readFile(path: String?): String {
            var reader: BufferedReader? = null
            return try {
                val output = StringBuilder()
                reader = BufferedReader(FileReader(path))
                var line = reader.readLine()
                var newLine = ""
                while (line != null) {
                    output.append(newLine).append(line)
                    newLine = "\n"
                    line = reader.readLine()
                }
                output.toString()
            } finally {
                reader?.close()
            }
        }

        @JvmField
        val CREATOR: Creator<ProcFile?> = object : Creator<ProcFile?> {
            override fun createFromParcel(`in`: Parcel): ProcFile? {
                return ProcFile(`in`)
            }

            override fun newArray(size: Int): Array<ProcFile?> {
                return arrayOfNulls(size)
            }
        }
    }

}
