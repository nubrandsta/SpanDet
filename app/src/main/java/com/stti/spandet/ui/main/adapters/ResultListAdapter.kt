package com.stti.spandet.ui.main.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stti.spandet.data.model.ResultImage
import com.stti.spandet.databinding.ItemResultBinding
import com.stti.spandet.ui.main.adapters.resultListAdapter.MyViewHolder.Companion.DIFF_CALLBACK_RESULT

class resultListAdapter (
    private val onClick: (ResultImage) -> Unit
) : ListAdapter<ResultImage, resultListAdapter.MyViewHolder>(DIFF_CALLBACK_RESULT) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val list = getItem(position)
        holder.bind(list)
    }

    class MyViewHolder(
        private val binding: ItemResultBinding,
        private val onClick: (ResultImage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: ResultImage) {
            binding.apply {
                //display image from uri
                val uri = result.uri
                uri.let {
                    Log.d("Image URI", "showImage: $it")
                    binding.ivIcon.setImageURI(it)
                }
                val spandukCount = result.classOccurence.spanduk

                val isEmpty = result.isEmpty
                if(isEmpty){
                    binding.tvStatusError.visibility = android.view.View.VISIBLE
                    binding.tvStatusDefect.visibility = android.view.View.GONE
                    binding.tvStatusOK.visibility = android.view.View.GONE
                }
                else if(spandukCount>0){
                    binding.tvDefectAdj.text = spandukCount.toString()
                    binding.tvDefectAdj.visibility = android.view.View.VISIBLE

                    binding.tvStatusCheck.visibility = android.view.View.VISIBLE
                    binding.tvStatusError.visibility = android.view.View.GONE
                    binding.tvStatusOK.visibility = android.view.View.GONE

//                    binding.tvStatusDefect.visibility = android.view.View.VISIBLE
                    binding.tvStatusError.visibility = android.view.View.GONE
                    binding.tvStatusOK.visibility = android.view.View.GONE
                    binding.tvStatusCheck.visibility = android.view.View.GONE

                    binding.tvDefectAdj.text = spandukCount.toString()
                    binding.tvDefectAdj.visibility = android.view.View.VISIBLE
                }
                else{
                    binding.tvStatusOK.visibility = android.view.View.VISIBLE
                    binding.tvStatusError.visibility = android.view.View.GONE
                    binding.tvStatusDefect.visibility = android.view.View.GONE
                    binding.tvDefectAdj.visibility = android.view.View.GONE

                }



                root.setOnClickListener {
                    onClick(result)

                }
            }

        }

        companion object {

            val DIFF_CALLBACK_RESULT = object : DiffUtil.ItemCallback<ResultImage>() {
                override fun areItemsTheSame(
                    oldItem: ResultImage,
                    newItem: ResultImage
                ): Boolean {
                    return oldItem.uri == newItem.uri
                }

                override fun areContentsTheSame(
                    oldItem: ResultImage,
                    newItem: ResultImage
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}