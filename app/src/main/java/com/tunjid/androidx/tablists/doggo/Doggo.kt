package com.tunjid.androidx.tablists.doggo

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.tunjid.androidx.R
import java.util.*

/**
 * Model class for G O O D  B O Y E S  A N D  G I R L E S  B R O N T
 *
 *
 * Created by tj.dahunsi on 5/20/17.
 */

class Doggo : Parcelable {

    @DrawableRes
    @get:DrawableRes
    var imageRes: Int
    var name: String

    private constructor(@DrawableRes imageRes: Int) {
        this.imageRes = imageRes
        name = ("${DOGGO_ADJECTIVES[Random().nextInt(DOGGO_ADJECTIVES.size)]}  ${DOGGO_NOUNS[Random().nextInt(DOGGO_NOUNS.size)]}")
    }

    private constructor(`in`: Parcel) {
        imageRes = `in`.readInt()
        name = `in`.readString()!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Doggo) return false

        val doggo = other as Doggo?

        return imageRes == doggo!!.imageRes && name == doggo.name
    }

    override fun hashCode(): Int = imageRes

    override fun toString(): String = name + "-" + hashCode()

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(imageRes)
        dest.writeString(name)
    }

    companion object {

        var transitionDoggo: Doggo? = null

        private val DOGGO_NOUNS = listOf(
                "F R E N",
                "B O Y E",
                "G I R L E"
        )

        private val DOGGO_ADJECTIVES = listOf(
                "F L Y",
                "L A Z Y",
                "G O O D",
                "F L O O F",
                "H E C K I N",
                "B A M B O O Z L E"
        )

        val doggos: List<Doggo> = listOf(
                Doggo(R.drawable.doggo1),
                Doggo(R.drawable.doggo2),
                Doggo(R.drawable.doggo3),
                Doggo(R.drawable.doggo4),
                Doggo(R.drawable.doggo5),
                Doggo(R.drawable.doggo6),
                Doggo(R.drawable.doggo7),
                Doggo(R.drawable.doggo8),
                Doggo(R.drawable.doggo9)
        )

        val transitionIndex: Int
            get() = transitionDoggo?.let { doggos.indexOf(it) } ?: 0

        @JvmField
        @Suppress("unused")
        val CREATOR: Parcelable.Creator<Doggo> = object : Parcelable.Creator<Doggo> {
            override fun createFromParcel(`in`: Parcel): Doggo = Doggo(`in`)

            override fun newArray(size: Int): Array<Doggo?> = arrayOfNulls(size)
        }
    }
}
