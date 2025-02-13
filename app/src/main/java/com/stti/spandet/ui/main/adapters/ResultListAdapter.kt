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
                val adjCount = result.classOccurence.adj
                val intCount = result.classOccurence.int
                val geoCount = result.classOccurence.geo
                val proCount = result.classOccurence.pro
                val nonCount = result.classOccurence.non

                val isEmpty = result.isEmpty
                if(isEmpty){
                    binding.tvStatusError.visibility = android.view.View.VISIBLE
                    binding.tvStatusDefect.visibility = android.view.View.GONE
                    binding.tvStatusOK.visibility = android.view.View.GONE
                }
                else if(adjCount>0 && intCount<1 && geoCount<1 && proCount<1 && nonCount<1){
                    binding.tvDefectAdj.text = adjCount.toString()
                    binding.tvDefectAdj.visibility = android.view.View.VISIBLE

                    binding.tvStatusCheck.visibility = android.view.View.VISIBLE
                    binding.tvStatusError.visibility = android.view.View.GONE
                    binding.tvStatusOK.visibility = android.view.View.GONE
                }
                else{
                    if(adjCount>0 || intCount>0 || geoCount>0 || proCount>0 || nonCount>0){
                        binding.tvStatusDefect.visibility = android.view.View.VISIBLE
                        binding.tvStatusError.visibility = android.view.View.GONE
                        binding.tvStatusOK.visibility = android.view.View.GONE
                        binding.tvStatusCheck.visibility = android.view.View.GONE

                        if(adjCount>0){
                            binding.tvDefectAdj.text = adjCount.toString()
                            binding.tvDefectAdj.visibility = android.view.View.VISIBLE
                        } else{
                            binding.tvDefectAdj.visibility = android.view.View.GONE
                        }
                        if(intCount>0){
                            binding.tvDefectInt.text = intCount.toString()
                            binding.tvDefectInt.visibility = android.view.View.VISIBLE
                        } else{
                            binding.tvDefectInt.visibility = android.view.View.GONE
                        }
                        if(geoCount>0){
                            binding.tvDefectGeo.text = geoCount.toString()
                            binding.tvDefectGeo.visibility = android.view.View.VISIBLE
                        } else{
                            binding.tvDefectGeo.visibility = android.view.View.GONE
                        }
                        if(proCount>0){
                            binding.tvDefectPro.text = proCount.toString()
                            binding.tvDefectPro.visibility = android.view.View.VISIBLE
                        } else{
                            binding.tvDefectPro.visibility = android.view.View.GONE
                        }
                        if(nonCount>0){
                            binding.tvDefectNon.text = nonCount.toString()
                            binding.tvDefectNon.visibility = android.view.View.VISIBLE
                        } else{
                            binding.tvDefectNon.visibility = android.view.View.GONE

                        }
                    }
                    else{
                        binding.tvStatusOK.visibility = android.view.View.VISIBLE
                        binding.tvStatusError.visibility = android.view.View.GONE
                        binding.tvStatusDefect.visibility = android.view.View.GONE

                    }
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