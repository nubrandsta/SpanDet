package com.stti.spandet.ui.info

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.stti.spandet.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AdjacentDefectFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AdjacentDefectFragment : Fragment() {

    private var countDefects: Int? = null


    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            countDefects = it.getInt("countDefects")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_adjacent_defect, container, false)
        val tvDefectChip: TextView = view.findViewById(R.id.tv_defect_chip)
        tvDefectChip.text = countDefects.toString()
        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(countDefects: Int) =
            AdjacentDefectFragment().apply {
                arguments = Bundle().apply {
                    putInt("countDefects", countDefects)
                }
            }
    }
}