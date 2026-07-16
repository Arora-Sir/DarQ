package com.kieronquinn.app.darq.ui.screens.bottomsheets.update

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.databinding.FragmentBottomSheetUpdateDownloadBinding
import com.kieronquinn.app.darq.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.darq.ui.screens.container.ContainerSharedViewModel
import com.kieronquinn.app.darq.utils.extensions.navGraphViewModel
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class UpdateDownloadBottomSheetFragment: BaseBottomSheetFragment<FragmentBottomSheetUpdateDownloadBinding>(FragmentBottomSheetUpdateDownloadBinding::inflate) {

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        sharedViewModel.clearUpdate()
    }

    private val updateViewModel by viewModel<UpdateDownloadBottomSheetViewModel>()

    private val sharedViewModel by navGraphViewModel<ContainerSharedViewModel>(R.id.nav_graph_main)

    override val cancelable = false

    private val update by lazy {
        sharedViewModel.getAvailableUpdate()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fragmentUpdateDownloadCancel.setOnClickListener {
            updateViewModel.cancelDownload(requireContext())
            dismiss()
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root){ view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val extraPadding = resources.getDimension(R.dimen.padding_8).toInt()
            view.updatePadding(bottom = bottomInset + extraPadding)
            insets
        }
        binding.fragmentUpdateDownloadProgress.applyMonet()
        val accentColor = monet.getAccentColor(requireContext())
        binding.fragmentUpdateDownloadCancel.setTextColor(accentColor)
        binding.fragmentUpdateDownloadInstall.setTextColor(accentColor)
        viewLifecycleOwner.lifecycleScope.launch {
            updateViewModel.downloadState.collect { state ->
                when(state){
                    is UpdateDownloadBottomSheetViewModel.State.Downloading -> {
                        if(state.progress > 0) {
                            binding.fragmentUpdateDownloadProgress.isIndeterminate = false
                            binding.fragmentUpdateDownloadProgress.progress = state.progress
                        }
                    }
                    is UpdateDownloadBottomSheetViewModel.State.Done -> {
                        binding.fragmentUpdateDownloadTitle.text = getString(R.string.download_complete)
                        binding.fragmentUpdateDownloadProgress.isIndeterminate = false
                        binding.fragmentUpdateDownloadProgress.progress = 100

                        binding.fragmentUpdateDownloadCancel.text = getString(R.string.close)
                        binding.fragmentUpdateDownloadCancel.setOnClickListener {
                            sharedViewModel.clearUpdate()
                            dismiss()
                        }

                        binding.fragmentUpdateDownloadInstall.visibility = View.VISIBLE
                        binding.fragmentUpdateDownloadInstall.setOnClickListener {
                            updateViewModel.openPackageInstaller(requireContext(), state.fileUri)
                            sharedViewModel.clearUpdate()
                            dismiss()
                        }
                    }
                    is UpdateDownloadBottomSheetViewModel.State.Failed -> {
                        Toast.makeText(requireContext(), R.string.bs_update_download_failed, Toast.LENGTH_LONG).show()
                        dismiss()
                    }
                    UpdateDownloadBottomSheetViewModel.State.Idle -> {
                        // Do nothing
                    }
                }
            }
        }
        update?.let {
            updateViewModel.startDownload(requireContext(), it)
        } ?: run {
            dismiss()
        }
    }

}