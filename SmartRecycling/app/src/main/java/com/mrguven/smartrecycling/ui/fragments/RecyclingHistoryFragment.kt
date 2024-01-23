package com.mrguven.smartrecycling.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.mrguven.smartrecycling.R
import com.mrguven.smartrecycling.ui.adapters.RecyclingProcessAdapter
import com.mrguven.smartrecycling.data.local.RecycledPackaging
import com.mrguven.smartrecycling.data.local.RecyclingProcessWithPackaging
import com.mrguven.smartrecycling.data.model.Packaging
import com.mrguven.smartrecycling.data.model.PackagingTypes
import com.mrguven.smartrecycling.databinding.FragmentRecyclingHistoryBinding
import com.mrguven.smartrecycling.viewmodels.RecyclingHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.BufferedReader
import java.io.InputStreamReader


@AndroidEntryPoint
class RecyclingHistoryFragment : Fragment() {
    private val recyclingHistoryViewModel: RecyclingHistoryViewModel by viewModels()

    private var _fragmentRecyclingHistoryBinding: FragmentRecyclingHistoryBinding? = null
    private val fragmentRecyclingHistoryBinding get() = _fragmentRecyclingHistoryBinding!!

    private lateinit var pieChart: PieChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentRecyclingHistoryBinding =
            FragmentRecyclingHistoryBinding.inflate(inflater, container, false)
        return fragmentRecyclingHistoryBinding.root
    }

    private lateinit var adapter: RecyclingProcessAdapter


    private var recyclingProcessList: MutableList<RecyclingProcessWithPackaging> = mutableListOf()
private var recycledPackagingList: MutableList<RecycledPackaging>  = mutableListOf()



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        readNamesFromFile()

        val recyclerView = fragmentRecyclingHistoryBinding.historyRecyclerView

        pieChart = fragmentRecyclingHistoryBinding.pieChart

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RecyclingProcessAdapter(recyclingProcessList, {})
        recyclerView.adapter = adapter

        recyclingHistoryViewModel.recyclingProcesses.observe(viewLifecycleOwner) { processes ->
            recyclingProcessList = processes.toMutableList()
            adapter.updateData(processes)
        }
        recyclingHistoryViewModel.loadRecyclingProcesses()

        recyclingHistoryViewModel.recycledPackagingList.observe(viewLifecycleOwner){
            recycledPackagingList = it.toMutableList()
            val list = recycledPackagingList.map { packaging -> PieEntry(packaging.count.toFloat(), packaging.title) }
            val colors = ColorTemplate.MATERIAL_COLORS.take(recycledPackagingList.size)
            val dataset = PieDataSet(list, "Packagings").apply {
                setColors(colors)
            }
            val pieData = PieData(dataset)
            pieChart.data = pieData
        }
        recyclingHistoryViewModel.loadRecycledPackagingList()

        recyclingHistoryViewModel.toggleState.observe(viewLifecycleOwner) {
            fragmentRecyclingHistoryBinding.historyRecyclerView.visibility = if (it) View.GONE else View.VISIBLE
            fragmentRecyclingHistoryBinding.pieChart.visibility = if (it) View.VISIBLE else View.GONE
        }


        val toggleButton = fragmentRecyclingHistoryBinding.toggleButton

        recyclingHistoryViewModel.toggleState.observe(viewLifecycleOwner) { isChecked ->
            toggleButton.isChecked = isChecked
        }

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            recyclingHistoryViewModel.setToggleState(isChecked)
        }
    }

    private fun readNamesFromFile(fileName: String = "packagingNameMap.txt") {
        try {
            val inputStream = requireActivity().assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(", ")
                if (parts.size == 3) {
                    val type = PackagingTypes.valueOf(parts[0].uppercase())
                    val id = parts[1]
                    val name = parts[2]
                    val packaging = Packaging(type, id, name)
                    packagingMap[id] = packaging
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var packagingList: MutableList<Packaging> = mutableListOf()
    private var packagingMap: MutableMap<String, Packaging> = mutableMapOf()
}