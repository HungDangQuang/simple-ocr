package org.tensorflow.lite.examples.ocr.adapter

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.tensorflow.lite.examples.ocr.ImageUtils
import org.tensorflow.lite.examples.ocr.R

class ImageAdapter(private val context: Context, private val mList: List<String>, private val mListener: OnItemClickListener?): Adapter<ImageAdapter.ViewHolder>() {


    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_adapter, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image:Bitmap = ImageUtils.getBitmapFromAssetsFile(context, mList[position])
        holder.imageView.setImageBitmap(image)

    }
}