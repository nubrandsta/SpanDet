package com.stti.spandet.ui.main.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stti.spandet.data.model.ProcessImage
import com.stti.spandet.databinding.ItemProcessBinding
import com.stti.spandet.ui.main.adapters.imageListAdapter.MyViewHolder.Companion.DIFF_CALLBACK_IMAGE

class imageListAdapter (
    private val onClick: (ProcessImage) -> Unit
) : ListAdapter<ProcessImage, imageListAdapter.MyViewHolder>(DIFF_CALLBACK_IMAGE) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            ItemProcessBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val list = getItem(position)
        holder.bind(list)
    }

    class MyViewHolder(
        private val binding: ItemProcessBinding,
        private val onClick: (ProcessImage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(image: ProcessImage) {
            binding.apply {
                //display image from uri
                val uri = image.uri
                uri.let {
                    Log.d("Image URI", "showImage: $it")
                    binding.ivIcon.setImageURI(it)
                }


                root.setOnClickListener {
                    onClick(image)

                }
            }

        }

        companion object {

            val DIFF_CALLBACK_IMAGE = object : DiffUtil.ItemCallback<ProcessImage>() {
                override fun areItemsTheSame(
                    oldItem: ProcessImage,
                    newItem: ProcessImage
                ): Boolean {
                    return oldItem.uri == newItem.uri
                }

                override fun areContentsTheSame(
                    oldItem: ProcessImage,
                    newItem: ProcessImage
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}