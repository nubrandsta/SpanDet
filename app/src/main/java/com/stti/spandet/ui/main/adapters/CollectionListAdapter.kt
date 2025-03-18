package com.stti.spandet.ui.main.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stti.spandet.data.model.Collection
import com.stti.spandet.databinding.ItemCollectionBinding
import com.stti.spandet.ui.main.adapters.collectionListAdapter.MyViewHolder.Companion.DIFF_CALLBACK_COLLECTION

class collectionListAdapter (
    private val onClick: (Collection) -> Unit
) : ListAdapter<Collection, collectionListAdapter.MyViewHolder>(DIFF_CALLBACK_COLLECTION) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            ItemCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val list = getItem(position)
        holder.bind(list)
    }

    class MyViewHolder(
        private val binding: ItemCollectionBinding,
        private val onClick: (Collection) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(collection: Collection) {
            binding.apply {
                tvName.text = collection.locationString
                tvDate.text = collection.name
                tvCount.text = collection.imgCount.toString()
//                tvDate.text = "remark"


                root.setOnClickListener {
                    onClick(collection)

                }
            }

        }

        companion object {

            val DIFF_CALLBACK_COLLECTION = object : DiffUtil.ItemCallback<Collection>() {
                override fun areItemsTheSame(
                    oldItem: Collection,
                    newItem: Collection
                ): Boolean {
                    return oldItem.name == newItem.name
                }

                override fun areContentsTheSame(
                    oldItem: Collection,
                    newItem: Collection
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}