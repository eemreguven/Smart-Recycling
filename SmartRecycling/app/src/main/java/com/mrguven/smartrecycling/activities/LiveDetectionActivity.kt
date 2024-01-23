package com.mrguven.smartrecycling.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mrguven.smartrecycling.databinding.ActivityLiveDetectionBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class LiveDetectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLiveDetectionBinding
    private lateinit var selectedContainerName:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val containerName = intent.getStringExtra(CONTAINER_KEY)

        if (containerName != null) {
            selectedContainerName = containerName
        }

        binding = ActivityLiveDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun getContainerName() : String{
        return selectedContainerName
    }
    companion object{

        const val CONTAINER_KEY = "CONTAINER_TITLE"
    }
}