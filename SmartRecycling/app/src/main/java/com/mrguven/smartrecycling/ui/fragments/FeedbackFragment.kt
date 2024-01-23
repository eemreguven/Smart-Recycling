package com.mrguven.smartrecycling.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.mrguven.smartrecycling.R
import com.mrguven.smartrecycling.ui.adapters.PackagingAdapter
import com.mrguven.smartrecycling.data.local.RecycledPackaging
import com.mrguven.smartrecycling.data.local.RecyclingProcess
import com.mrguven.smartrecycling.data.model.Packaging
import com.mrguven.smartrecycling.databinding.FragmentFeedbackBinding
import com.mrguven.smartrecycling.viewmodels.RecyclingViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar


@AndroidEntryPoint
class FeedbackFragment : Fragment() {

    private var _fragmentFeedbackBinding: FragmentFeedbackBinding? = null
    private val fragmentFeedbackBinding get() = _fragmentFeedbackBinding!!

    private lateinit var detectionList: List<Packaging>

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PackagingAdapter
    private lateinit var lottieAnimationView: LottieAnimationView

    private lateinit var containerName: String

    private val recyclingViewModel: RecyclingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _fragmentFeedbackBinding = FragmentFeedbackBinding.inflate(inflater, container, false)

        val args: FeedbackFragmentArgs by navArgs()
        detectionList = args.detectionList.toList()
        containerName = args.containerName ?: "Unknown Container"

        recyclerView = fragmentFeedbackBinding.detectionResultRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = PackagingAdapter(detectionList)
        recyclerView.adapter = adapter
        lottieAnimationView = fragmentFeedbackBinding.lottieAnimationView


        return fragmentFeedbackBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Disable back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
        }

        // Set animation from JSON file in the res/raw folder
        lottieAnimationView.setAnimation(R.raw.finished_recycling_animation)
        lottieAnimationView.playAnimation()

        // Delay and navigate to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            lottieAnimationView.cancelAnimation()

            saveRecyclingProcessToDatabase()
            navigateToMainActivity()
        }, 5000) // 5000 milliseconds (5 seconds)
    }

    private fun navigateToMainActivity() {
        // If the fragment is still attached to the activity
        if (isAdded) {
            // Replace with the actual MainActivity class or destination
            requireActivity().finish()
        }
    }


    private fun saveRecyclingProcessToDatabase() {
        val currentDateTime = Calendar.getInstance()

        val recyclingProcess = RecyclingProcess(
            date = currentDateTime.timeInMillis,
            containerName = containerName
        )
        val recycledPackagingList: List<RecycledPackaging> = detectionList.map {
            RecycledPackaging(
                type = it.type.toString(),
                title = it.name,
                count = it.count,
                recyclingProcessId = recyclingProcess.id
            )
        }
        recyclingViewModel.createRecyclingProcessWithPackaging(
            recyclingProcess,
            recycledPackagingList
        )

        recyclingViewModel.creationStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "You have successfully contributed to recycling!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(requireContext(), "An error occurred, try again!", Toast.LENGTH_SHORT).show()
            }
        }
    }

}