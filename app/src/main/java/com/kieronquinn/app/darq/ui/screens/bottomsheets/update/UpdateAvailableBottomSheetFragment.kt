package com.kieronquinn.app.darq.ui.screens.bottomsheets.update

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.util.Log
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.darq.ui.screens.container.ContainerSharedViewModel
import com.kieronquinn.app.darq.utils.openLink
import com.kieronquinn.app.darq.utils.extensions.navGraphViewModel
import ru.noties.markwon.Markwon

class UpdateAvailableBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val sharedViewModel by navGraphViewModel<ContainerSharedViewModel>(R.id.nav_graph_main)

    override val cancelable = true

    private val update by lazy {
        sharedViewModel.getAvailableUpdate()
    }

    override val title by lazy {
        getString(R.string.bottom_sheet_update_available_title)
    }

    override val content by lazy {
        update?.changelog ?: ""
    }

    override val positiveText by lazy {
        getString(R.string.bottom_sheet_update_available_positive)
    }

    override val negativeText by lazy {
        getString(R.string.bottom_sheet_update_available_negative)
    }

    override val neutralText by lazy {
        getString(R.string.bottom_sheet_update_available_neutral)
    }

    private var navigatingToDownload = false

    override fun onNegativeClicked(dialog: BottomSheetDialog) {
        Log.d("DarQUpdate", "UpdateAvailableBottomSheetFragment: onNegativeClicked, clearing update and closing sheet")
        sharedViewModel.clearUpdate()
        super.onNegativeClicked(dialog)
    }

    override fun onPositiveClicked(dialog: BottomSheetDialog) {
        Log.d("DarQUpdate", "UpdateAvailableBottomSheetFragment: onPositiveClicked, navigating to download bottom sheet")
        navigatingToDownload = true
        lifecycleScope.launchWhenResumed {
            navigation.navigate(UpdateAvailableBottomSheetFragmentDirections.actionUpdateAvailableBottomSheetFragmentToUpdateDownloadBottomSheetFragment())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("DarQUpdate", "UpdateAvailableBottomSheetFragment: onViewCreated. Title: $title, content is empty: ${content.isEmpty()}, updateObj: $update")
        super.onViewCreated(view, savedInstanceState)
        val markwon = Markwon.create(requireContext())
        markwon.setMarkdown(binding.bottomSheetContent, content.toString())
    }

    override fun onNeutralClicked(dialog: BottomSheetDialog) {
        Log.d("DarQUpdate", "UpdateAvailableBottomSheetFragment: onNeutralClicked, opening releaseUrl = ${update?.releaseUrl}")
        update?.releaseUrl?.let {
            requireContext().openLink(it)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        Log.d("DarQUpdate", "UpdateAvailableBottomSheetFragment: onDismiss, navigatingToDownload = $navigatingToDownload")
        super.onDismiss(dialog)
        if (!navigatingToDownload) {
            sharedViewModel.clearUpdate()
        }
    }

}