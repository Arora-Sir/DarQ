package com.kieronquinn.app.darq.ui.screens.bottomsheets.errors

import android.os.Build
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.ui.base.BaseBottomSheetDialogFragment

class ServiceTimeoutBottomSheetFragment: BaseBottomSheetDialogFragment() {

    override val title by lazy {
        getString(R.string.bottom_sheet_service_timeout_title)
    }

    override val content by lazy {
        val baseContent = getString(R.string.bottom_sheet_service_timeout_content)
        val manufacturer = Build.MANUFACTURER ?: ""
        val brand = Build.BRAND ?: ""
        val isXiaomi = manufacturer.equals("Xiaomi", ignoreCase = true) || brand.equals("Xiaomi", ignoreCase = true) ||
                manufacturer.equals("Redmi", ignoreCase = true) || brand.equals("Redmi", ignoreCase = true) ||
                manufacturer.equals("POCO", ignoreCase = true) || brand.equals("POCO", ignoreCase = true)
        val isOppoLike = manufacturer.equals("OPPO", ignoreCase = true) || brand.equals("OPPO", ignoreCase = true) ||
                manufacturer.equals("OnePlus", ignoreCase = true) || brand.equals("OnePlus", ignoreCase = true) ||
                manufacturer.equals("Realme", ignoreCase = true) || brand.equals("Realme", ignoreCase = true)

        when {
            isXiaomi -> {
                "$baseContent\n\nFor Xiaomi/HyperOS, ensure you have enabled \"USB Debugging (Security settings)\" in Developer Options, and set Shizuku's Battery Saver to \"No restrictions\"."
            }
            isOppoLike -> {
                "$baseContent\n\nFor OPPO/OnePlus/Realme, ensure you have enabled \"Disable permission monitoring\" in Developer Options."
            }
            else -> {
                baseContent
            }
        }
    }

    override val positiveText by lazy {
        getString(R.string.bottom_sheet_service_timeout_positive)
    }

    override val cancelable = false

    override fun onPositiveClicked(dialog: BottomSheetDialog) {
        super.onPositiveClicked(dialog)
        requireActivity().finish()
    }

}